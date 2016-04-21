/**
  * APICloud Modules
  * Copyright (c) 2014-2015 by APICloud, Inc. All Rights Reserved.
  * Licensed under the terms of the The MIT License (MIT).
  * Please see the license.html included with this distribution for details.
  */

#import "ACBle.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import <CoreBluetooth/CBService.h>
#import <CoreBluetooth/CBCharacteristic.h>
#import "NSDictionaryUtils.h"

@interface ACBle ()
<CBCentralManagerDelegate, CBPeripheralDelegate>
{
    CBCentralManager *_centralManager;
    NSMutableDictionary *_allPeripheral, *_allPeripheralInfo;
    
    NSInteger initCbid, connectCbid, disconnectCbid, discoverServiceCbid, discoverCharacteristicsCbid, discoverDescriptorsForCharacteristicCbid;
    NSInteger setNotifyCbid, readValueForCharacteristicCbid, readValueForDescriptorCbid, writeValueCbid;
    BOOL disconnectClick;
}

@property (strong, nonatomic) CBCentralManager *centralManager;
@property (strong, nonatomic) NSMutableDictionary *allPeripheral, *allPeripheralInfo;

@end

@implementation ACBle

@synthesize centralManager = _centralManager;
@synthesize allPeripheral = _allPeripheral;
@synthesize allPeripheralInfo = _allPeripheralInfo;

#pragma mark - lifeCycle -

- (void)dispose {
    if (_centralManager) {
        _centralManager.delegate = nil;
        self.centralManager = nil;
    }
    if (initCbid >= 0) {
        [self deleteCallback:initCbid];
    }
    if (connectCbid >= 0) {
        [self deleteCallback:connectCbid];
    }
    if (setNotifyCbid >= 0) {
        [self deleteCallback:setNotifyCbid];
    }
    [self cleanStoredPeripheral];
}

- (id)initWithUZWebView:(UZWebView *)webView {
    self = [super initWithUZWebView:webView];
    if (self) {
        _allPeripheral = [NSMutableDictionary dictionary];
        _allPeripheralInfo = [NSMutableDictionary dictionary];
        disconnectClick = NO;
    }
    initCbid = -1;
    setNotifyCbid = -1;
    return self;
}

#pragma mark - interface -

- (void)initManager:(NSDictionary *)paramsDict_ {
    if (initCbid >= 0) {
        [self deleteCallback:initCbid];
    }
    initCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    if (_centralManager) {
        [self initManagerCallback:_centralManager.state];
        return;
    }
    _centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
}

- (void)scan:(NSDictionary *)paramsDict_ {
    NSArray *serviceIDs = [paramsDict_ arrayValueForKey:@"serviceUUIDS" defaultValue:@[]];
    NSMutableArray *allCBUUID = [self creatCBUUIDAry:serviceIDs];
    if (allCBUUID.count == 0) {
        allCBUUID = nil;
    }
    [_centralManager scanForPeripheralsWithServices:allCBUUID options:nil];
    NSInteger scanCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    if (_centralManager) {
        [self callbackToJs:YES withID:scanCbid];
    } else {
        [self callbackToJs:NO withID:scanCbid];
    }
}

- (void)getPeripheral:(NSDictionary *)paramsDict_ {
    NSInteger getCurDvcCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    if (_allPeripheralInfo.count > 0) {
        NSMutableArray *sendAry = [NSMutableArray array];
        for (NSString *targetId in [_allPeripheralInfo allKeys]) {
            NSDictionary *peripheral = [_allPeripheralInfo dictValueForKey:targetId defaultValue:@{}];
            if (peripheral) {
                [sendAry addObject:peripheral];
            }
        }
        [self sendResultEventWithCallbackId:getCurDvcCbid dataDict:[NSDictionary dictionaryWithObject:sendAry forKey:@"peripherals"] errDict:nil doDelete:YES];
    } else {
        [self sendResultEventWithCallbackId:getCurDvcCbid dataDict:nil errDict:nil doDelete:YES];
    }
}

- (void)isScanning:(NSDictionary *)paramsDict_ {
    NSInteger isConnectedCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    if(_centralManager && _centralManager.isScanning) {
        [self callbackToJs:YES withID:isConnectedCbid];
    } else {
        [self callbackToJs:NO withID:isConnectedCbid];
    }
}

- (void)stopScan:(NSDictionary *)paramsDict_ {
    if (_centralManager) {
        [_centralManager stopScan];
        [self cleanStoredPeripheral];
        self.allPeripheral = [NSMutableDictionary dictionary];
        self.allPeripheralInfo = [NSMutableDictionary dictionary];
    }
}

- (void)connect:(NSDictionary *)paramsDict_ {
    if (connectCbid >= 0) {
        [self deleteCallback:connectCbid];
    }
    connectCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:connectCbid doDelete:NO];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  == CBPeripheralStateConnected) {
            [self callbackCodeInfo:NO withCode:3 andCbid:connectCbid doDelete:NO];
        } else {
            [_centralManager connectPeripheral:peripheral options:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES] forKey:CBConnectPeripheralOptionNotifyOnDisconnectionKey]];
        }
    } else {
        [self callbackCodeInfo:NO withCode:2 andCbid:connectCbid doDelete:NO];
    }
}

- (void)disconnect:(NSDictionary *)paramsDict_ {
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        return;
    }
    disconnectClick = YES;
    disconnectCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  == CBPeripheralStateConnected) {
            [_centralManager cancelPeripheralConnection:peripheral];
        } else {
            disconnectClick = NO;
            [self callbackToJs:YES withID:disconnectCbid];
        }
    }
}

- (void)isConnected:(NSDictionary *)paramsDict_ {
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        return;
    }
    NSInteger isConnectedCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  == CBPeripheralStateConnected) {
            [self callbackToJs:YES withID:isConnectedCbid];
        } else {
            [self callbackToJs:NO withID:isConnectedCbid];
        }
    }
}

- (void)retrievePeripheral:(NSDictionary *)paramsDict_ {
    NSArray *peripheralUUIDs = [paramsDict_ arrayValueForKey:@"peripheralUUIDs" defaultValue:@[]];
    NSMutableArray *allPeriphralId = [self creatPeripheralNSUUIDAry:peripheralUUIDs];
    if (allPeriphralId.count == 0) {
        return;
    }
    NSMutableArray *allRetrivedPeripheral = nil;
    if (_centralManager) {
        NSArray *retrivedPer = [_centralManager retrievePeripheralsWithIdentifiers:allPeriphralId];
       allRetrivedPeripheral = [self getAllPeriphoeralInfoAry:retrivedPer];
    }
    if (!allRetrivedPeripheral) {
        allRetrivedPeripheral = [NSMutableArray array];
    }
    NSInteger retrieveCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    [self sendResultEventWithCallbackId:retrieveCbid dataDict:[NSDictionary dictionaryWithObject:allRetrivedPeripheral forKey:@"peripherals"] errDict:nil doDelete:YES];
}

- (void)retrieveConnectedPeripheral:(NSDictionary *)paramsDict_ {
    NSArray *serviceIDs = [paramsDict_ arrayValueForKey:@"serviceUUIDS" defaultValue:@[]];
    NSMutableArray *allCBUUID = [self creatCBUUIDAry:serviceIDs];
    if (allCBUUID.count == 0) {
        return;
    }
    NSMutableArray *allRetrivedPeripheral = nil;
    if (_centralManager) {
       NSArray *retrivedPer = [_centralManager retrieveConnectedPeripheralsWithServices:allCBUUID];
        allRetrivedPeripheral = [self getAllPeriphoeralInfoAry:retrivedPer];
    }
    if (!allRetrivedPeripheral) {
        allRetrivedPeripheral = [NSMutableArray array];
    }
    NSInteger retrieveCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    [self sendResultEventWithCallbackId:retrieveCbid dataDict:[NSDictionary dictionaryWithObject:allRetrivedPeripheral forKey:@"peripherals"] errDict:nil doDelete:YES];
}

- (void)discoverService:(NSDictionary *)paramsDict_ {
    discoverServiceCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:discoverServiceCbid doDelete:YES];
        return;
    }
    NSArray *serviceIDs = [paramsDict_ arrayValueForKey:@"serviceUUIDS" defaultValue:@[]];
    NSMutableArray *allCBUUID = [self creatCBUUIDAry:serviceIDs];
    if (allCBUUID.count == 0) {
        allCBUUID = nil;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        peripheral.delegate = self;
        [peripheral discoverServices:allCBUUID];
    } else {
        [self callbackCodeInfo:NO withCode:2 andCbid:discoverServiceCbid doDelete:YES];
    }
}

- (void)discoverCharacteristics:(NSDictionary *)paramsDict_ {
    discoverCharacteristicsCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:discoverCharacteristicsCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:discoverCharacteristicsCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            [peripheral discoverCharacteristics:nil forService:myService];
        } else {
            [self callbackCodeInfo:NO withCode:3 andCbid:discoverCharacteristicsCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:4 andCbid:discoverCharacteristicsCbid doDelete:YES];
    }
}

- (void)discoverDescriptorsForCharacteristic:(NSDictionary *)paramsDict_ {
    discoverDescriptorsForCharacteristicCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                [peripheral discoverDescriptorsForCharacteristic:characteristic];
            } else {
                [self callbackCodeInfo:NO withCode:4 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
            }
        } else {
            [self callbackCodeInfo:NO withCode:5 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:6 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
    }
}

- (void)setNotify:(NSDictionary *)paramsDict_ {
    if (setNotifyCbid >= 0) {
        [self deleteCallback:setNotifyCbid];
    }
    setNotifyCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:setNotifyCbid doDelete:NO];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:setNotifyCbid doDelete:NO];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:setNotifyCbid doDelete:NO];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                [peripheral setNotifyValue:YES forCharacteristic:characteristic];
            } else {
                [self callbackCodeInfo:NO withCode:4 andCbid:setNotifyCbid doDelete:NO];
            }
        } else {
            [self callbackCodeInfo:NO withCode:5 andCbid:setNotifyCbid doDelete:NO];
        }
    } else {
        [self callbackCodeInfo:NO withCode:6 andCbid:setNotifyCbid doDelete:NO];
    }
}

- (void)readValueForCharacteristic:(NSDictionary *)paramsDict_ {
    readValueForCharacteristicCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:readValueForCharacteristicCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:readValueForCharacteristicCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:readValueForCharacteristicCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                [peripheral readValueForCharacteristic:characteristic];
            } else {
                [self callbackCodeInfo:NO withCode:4 andCbid:readValueForCharacteristicCbid doDelete:YES];
            }
        } else {
            [self callbackCodeInfo:NO withCode:5 andCbid:readValueForCharacteristicCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:6 andCbid:readValueForCharacteristicCbid doDelete:YES];
    }
}

- (void)readValueForDescriptor:(NSDictionary *)paramsDict_ {
    readValueForDescriptorCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    }
    NSString *descriptorUUID = [paramsDict_ stringValueForKey:@"descriptorUUID" defaultValue:nil];
    if (descriptorUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:4 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
               CBDescriptor *descriptor = [self getDescriptorInCharacteristic:characteristic withUUID:descriptorUUID];
                if (descriptor) {
                    [peripheral readValueForDescriptor:descriptor];
                } else {
                    [self callbackCodeInfo:NO withCode:5 andCbid:readValueForDescriptorCbid doDelete:YES];
                }
            } else {
                [self callbackCodeInfo:NO withCode:6 andCbid:readValueForDescriptorCbid doDelete:YES];
            }
        } else {
            [self callbackCodeInfo:NO withCode:7 andCbid:readValueForDescriptorCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:8 andCbid:readValueForDescriptorCbid doDelete:YES];
    }
}

- (void)writeValueForCharacteristic:(NSDictionary *)paramsDict_ {
    writeValueCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *value = [paramsDict_ stringValueForKey:@"value" defaultValue:nil];
    if (value.length == 0) {
        [self callbackCodeInfo:NO withCode:4 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                NSData *valueData = [[NSData alloc] initWithBase64EncodedString:value options:0];
                if (valueData) {
                    CBCharacteristicWriteType type = CBCharacteristicWriteWithResponse;
                    if((characteristic.properties == CBCharacteristicPropertyWriteWithoutResponse)) {
                        type = CBCharacteristicWriteWithoutResponse;
                    } else if((characteristic.properties == CBCharacteristicPropertyWrite)) {
                        type = CBCharacteristicWriteWithResponse;
                    }usleep(20 * 1000);
                    [peripheral writeValue:valueData forCharacteristic:characteristic type:type];
                }
            } else {
                [self callbackCodeInfo:NO withCode:5 andCbid:writeValueCbid doDelete:YES];
            }
        } else {
            [self callbackCodeInfo:NO withCode:6 andCbid:writeValueCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:7 andCbid:writeValueCbid doDelete:YES];
    }
}

- (void)writeValueForDescriptor:(NSDictionary *)paramsDict_ {
    writeValueCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:1 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:2 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:3 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *descriptorUUID = [paramsDict_ stringValueForKey:@"descriptorUUID" defaultValue:nil];
    if (descriptorUUID.length == 0) {
        [self callbackCodeInfo:NO withCode:4 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSString *value = [paramsDict_ stringValueForKey:@"value" defaultValue:nil];
    if (value.length == 0) {
        [self callbackCodeInfo:NO withCode:5 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                CBDescriptor *descriptor = [self getDescriptorInCharacteristic:characteristic withUUID:descriptorUUID];
                if (descriptor) {
                    NSData *valueData = [[NSData alloc] initWithBase64EncodedString:value options:0];
                    if (valueData) {
                        [peripheral writeValue:valueData forDescriptor:descriptor];
                    }
                } else {
                    [self callbackCodeInfo:NO withCode:6 andCbid:writeValueCbid doDelete:YES];
                }
            } else {
                [self callbackCodeInfo:NO withCode:7 andCbid:writeValueCbid doDelete:YES];
            }
        } else {
            [self callbackCodeInfo:NO withCode:8 andCbid:writeValueCbid doDelete:YES];
        }
    } else {
        [self callbackCodeInfo:NO withCode:9 andCbid:writeValueCbid doDelete:YES];
    }
}

#pragma mark - CBPeripheralDelegate -

//按特征&描述符发送数据的回调
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    NSMutableDictionary *writeCharacteristic = [NSMutableDictionary dictionary];
    if(error){
        [self callbackCodeInfo:NO withCode:-1 andCbid:writeValueCbid doDelete:YES];
        return;
    }
    NSMutableDictionary *characterDict = [self getCharacteristicsDict:characteristic];
    [writeCharacteristic setValue:characterDict forKey:@"characteristic"];
    [writeCharacteristic setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
    [self sendResultEventWithCallbackId:writeValueCbid dataDict:writeCharacteristic errDict:nil doDelete:YES];
}

//根据描述符读取数据
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForDescriptor:(CBDescriptor *)descriptor error:(NSError *)error{
    NSMutableDictionary *descriptorDict = [NSMutableDictionary dictionary];
    NSString *descriptorUUID = descriptor.UUID.UUIDString;
    if (!descriptorUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    }
    //[descriptorDict setValue:descriptorUUID forKey:@"descriptorUUID"];
    if(error) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:readValueForDescriptorCbid doDelete:YES];
        return;
    } else {
        NSMutableDictionary *descriptorsDict = [self getDescriptorInfo:descriptor];
        [descriptorDict setValue:descriptorsDict forKey:@"descriptor"];
        [descriptorDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:readValueForDescriptorCbid dataDict:descriptorDict errDict:nil doDelete:YES];
    }
}

//按特征读取数据
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:readValueForCharacteristicCbid doDelete:NO];
        return;
    }
    //[characteristicDict setValue:characterUUID forKey:@"uuid"];
    if(error) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:readValueForCharacteristicCbid doDelete:NO];
    } else {
        NSMutableDictionary *characteristics = [self getCharacteristicsDict:characteristic];
        [characteristicDict setValue:characteristics forKey:@"characteristic"];
        [characteristicDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:readValueForCharacteristicCbid dataDict:characteristicDict errDict:nil doDelete:YES];
    }
}

//监听外围设备后有心跳数据包发送时的回调
- (void)peripheral:(CBPeripheral *)peripheral didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)characteristic error:(nullable NSError *)error {
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:setNotifyCbid doDelete:NO];
        return;
    }
    //[characteristicDict setValue:characterUUID forKey:@"uuid"];
    if(error) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:setNotifyCbid doDelete:NO];
    } else {
        NSMutableDictionary *characteristics = [self getCharacteristicsDict:characteristic];
        [characteristicDict setValue:characteristics forKey:@"characteristic"];
        [characteristicDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:setNotifyCbid dataDict:characteristicDict errDict:nil doDelete:YES];
    }
}

//根据特征查找描述符
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverDescriptorsForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    NSMutableDictionary *descriptorDict = [NSMutableDictionary dictionary];
    NSString *serviceUUID = characteristic.service.UUID.UUIDString;
    if (!serviceUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        return;
    }
    NSString *characteristicUUID = characteristic.UUID.UUIDString;
    if (!characteristicUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
        return;
    }
    //[descriptorDict setValue:characteristicUUID forKey:@"characteristaicUUID"];
    //[descriptorDict setValue:serviceUUID forKey:@"serviceUUID"];
    if(error) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverDescriptorsForCharacteristicCbid doDelete:YES];
    } else {
        NSMutableArray *descriptors = [NSMutableArray array];
        for(CBDescriptor *descriptor in characteristic.descriptors) {
            [descriptors addObject:[self getDescriptorInfo:descriptor]];
        }
        [descriptorDict setValue:descriptors forKey:@"descriptors"];
        [descriptorDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:discoverDescriptorsForCharacteristicCbid dataDict:descriptorDict errDict:nil doDelete:YES];
    }
}

//查询指定服务的所有特征
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *serviceUUID = service.UUID.UUIDString;
    if (!serviceUUID) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverCharacteristicsCbid doDelete:YES];
        return;
    }
    //[serviceDict setValue:serviceUUID forKey:@"uuid"];
    if(error) {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverCharacteristicsCbid doDelete:YES];
    } else {
        NSMutableArray *characteristics = [self getAllCharacteristicsInfoAry:service.characteristics];
        [characteristicDict setValue:characteristics forKey:@"characteristics"];
        [characteristicDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:discoverCharacteristicsCbid dataDict:characteristicDict errDict:nil doDelete:YES];
    }
}

//查询指定设备的所有服务
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    if(!peripheral.identifier) {
        return;
    }
    if (!error) {
        NSMutableArray *services = [self getAllServiceInfoAry:peripheral.services];
        NSMutableDictionary *sendDict = [NSMutableDictionary dictionary];
        [sendDict setObject:services forKey:@"services"];
        [sendDict setObject:[NSNumber numberWithBool:YES] forKey:@"status"];
        [self sendResultEventWithCallbackId:discoverServiceCbid dataDict:sendDict errDict:nil doDelete:YES];
    } else {
        [self callbackCodeInfo:NO withCode:-1 andCbid:discoverServiceCbid doDelete:YES];
    }
}

#pragma mark - CBCentralManagerDelegate -

//初始化中心设备管理器时返回其状态
- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    [self initManagerCallback:central.state];
}

//app状态的保存或者恢复，这是第一个被调用的方法当APP进入后台去完成一些蓝牙有关的工作设置，使用这个方法同步app状态通过蓝牙系统
- (void)centralManager:(CBCentralManager *)central willRestoreState:(NSDictionary<NSString *, id> *)dict {

}

//扫描设备的回调，大概每秒十次的频率在重复回调
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *, id> *)advertisementData RSSI:(NSNumber *)RSSI {
    if (!peripheral.identifier) {
        return;
    }
    NSString *periphoeralUUID = peripheral.identifier.UUIDString;
    if (![periphoeralUUID isKindOfClass:[NSString class]] || periphoeralUUID.length<=0) {
        return;
    }
    if([[_allPeripheral allValues] containsObject:peripheral]) {//更新旧设备的信号强度值
        NSMutableDictionary *targetPerInfo = [_allPeripheralInfo objectForKey:periphoeralUUID];
        if (targetPerInfo) {
            [targetPerInfo setObject:RSSI forKey:@"rssi"];
        }
    } else {//发现新设备
        [_allPeripheral setObject:peripheral forKey:periphoeralUUID];
        NSMutableDictionary *peripheralInfo = [self getAllPeriphoerDict:peripheral];
        [_allPeripheralInfo setObject:peripheralInfo forKey:periphoeralUUID];
    }
}

//连接外围设备成功后的回调
- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    [self callbackCodeInfo:YES withCode:0 andCbid:connectCbid doDelete:NO];
}

//连接外围设备失败后的回调
- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(nullable NSError *)error {
    [self callbackCodeInfo:NO withCode:-1 andCbid:connectCbid doDelete:NO];
}

//断开通过uuid指定的外围设备的连接
- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(nullable NSError *)error {
    if (disconnectClick) {
        disconnectClick = NO;
        if (error) {
            [self callbackToJs:NO withID:disconnectCbid];
        } else {
            [self callbackToJs:YES withID:disconnectCbid];
        }
    } else {
        [self callbackCodeInfo:NO withCode:-1 andCbid:connectCbid doDelete:NO];
    }
}

#pragma mark - Utilities -
//初始化蓝牙管理器回调
- (void)initManagerCallback:(CBCentralManagerState)managerState {
    NSString *state = nil;
    switch (managerState) {
        case CBCentralManagerStatePoweredOff://设备关闭状态
            state = @"poweredOff";
            break;
            
        case CBCentralManagerStatePoweredOn:// 设备开启状态 -- 可用状态
            state = @"poweredOn";
            break;
            
        case CBCentralManagerStateResetting://正在重置状态
            state = @"resetting";
            break;
            
        case CBCentralManagerStateUnauthorized:// 设备未授权状态
            state = @"unauthorized";
            break;
            
        case CBCentralManagerStateUnknown:// 初始的时候是未知的（刚刚创建的时候）
            state = @"unknown";
            break;
            
        case CBCentralManagerStateUnsupported://设备不支持的状态
            state = @"unsupported";
            break;
            
        default:
            state = @"unknown";
            break;
    }
    if (initCbid >= 0) {
        [self sendResultEventWithCallbackId:initCbid dataDict:[NSDictionary dictionaryWithObject:state forKey:@"state"] errDict:nil doDelete:NO];
    }
}
//获取所有设备所有信息
- (NSMutableArray *)getAllPeriphoeralInfoAry:(NSArray *)peripherals {
    NSMutableArray *allRetrivedPeripheral = [NSMutableArray array];
    for (CBPeripheral *targetPer in peripherals) {
        NSMutableDictionary *peripheralInfo = [self getAllPeriphoerDict:targetPer];
        if (peripheralInfo && peripheralInfo.count>0) {
            [allRetrivedPeripheral addObject:peripheralInfo];
        }
    }
    return allRetrivedPeripheral;
}
//获取指定设备所有信息
- (NSMutableDictionary *)getAllPeriphoerDict:(CBPeripheral *)singlePeripheral {
    NSMutableDictionary *peripheralInfo = [NSMutableDictionary dictionary];
    NSString *periphoeralUUID = singlePeripheral.identifier.UUIDString;
    if (!periphoeralUUID) {
        return peripheralInfo;
    }
    [peripheralInfo setValue:periphoeralUUID forKey:@"uuid"];
    NSString *periphoeralName = singlePeripheral.name;
    if ([periphoeralName isKindOfClass:[NSString class]] && periphoeralName.length>0) {
        [peripheralInfo setValue:periphoeralName forKey:@"name"];
    }
    NSNumber *RSSI = singlePeripheral.RSSI;
    if (RSSI) {
        [peripheralInfo setValue:RSSI forKey:@"rssi"];
    }
    NSMutableArray *allServiceUid = [self getAllServiceInfoAry:singlePeripheral.services];
    [peripheralInfo setObject:allServiceUid forKey:@"services"];
    return peripheralInfo;
}
//收集指定设备的所有服务（service）
- (NSMutableArray *)getAllServiceInfoAry:(NSArray *)serviceAry {
    NSMutableArray *allServiceUid = [NSMutableArray array];
    for (CBService *targetService in serviceAry) {
        NSString *serviceUUID = targetService.UUID.UUIDString;
        if ([serviceUUID isKindOfClass:[NSString class]] && serviceUUID.length>0) {
            [allServiceUid addObject:serviceUUID];
        }
    }
    return allServiceUid;
}
//收集指定服务的所有特征（Characteristics）
- (NSMutableArray *)getAllCharacteristicsInfoAry:(NSArray *)characteristicsAry {
    NSMutableArray *allCharacteristics = [NSMutableArray array];
    for (CBCharacteristic *characteristic in characteristicsAry) {
        NSMutableDictionary *characterInfo = [self getCharacteristicsDict:characteristic];
        [allCharacteristics addObject:characterInfo];
        continue;
        NSString *characteristicsUUID = characteristic.UUID.UUIDString;
        if ([characteristicsUUID isKindOfClass:[NSString class]] && characteristicsUUID.length>0) {
            [allCharacteristics addObject:characteristicsUUID];
        }
    }
    return allCharacteristics;
}
//收集指定特征的所有数据
- (NSMutableDictionary *)getCharacteristicsDict:(CBCharacteristic *)characteristic {
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        return characteristicDict;
    }
    NSString *charactProperti = @"broadcast";
    switch (characteristic.properties) {
        case CBCharacteristicPropertyBroadcast:
            charactProperti = @"broadcast";
            break;
            
        case CBCharacteristicPropertyRead:
            charactProperti = @"read";
            break;
            
        case CBCharacteristicPropertyWriteWithoutResponse:
            charactProperti = @"writeWithoutResponse";
            break;
            
        case CBCharacteristicPropertyWrite:
            charactProperti = @"write";
            break;
            
        case CBCharacteristicPropertyNotify:
            charactProperti = @"notify";
            break;
            
        case CBCharacteristicPropertyIndicate:
            charactProperti = @"indicate";
            break;
            
        case CBCharacteristicPropertyAuthenticatedSignedWrites:
            charactProperti = @"authenticatedSignedWrites";
            break;
            
        case CBCharacteristicPropertyExtendedProperties:
            charactProperti = @"extendedProperties";
            break;
            
        case CBCharacteristicPropertyNotifyEncryptionRequired:
            charactProperti = @"notifyEncryptionRequired";
            break;
            
        case CBCharacteristicPropertyIndicateEncryptionRequired:
            charactProperti = @"indicateEncryptionRequired";
            break;
            
        default:
            charactProperti = @"broadcast";
            break;
    }
    [characteristicDict setValue:charactProperti forKey:@"properties"];
    if([characteristic isKindOfClass:[CBMutableCharacteristic class]]) {
        CBMutableCharacteristic *mutableCharacteristic = (CBMutableCharacteristic *)characteristic;
        NSString *permission = @"readable";
        switch (mutableCharacteristic.permissions) {
            case CBAttributePermissionsReadable:
                permission = @"readable";
                break;
                
            case CBAttributePermissionsWriteable:
                permission = @"writeable";
                break;
                
            case CBAttributePermissionsReadEncryptionRequired:
                permission = @"readEncryptionRequired";
                break;
                
            case CBAttributePermissionsWriteEncryptionRequired:
                permission = @"writeEncryptionRequired";
                break;
                
            default:
                permission = @"readable";
                break;
        }
        [characteristicDict setValue:permission forKey:@"permissions"];
    }
    NSString *serviceuuid = characteristic.service.UUID.UUIDString;
    if ([serviceuuid isKindOfClass:[NSString class]] && serviceuuid.length>0) {
        [characteristicDict setValue:serviceuuid forKey:@"serviceUUID"];
    }
    [characteristicDict setValue:characterUUID forKey:@"uuid"];
    NSData *characterData = characteristic.value;
    if (characterData) {
        NSString *value = [characterData base64EncodedStringWithOptions:0];
        [characteristicDict setValue:value forKey:@"value"];
    }
    NSMutableArray *descriptorAry = [self getAllDescriptorInfo:characteristic.descriptors];
    if (descriptorAry.count > 0) {
        [characteristicDict setValue:descriptorAry forKey:@"descriptors"];
    }
    
    return characteristicDict;
}
//获取指定特征的所有描述信息
- (NSMutableArray *)getAllDescriptorInfo:(NSArray *)descripterAry {
    NSMutableArray *descriptorInfoAry = [NSMutableArray array];
    for(CBDescriptor *descriptor in descripterAry){
        [descriptorInfoAry addObject:[self getDescriptorInfo:descriptor]];
    }
    return descriptorInfoAry;
}
//获取指定描述的所有信息
- (NSMutableDictionary *)getDescriptorInfo:(CBDescriptor *)descriptor {
    NSMutableDictionary *descriptorDict = [NSMutableDictionary dictionary];
    NSString *descriptorUUID = descriptor.UUID.UUIDString;
    if (!descriptorUUID) {
        return descriptorDict;
    }
    [descriptorDict setValue:descriptorUUID forKey:@"uuid"];
    NSString *characterUUID = descriptor.characteristic.UUID.UUIDString;
    [descriptorDict setValue:characterUUID forKey:@"characteristicUUID"];
    NSString *serviceUUID = descriptor.characteristic.service.UUID.UUIDString;
    [descriptorDict setValue:serviceUUID forKey:@"serviceUUID"];
    
    NSString *valueStr;
    id value = descriptor.value;
    if([value isKindOfClass:[NSNumber class]]){
        valueStr = [value stringValue];
        [descriptorDict setValue:[NSNumber numberWithBool:NO] forKey:@"decode"];
    }
    if([value isKindOfClass:[NSString class]]){
        valueStr = (NSString *)value;
        [descriptorDict setValue:[NSNumber numberWithBool:NO] forKey:@"decode"];
    }
    if([value isKindOfClass:[NSData class]]){
        NSData *descripterData = (NSData *)value;
        valueStr = [descripterData base64EncodedStringWithOptions:0];;
        [descriptorDict setValue:[NSNumber numberWithBool:YES] forKey:@"decode"];
    }
    [descriptorDict setValue:valueStr forKey:@"value"];
    return descriptorDict;
}
//根据uuid查找服务（service）
- (CBService *)getServiceWithPeripheral:(CBPeripheral *)peripheral andUUID:(NSString *)uuid {
    if(!peripheral || peripheral.state!=CBPeripheralStateConnected) {
        return nil;
    }
    for (CBService *service in peripheral.services){
        NSString *serviceUUID = service.UUID.UUIDString;
        if (!serviceUUID) {
            return nil;
        }
        if([serviceUUID isEqual:uuid]){
            return service;
        }
    }
    return nil;
}
//根据服务查找特征
- (CBCharacteristic *)getCharacteristicInService:(CBService *)service withUUID:(NSString *)uuid {
    for(CBCharacteristic *characteristic in service.characteristics) {
        NSString *characterUUID = characteristic.UUID.UUIDString;
        if (!characterUUID) {
            return nil;
        }
        if([characterUUID isEqual:uuid]){
            return characteristic;
        }
    }
    return nil;
}
//根据特征查找描述符
- (CBDescriptor *)getDescriptorInCharacteristic:(CBCharacteristic *)characteristic withUUID:(NSString *)uuid {
    for(CBDescriptor *descriptor in characteristic.descriptors){
        NSString *descriptorUUID = characteristic.UUID.UUIDString;
        if (!descriptorUUID) {
            return nil;
        }
        if([descriptorUUID isEqual:uuid]){
            return descriptor;
        }
    }
    return nil;
}
//获取或生成设备的NSUUID数组
- (NSMutableArray *)creatPeripheralNSUUIDAry:(NSArray *)peripheralUUIDS {
    NSMutableArray *allPeriphralId = [NSMutableArray array];
    for(int i=0; i<[peripheralUUIDS count]; i++) {
        NSString *peripheralUUID = [peripheralUUIDS objectAtIndex:i];
        if ([peripheralUUID isKindOfClass:[NSString class]] && peripheralUUID.length>0) {
            NSUUID *perIdentifier;
            CBPeripheral *peripheralStored = [_allPeripheral objectForKey:peripheralUUID];
            if (peripheralStored) {
                perIdentifier = peripheralStored.identifier;
            } else {
                perIdentifier = [[NSUUID alloc]initWithUUIDString:peripheralUUID];
            }
            if (perIdentifier) {
                [allPeriphralId addObject:perIdentifier];
            }
        }
    }
    return allPeriphralId;
}
//生成设备的服务的CBUUID数组
- (NSMutableArray *)creatCBUUIDAry:(NSArray *)serviceUUIDs {
    NSMutableArray *allCBUUID = [NSMutableArray array];
    for(int i=0; i<[serviceUUIDs count]; i++) {
        NSString *serviceID = [serviceUUIDs objectAtIndex:i];
        if ([serviceID isKindOfClass:[NSString class]] && serviceID.length>0) {
            [allCBUUID addObject:[CBUUID UUIDWithString:serviceID]];
        }
    }
    return allCBUUID;
}
//带code的回调
- (void)callbackCodeInfo:(BOOL)status withCode:(int)code andCbid:(NSInteger)backID doDelete:(BOOL)delete {
    [self sendResultEventWithCallbackId:backID dataDict:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:status] forKey:@"status"] errDict:[NSDictionary dictionaryWithObject:[NSNumber numberWithInt:code] forKey:@"code"] doDelete:delete];
}
//回调状态
- (void)callbackToJs:(BOOL)status withID:(NSInteger)backID {
    [self sendResultEventWithCallbackId:backID dataDict:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:status] forKey:@"status"] errDict:nil doDelete:YES];
}
//情况本地记录的所有设备信息
- (void)cleanStoredPeripheral {
    if (_allPeripheralInfo.count > 0) {
        [_allPeripheralInfo removeAllObjects];
        self.allPeripheralInfo = nil;
    }
    if (_allPeripheral.count > 0) {
        for (CBPeripheral *targetPer in [_allPeripheral allValues]) {
            targetPer.delegate = nil;
        }
        [_allPeripheral removeAllObjects];
        self.allPeripheral = nil;
    }
}
@end
