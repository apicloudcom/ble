//
//  BLESingle.m
//  UZApp
//
//  Created by 孙政篡 on 2017/12/15.
//  Copyright © 2017年 APICloud. All rights reserved.
//

#import "BLESingle.h"

#import <CoreBluetooth/CoreBluetooth.h>
#import <CoreBluetooth/CBService.h>
#import <CoreBluetooth/CBCharacteristic.h>
#import "NSDictionaryUtils.h"
#import <objc/runtime.h>

@interface BLESingle ()
<CBCentralManagerDelegate, CBPeripheralDelegate>
{
    BOOL disconnectClick;
}

@property (strong, nonatomic) CBCentralManager *centralManager;
@property (strong, nonatomic) NSMutableDictionary *allPeripheral, *allPeripheralInfo;
@property (strong, nonatomic) NSMutableDictionary *notifyPeripheralInfo;

@property (copy, nonatomic) void(^initCallback)(NSString *state);
@property (copy, nonatomic) void (^getRssiCallback)(BOOL success, int errorCode, NSNumber *rssi);
@property (copy, nonatomic) void (^connectCallbackUUID)(BOOL success, int erroCode, NSString *uuid);
@property (copy, nonatomic) void (^disconnectCallback)(BOOL success, NSString *perid);
@property (copy, nonatomic) void (^discoverServiceCallback)(BOOL success, NSArray *services, int errorCode);
@property (copy, nonatomic) void (^discoverCharacteristicsCallback)(BOOL success, NSArray *characteristics, int errorCode);
@property (copy, nonatomic) void (^disDesForChaCallback)(BOOL success, NSArray *descriptors, int errorCode);
@property (copy, nonatomic) void(^setNotifyCallback)(BOOL success, NSDictionary *characteristic, int errorCode);
@property (copy, nonatomic) void(^readForCharCallback)(BOOL success, NSDictionary *characteristic, int errorCode);
@property (copy, nonatomic) void(^readForDescCallback)(BOOL success, NSDictionary *descriptor, int errorCode);
@property (copy, nonatomic) void(^writeForCharCallback)(BOOL success, NSDictionary *characteristic, int errorCode);
@property (copy, nonatomic) void(^writeForDescCallback)(BOOL success, NSDictionary *descriptor, int errorCode);
@property (copy, nonatomic) void(^connectPeripherals)(BOOL success, NSString *peripherals);

- (void)cleanStoredPeripheral;
- (void)initManagerCallback:(CBManagerState)managerState;
- (NSMutableArray *)creatCBUUIDAry:(NSArray *)serviceUUIDs;
- (NSMutableArray *)creatPeripheralNSUUIDAry:(NSArray *)peripheralUUIDS;
- (NSMutableArray *)getAllPeriphoeralInfoAry:(NSArray *)peripherals;
- (CBService *)getServiceWithPeripheral:(CBPeripheral *)peripheral andUUID:(NSString *)uuid;
- (CBCharacteristic *)getCharacteristicInService:(CBService *)service withUUID:(NSString *)uuid;
- (CBDescriptor *)getDescriptorInCharacteristic:(CBCharacteristic *)characteristic withUUID:(NSString *)uuid;
- (NSData*)dataFormHexString:(NSString *)hexString;
- (NSMutableDictionary *)getCharacteristicsDict:(CBCharacteristic *)characteristic;
- (NSMutableDictionary *)getDescriptorInfo:(CBDescriptor *)descriptor;
- (void)restoreNotifyData:(CBPeripheral *)peripheral withCharacteristic:(CBCharacteristic *)characteristic;
@end

@implementation BLESingle

static BLESingle *bleInstance = nil;

@synthesize centralManager = _centralManager;
@synthesize allPeripheral = _allPeripheral;
@synthesize allPeripheralInfo = _allPeripheralInfo;
@synthesize notifyPeripheralInfo = _notifyPeripheralInfo;

//static char bleExtendKey;
#pragma mark - lifeCycle -

- (void)dealloc {
    if (_centralManager) {
        _centralManager.delegate = nil;
        self.centralManager = nil;
    }
    [self cleanStoredPeripheral];
    [self clearAllSimpleNotifyData];
}

- (void)initData {
    _allPeripheral = [NSMutableDictionary dictionary];
    _allPeripheralInfo = [NSMutableDictionary dictionary];
    _notifyPeripheralInfo = [NSMutableDictionary dictionary];
    disconnectClick = NO;
}

+ (BLESingle *)sharedInstance {
    @synchronized(self) {
        if (!bleInstance) {
            bleInstance = [[BLESingle alloc]init];
        }
        return bleInstance;
    }
    return bleInstance;
}

+ (id)allocWithZone:(struct _NSZone *)zone {
    @synchronized(self) {
        if (!bleInstance) {
            bleInstance = [super allocWithZone:zone];
        }
        return bleInstance;
    }
    return bleInstance;
}

- (id)copyWithZone:(NSZone *)zone {
    return bleInstance;
}

- (id)mutableCopyWithZone:(NSZone *)zone {
    return bleInstance;
}
#pragma mark - interface -

- (void)initManager:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(NSString * state))callback {
    self.initCallback = callback;
    if (_centralManager) {
        [self initManagerCallback:_centralManager.state];
        return;
    }
    _centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
}

- (void)scan:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success))callback{
    NSArray *serviceIDs = [paramsDict_ arrayValueForKey:@"serviceUUIDs" defaultValue:@[]];
    BOOL clean = [paramsDict_ boolValueForKey:@"clean" defaultValue:YES];
    
    if (clean) {
        //先清空上次缓存
        [self cleanStoredPeripheral];
        self.allPeripheral = [NSMutableDictionary dictionary];
        self.allPeripheralInfo = [NSMutableDictionary dictionary];
    }
    
    NSMutableArray *allCBUUID = [self creatCBUUIDAry:serviceIDs];
    if (allCBUUID.count == 0) {
        allCBUUID = nil;
    }
    
    [_centralManager scanForPeripheralsWithServices:allCBUUID options:nil];
    if (_centralManager) {
        callback(YES);
    } else {
        callback(NO);
    }
}

- (void)clean {
    [self cleanStoredPeripheral];
    self.allPeripheral = [NSMutableDictionary dictionary];
    self.allPeripheralInfo = [NSMutableDictionary dictionary];
}

- (void)getPeripheral:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSDictionary *sendDict))callback {
    if (_allPeripheralInfo.count > 0) {
        NSMutableArray *sendAry = [NSMutableArray array];
        for (NSString *targetId in [_allPeripheralInfo allKeys]) {
            NSDictionary *peripheral = [_allPeripheralInfo dictValueForKey:targetId defaultValue:@{}];
            if (peripheral) {
                [sendAry addObject:peripheral];
            }
        }
        NSDictionary *sendDict = @{@"peripherals":sendAry};
        callback(sendDict);
    } else {
        callback(nil);
    }
}

- (void)getPeripheralRssi:(NSDictionary *)paramsDict_  callbackBlock:(void(^)(BOOL success, int errorCode, NSNumber *rssi))callback {
    self.getRssiCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.getRssiCallback(NO, 1, nil);
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (!peripheral) {
        self.getRssiCallback(NO, 2, nil);
        return;
    }
    peripheral.delegate = self;
    //NSInteger getPeripheralRssiCbid = [paramsDict_ integerValueForKey:@"cbId" defaultValue:-1];
    //NSNumber *cbid = [NSNumber numberWithInteger:getPeripheralRssiCbid];
    //objc_setAssociatedObject(peripheral, &bleExtendKey, cbid, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [peripheral readRSSI];
}
//获取peripheral的RSSI的delegate
- (void)peripheral:(CBPeripheral *)peripheral didReadRSSI:(NSNumber *)RSSI error:(nullable NSError *)error NS_AVAILABLE(NA, 8_0) {
    //NSNumber *getPeripheralRssiCbid = (NSNumber *)objc_getAssociatedObject(peripheral, &bleExtendKey);
    //if (getPeripheralRssiCbid) {
    //    NSInteger getPerCbid = [getPeripheralRssiCbid integerValue];
        if (RSSI) {
            self.getRssiCallback(YES, 0, RSSI);
        } else {
            self.getRssiCallback(NO, 3, nil);
        }
    //}
}

- (void)isScanning:(NSDictionary *)paramsDict_  callbackBlock:(void(^)(BOOL status))callback {
    if(_centralManager && _centralManager.isScanning) {
        callback(YES);
    } else {
        callback(NO);
    }
}

- (void)stopScan {
    if (_centralManager) {
        [_centralManager stopScan];
//        [self cleanStoredPeripheral];
//        self.allPeripheral = [NSMutableDictionary dictionary];
//        self.allPeripheralInfo = [NSMutableDictionary dictionary];
    }
}

- (void)connect:(NSDictionary *)paramsDict_  callbackBlock:(void(^)(BOOL status, int erroCode, NSString *uuid))callback {
    self.connectCallbackUUID = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        callback(NO,1,nil);
        return;
    }
    
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    NSString *perUUID = peripheral.identifier.UUIDString;
    if (![perUUID isKindOfClass:[NSString class]] || perUUID.length==0) {
        perUUID = @"";
    }
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  == CBPeripheralStateConnected) {
            callback(NO,3,perUUID);
        } else {
            [_centralManager connectPeripheral:peripheral options:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES] forKey:CBConnectPeripheralOptionNotifyOnDisconnectionKey]];
        }
    } else {
        callback(NO,2,perUUID);
    }
}

- (void)disconnect:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL status, NSString *perid))callback{
    self.disconnectCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        return;
    }
    disconnectClick = YES;
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state != CBPeripheralStateDisconnected) {
            [_centralManager cancelPeripheralConnection:peripheral];
        } else {
            disconnectClick = NO;
            callback(YES,peripheral.identifier.UUIDString);
        }
    }
}


- (void)getPeripheralState:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(NSString *perid))callback {
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    CBPeripheralState state = peripheral.state;
    NSString *stateStr = @"unknow";
    switch (state) {
        case CBPeripheralStateDisconnected:
            stateStr = @"disconnected";
            break;
        case CBPeripheralStateConnecting:
            stateStr = @"connecting";
            break;
        case CBPeripheralStateConnected:
            stateStr = @"connected";
            break;
        case CBPeripheralStateDisconnecting:
            stateStr = @"disconnecting";
            break;
            
        default:
            break;
    }
    callback(stateStr);
}

- (void)isConnected:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL status, NSString *perid))callback {
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  == CBPeripheralStateConnected) {
            callback(YES,peripheral.identifier.UUIDString);
        } else {
            callback(NO,peripheral.identifier.UUIDString);
        }
    }else {
        callback(NO,peripheral.identifier.UUIDString);
    }
}

- (void)retrievePeripheral:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(NSDictionary *perDict))callback {
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
    callback([NSDictionary dictionaryWithObject:allRetrivedPeripheral forKey:@"peripherals"]);
}

- (void)retrieveConnectedPeripheral:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(NSDictionary *perDict))callback {
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
    callback([NSDictionary dictionaryWithObject:allRetrivedPeripheral forKey:@"peripherals"]);
}

- (void)discoverService:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSArray *services, int errorCode))callback {
    self.discoverServiceCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        callback(NO,nil,1);
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
        callback(NO,nil,2);
    }
}

- (void)discoverCharacteristics:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSArray *characteristics, int errorCode))callback{
    self.discoverCharacteristicsCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        callback(NO,nil,1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        callback(NO,nil,2);
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            [peripheral discoverCharacteristics:nil forService:myService];
        } else {
            callback(NO,nil,3);
        }
    } else {
        callback(NO,nil,4);
    }
}

- (void)discoverDescriptorsForCharacteristic:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSArray *descriptors, int errorCode))callback {
    self.disDesForChaCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        callback(NO,nil,1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        callback(NO,nil,1);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        callback(NO,nil,3);
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
                callback(NO,nil,4);
            }
        } else {
            callback(NO,nil,5);
        }
    } else {
        callback(NO,nil,6);
    }
}

- (void)stopNotify {
    if (self.setNotifyCallback) {
        self.setNotifyCallback = nil;
    }
}

- (void)setNotify:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback {
    self.setNotifyCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.setNotifyCallback(NO,nil,1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        self.setNotifyCallback(NO,nil,2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        self.setNotifyCallback(NO,nil,3);
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
                self.setNotifyCallback(NO,nil,4);
            }
        } else {
            self.setNotifyCallback(NO,nil,5);
        }
    } else {
        self.setNotifyCallback(NO,nil,6);
    }
}

- (void)readValueForCharacteristic:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback {
    self.readForCharCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.readForCharCallback(NO, nil, 1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        self.readForCharCallback(NO, nil, 2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        self.readForCharCallback(NO, nil, 3);
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
                self.readForCharCallback(NO, nil, 4);
            }
        } else {
            self.readForCharCallback(NO, nil, 5);
        }
    } else {
        self.readForCharCallback(NO, nil, 6);
    }
}

- (void)readValueForDescriptor:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSDictionary *descriptor, int errorCode))callback {
    self.readForDescCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.readForDescCallback(NO, nil, 1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        self.readForDescCallback(NO, nil, 2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        self.readForDescCallback(NO, nil, 3);
        return;
    }
    NSString *descriptorUUID = [paramsDict_ stringValueForKey:@"descriptorUUID" defaultValue:nil];
    if (descriptorUUID.length == 0) {
        self.readForDescCallback(NO, nil, 4);
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
                    self.readForDescCallback(NO, nil, 5);
                }
            } else {
                self.readForDescCallback(NO, nil, 6);
            }
        } else {
            self.readForDescCallback(NO, nil, 7);
        }
    } else {
        self.readForDescCallback(NO, nil, 8);
    }
}

- (void)writeValueForCharacteristic:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback{
    self.writeForCharCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.writeForCharCallback(NO, nil, 1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        self.writeForCharCallback(NO, nil, 2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        self.writeForCharCallback(NO, nil, 3);
        return;
    }
    NSString *value = [paramsDict_ stringValueForKey:@"value" defaultValue:nil];
    if (value.length == 0) {
        self.writeForCharCallback(NO, nil, 4);
        return;
    }
    CBPeripheral *peripheral = [_allPeripheral objectForKey:peripheralUUID];
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        CBService *myService = [self getServiceWithPeripheral:peripheral andUUID:serviceUUID];
        if (myService) {
            CBCharacteristic *characteristic = [self getCharacteristicInService:myService withUUID:characteristicUUID];
            if(characteristic){
                NSData *valueData = [self dataFormHexString:value];
                if (valueData) {
                    CBCharacteristicWriteType type = CBCharacteristicWriteWithResponse;
                    if((characteristic.properties == CBCharacteristicPropertyWriteWithoutResponse)) {
                        type = CBCharacteristicWriteWithoutResponse;
                    } else if((characteristic.properties == CBCharacteristicPropertyWrite)) {
                        type = CBCharacteristicWriteWithResponse;
                    }
                    NSString *writeType = [paramsDict_ stringValueForKey:@"writeType" defaultValue:@""];
                    if (writeType.length > 0) {
                        if ([writeType isEqualToString:@"response"]) {
                            type = CBCharacteristicWriteWithResponse;
                        } else {
                            type = CBCharacteristicWriteWithoutResponse;
                        }
                    }
                    [peripheral writeValue:valueData forCharacteristic:characteristic type:type];
                }
            } else {
                self.writeForCharCallback(NO, nil, 5);
            }
        } else {
            self.writeForCharCallback(NO, nil, 6);
        }
    } else {
        self.writeForCharCallback(NO, nil, 7);
    }
}

- (void)writeValueForDescriptor:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSDictionary *descriptor, int errorCode))callback {
    self.writeForDescCallback = callback;
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        self.writeForDescCallback(NO, nil, 1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        self.writeForDescCallback(NO, nil, 2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        self.writeForDescCallback(NO, nil, 3);
        return;
    }
    NSString *descriptorUUID = [paramsDict_ stringValueForKey:@"descriptorUUID" defaultValue:nil];
    if (descriptorUUID.length == 0) {
        self.writeForDescCallback(NO, nil, 4);
        return;
    }
    NSString *value = [paramsDict_ stringValueForKey:@"value" defaultValue:nil];
    if (value.length == 0) {
        self.writeForDescCallback(NO, nil, 5);
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
                    self.writeForDescCallback(NO, nil, 6);
                }
            } else {
                self.writeForDescCallback(NO, nil, 7);
            }
        } else {
            self.writeForDescCallback(NO, nil, 8);
        }
    } else {
        self.writeForDescCallback(NO, nil, 9);
    }
}

- (void)connectPeripherals:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, NSString *peripheralUUID))callback{
    self.connectPeripherals = callback;
    NSArray *perAry = [paramsDict_ arrayValueForKey:@"peripheralUUIDs" defaultValue:@[]];
    if (perAry.count == 0) {
        self.connectPeripherals(NO, nil);
        return;
    }
    for (NSString *perUUID in perAry) {
        if ([perUUID isKindOfClass:[NSString class]] && perUUID.length>0) {
            CBPeripheral *peripheral = [_allPeripheral objectForKey:perUUID];
            if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
                if(peripheral.state  != CBPeripheralStateConnected) {
                    [_centralManager connectPeripheral:peripheral options:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES] forKey:CBConnectPeripheralOptionNotifyOnDisconnectionKey]];
                }
            }
        }
    }
}

- (void)setSimpleNotify:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(BOOL success, int code))callback {
    NSString *peripheralUUID = [paramsDict_ stringValueForKey:@"peripheralUUID" defaultValue:nil];
    if (peripheralUUID.length == 0) {
        callback(NO,1);
        return;
    }
    NSString *serviceUUID = [paramsDict_ stringValueForKey:@"serviceUUID" defaultValue:nil];
    if (serviceUUID.length == 0) {
        callback(NO,2);
        return;
    }
    NSString *characteristicUUID = [paramsDict_ stringValueForKey:@"characteristicUUID" defaultValue:nil];
    if (characteristicUUID.length == 0) {
        callback(NO,3);
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
                callback(NO,4);
            }
        } else {
            callback(NO,5);
        }
    } else {
        callback(NO,6);
    }
}

- (void)getAllSimpleNotifyData:(NSDictionary *)paramsDict_ callbackBlock:(void(^)(NSDictionary *data))callback {
    callback(_notifyPeripheralInfo);
}

- (void)clearAllSimpleNotifyData {
    if (_notifyPeripheralInfo) {
        [_notifyPeripheralInfo removeAllObjects];
        self.notifyPeripheralInfo = nil;
    }
    self.notifyPeripheralInfo = [NSMutableDictionary dictionary];
}
#pragma mark - CBPeripheralDelegate -

#pragma mark 按特征&描述符发送数据的回调-----------发送数据的回调
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    if(error){
        if (self.writeForCharCallback) {
            self.writeForCharCallback(NO, nil, -1);
        }
        if (self.writeForDescCallback) {
            self.writeForDescCallback(NO, nil, -1);
        }
        return;
    }
    NSMutableDictionary *characterDict = [self getCharacteristicsDict:characteristic];
    if (self.writeForCharCallback) {
        self.writeForCharCallback(YES, characterDict, 0);
    }
    if (self.writeForDescCallback) {
        self.writeForDescCallback(YES, characterDict, 0);
    }
}

#pragma mark 根据描述符读取数据---------------------接受数据的回调
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForDescriptor:(CBDescriptor *)descriptor error:(NSError *)error{
    NSString *descriptorUUID = descriptor.UUID.UUIDString;
    if (!descriptorUUID) {
        if (self.readForDescCallback) {
            self.readForDescCallback(NO, nil, -1);
        }
        return;
    }
    //[descriptorDict setValue:descriptorUUID forKey:@"descriptorUUID"];
    if(error) {
        if (self.readForDescCallback) {
            self.readForDescCallback(NO, nil, -1);
        }
        return;
    } else {
        NSMutableDictionary *descriptorsDict = [self getDescriptorInfo:descriptor];
        if (self.readForDescCallback) {
            self.readForDescCallback(YES, descriptorsDict, 0);
        }
    }
}

#pragma mark 监听外围设备后不断的有心跳数据包的回调-----监听数据的回调
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    /*
     NSData *characterData = characteristic.value;
     if (characterData) {
     NSString *value = [self hexStringFromData:characterData];
     NSLog(@"didUpdateValueForCharacteristic:******%@",value);
     }
     */
    if (peripheral && [peripheral isKindOfClass:[CBPeripheral class]]) {
        if(peripheral.state  != CBPeripheralStateConnected) {
            return;
        }
    }
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        if (self.readForCharCallback) {
            self.readForCharCallback(NO, nil, -1);
        }
        return;
    }
    //[characteristicDict setValue:characterUUID forKey:@"uuid"];
    if(error) {
        if (self.readForCharCallback) {
            self.readForCharCallback(NO, nil, -1);
        }
        if (self.setNotifyCallback) {
            self.setNotifyCallback(NO,nil,-1);
        }
    } else {
        NSMutableDictionary *characteristics = [self getCharacteristicsDict:characteristic];
        [characteristicDict setValue:characteristics forKey:@"characteristic"];
        [characteristicDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        //readValue
        if (self.readForCharCallback) {
            self.readForCharCallback(YES, characteristics, -1);
        }
        //setNotify
        if (self.setNotifyCallback) {
            self.setNotifyCallback(YES,characteristics,0);
        }
        //restoreData存储接收到的心跳数据包
        [self restoreNotifyData:peripheral withCharacteristic:characteristic];
    }
}

#pragma mark 是否监听外围设备后成功的回调---------------连接成功的回调
- (void)peripheral:(CBPeripheral *)peripheral didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)characteristic error:(nullable NSError *)error {
    //NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        if (self.setNotifyCallback) {
            self.setNotifyCallback(NO,nil,-1);
        }
        return;
    }
    //[characteristicDict setValue:characterUUID forKey:@"uuid"];
    if(error) {
        if (self.setNotifyCallback) {
            self.setNotifyCallback(NO,nil,-1);
        }
    } else {
        //NSMutableDictionary *characteristics = [self getCharacteristicsDict:characteristic];
        //[characteristicDict setValue:characteristics forKey:@"characteristic"];
        //[characteristicDict setValue:[NSNumber numberWithBool:YES] forKey:@"status"];
        //[self sendResultEventWithCallbackId:setNotifyCbid dataDict:characteristicDict errDict:nil doDelete:NO];
    }
}

#pragma mark 根据特征查找描述符-------------------------根据特征查找描述符的代理
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverDescriptorsForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    NSString *serviceUUID = characteristic.service.UUID.UUIDString;
    if (!serviceUUID) {
        self.disDesForChaCallback(NO,nil,-1);
        return;
    }
    NSString *characteristicUUID = characteristic.UUID.UUIDString;
    if (!characteristicUUID) {
        self.disDesForChaCallback(NO,nil,-1);
        return;
    }
    //[descriptorDict setValue:characteristicUUID forKey:@"characteristaicUUID"];
    //[descriptorDict setValue:serviceUUID forKey:@"serviceUUID"];
    if(error) {
        self.disDesForChaCallback(NO,nil,-1);
    } else {
        NSMutableArray *descriptors = [NSMutableArray array];
        for(CBDescriptor *descriptor in characteristic.descriptors) {
            [descriptors addObject:[self getDescriptorInfo:descriptor]];
        }
        self.disDesForChaCallback(YES,descriptors,0);
    }
}

#pragma mark 查询指定服务的所有特征-----------------------查询服务的特征的代理
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    NSString *serviceUUID = service.UUID.UUIDString;
    if (!serviceUUID) {
        self.discoverCharacteristicsCallback(NO,nil,-1);
        return;
    }
    //[serviceDict setValue:serviceUUID forKey:@"uuid"];
    /*
     for (CBCharacteristic *chara in service.characteristics) {
     NSString *uuid = chara.UUID.UUIDString;
     if (uuid && [uuid isEqualToString:@"FFF1"]) {
     [peripheral setNotifyValue:YES forCharacteristic:chara];
     }
     }
     */
    
    if(error) {
        self.discoverCharacteristicsCallback(NO,nil,-1);
    } else {
        NSArray *characteristics = [self getAllCharacteristicsInfoAry:service.characteristics];
        if (!characteristics) {
            characteristics = @[];
        }
        self.discoverCharacteristicsCallback(YES,characteristics,-1);
    }
}

#pragma mark 查询指定设备的所有服务------------------------查询设备的所有服务的代理
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    if(!peripheral.identifier) {
        return;
    }
    
    /*
     for (CBService *ser in peripheral.services) {
     NSString *uuid = ser.UUID.UUIDString;
     if (uuid && [uuid isEqualToString:@"FFF0"]) {
     [peripheral discoverCharacteristics:nil forService:ser];
     }
     }
     */
    
    if (!error) {
        NSMutableArray *services = [self getAllServiceInfoAry:peripheral.services];
        self.discoverServiceCallback(YES, services, 0);
    } else {
        self.discoverServiceCallback(NO, @[], -1);
    }
}

#pragma mark - CBCentralManagerDelegate -

#pragma mark 初始化中心设备管理器时返回其状态---------------app进入后台时的回调
- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    [self initManagerCallback:central.state];
}

#pragma mark app进入后台时的回调----------------------------app进入后台时的回调
//app状态的保存或者恢复，这是第一个被调用的方法当APP进入后台去完成一些蓝牙有关的工作设置，使用这个方法同步app状态通过蓝牙系统
- (void)centralManager:(CBCentralManager *)central willRestoreState:(NSDictionary<NSString *, id> *)dict {
    
}
#pragma mark 扫描设备的回调，大概每秒十次的频率在重复回调---发型周围设备回调
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *, id> *)advertisementData RSSI:(NSNumber *)RSSI {
    
    //NSString * name =  advertisementData[CBAdvertisementDataLocalNameKey];
    //自定义数据，可配合硬件工程师获取mac地址
    //    NSData *data = [advertisementData objectForKey:@"kCBAdvDataManufacturerData"];
    //    NSString *aStr = [self hexStringFromData:data];//[[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding];
    //    if ([aStr isKindOfClass:[NSString class]] && aStr.length>0) {
    //        NSLog(@"macStr:%@",aStr);
    //    }
    
    if (!peripheral.identifier) {
        return;
    }
    NSString *periphoeralUUID = peripheral.identifier.UUIDString;
    if (![periphoeralUUID isKindOfClass:[NSString class]] || periphoeralUUID.length<=0) {
        return;
    }
    //一个是GAP name,一个是一个 advertising name，设备没有连接外设时，获取的perpheral.name会是advertising name，然后当设备第一次连接成功外设后，GAP name就会被缓存下来，以后在连接时，获取的也都是GAP Name, 这样就造成了修改名称后苹果设备不更新的问题
    NSString *advertisingName =  advertisementData[CBAdvertisementDataLocalNameKey];if (![advertisingName isKindOfClass:[NSString class]] || advertisingName.length==0) {
        advertisingName = @"";
    }
    if([[_allPeripheral allValues] containsObject:peripheral]) {//更新旧设备的信号强度值
        NSMutableDictionary *targetPerInfo = [_allPeripheralInfo objectForKey:periphoeralUUID];
        if (targetPerInfo && RSSI) {
            [targetPerInfo setObject:RSSI forKey:@"rssi"];
        }
        [targetPerInfo setValue:advertisingName forKey:@"advertisingName"];
    } else {//发现新设备
        [_allPeripheral setObject:peripheral forKey:periphoeralUUID];
        NSMutableDictionary *peripheralInfo = [self getAllPeriphoerDict:peripheral];
        //自定义数据，可配合硬件工程师获取mac地址
        NSData *data = [advertisementData objectForKey:@"kCBAdvDataManufacturerData"];
        NSString *manufacturerStr = [self hexStringFromData:data];//[[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding];
        if (![manufacturerStr isKindOfClass:[NSString class]] || manufacturerStr.length==0) {
            manufacturerStr = @"";
        } 
        [peripheralInfo setValue:manufacturerStr forKey:@"manufacturerData"];
        [peripheralInfo setValue:advertisingName forKey:@"advertisingName"];
        if (RSSI) {
            [peripheralInfo setValue:RSSI forKey:@"rssi"];
        }
        [_allPeripheralInfo setObject:peripheralInfo forKey:periphoeralUUID];
    }
}

#pragma mark 连接外围设备成功后的回调------------------------连接成功的回调
- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    NSString *perUUID = peripheral.identifier.UUIDString;
    if (![perUUID isKindOfClass:[NSString class]] || perUUID.length==0) {
        perUUID = @"";
    }
    self.connectCallbackUUID(YES, 0,perUUID);
    if (self.connectPeripherals) {
        NSString *perUUID = peripheral.identifier.UUIDString;
        self.connectPeripherals(YES, perUUID);
    }
}

#pragma mark 连接外围设备失败后的回调------------------------连接失败的回调
- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(nullable NSError *)error {
    NSString *perUUID = peripheral.identifier.UUIDString;
    if (![perUUID isKindOfClass:[NSString class]] || perUUID.length==0) {
        perUUID = @"";
    }
    self.connectCallbackUUID(NO, -1, perUUID);
    
    if (self.connectPeripherals) {
        NSString *perUUID = peripheral.identifier.UUIDString;
        self.connectPeripherals(NO, perUUID);
    }
}
#pragma mark 断开通过uuid指定的外围设备的连接----------------断开连接的回调
- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(nullable NSError *)error {
    NSString *perUUID = peripheral.identifier.UUIDString;
    if (![perUUID isKindOfClass:[NSString class]] || perUUID.length==0) {
        perUUID = @"";
    }
    if (disconnectClick) {
        disconnectClick = NO;
        if (error) {
            self.disconnectCallback(NO, peripheral.identifier.UUIDString);
        } else {
            self.disconnectCallback(YES, peripheral.identifier.UUIDString);
        }
    } else {
        self.connectCallbackUUID(NO, -1, perUUID);
    }
}

#pragma mark - Utilities -
//收集监听的外网设备的数据信息
- (void)restoreNotifyData:(CBPeripheral *)peripheral withCharacteristic:(CBCharacteristic *)characteristic {
    NSString *peripheralUUID = peripheral.identifier.UUIDString;
    if (![peripheralUUID isKindOfClass:[NSString class]] || peripheralUUID.length==0) {
        return;
    }
    NSMutableDictionary *peripheralInfo = [self.notifyPeripheralInfo objectForKey:peripheralUUID];
    if (!peripheralInfo || peripheralInfo.count == 0) {
        peripheralInfo = [self getCharacteristicsData:characteristic];
    } else {
        NSMutableArray *perDataAry = [peripheralInfo objectForKey:@"data"];
        if (!perDataAry || perDataAry.count==0) {
            perDataAry = [NSMutableArray array];
        } else {
            NSData *characterData = characteristic.value;
            if (characterData) {
                NSString *value = [self hexStringFromData:characterData];
                [perDataAry addObject:value];
            }
        }
        [peripheralInfo setValue:perDataAry forKey:@"data"];
    }
    [self.notifyPeripheralInfo setObject:peripheralInfo forKey:peripheralUUID];
}
//收集监听的特征的数据信息
- (NSMutableDictionary *)getCharacteristicsData:(CBCharacteristic *)characteristic {
    NSMutableDictionary *characteristicDict = [NSMutableDictionary dictionary];
    NSString *characterUUID = characteristic.UUID.UUIDString;
    if (!characterUUID) {
        return characteristicDict;
    }
    NSString *serviceuuid = characteristic.service.UUID.UUIDString;
    if ([serviceuuid isKindOfClass:[NSString class]] && serviceuuid.length>0) {
        [characteristicDict setValue:serviceuuid forKey:@"serviceUUID"];
    }
    [characteristicDict setValue:characterUUID forKey:@"characterUUID"];
    NSData *characterData = characteristic.value;
    if (characterData) {
        NSString *value = [self hexStringFromData:characterData];
        NSMutableArray *datas = [NSMutableArray array];
        [datas addObject:value];
        [characteristicDict setValue:datas forKey:@"data"];
    }
    
    return characteristicDict;
}
//初始化蓝牙管理器回调
- (void)initManagerCallback:(CBManagerState)managerState {
    NSString *state = nil;
    switch (managerState) {
        case CBManagerStatePoweredOff://设备关闭状态
            state = @"poweredOff";
            break;
            
        case CBManagerStatePoweredOn:// 设备开启状态 -- 可用状态
            state = @"poweredOn";
            break;
            
        case CBManagerStateResetting://正在重置状态
            state = @"resetting";
            break;
            
        case CBManagerStateUnauthorized:// 设备未授权状态
            state = @"unauthorized";
            break;
            
        case CBManagerStateUnknown:// 初始的时候是未知的（刚刚创建的时候）
            state = @"unknown";
            break;
            
        case CBManagerStateUnsupported://设备不支持的状态
            state = @"unsupported";
            break;
            
        default:
            state = @"unknown";
            break;
    }
    self.initCallback(state);
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
    if (allServiceUid.count > 0) {
        [peripheralInfo setObject:allServiceUid forKey:@"services"];
    }
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
        NSString *value = [self hexStringFromData:characterData];
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

- (NSData *)replaceNoUtf8:(NSData *)data {
    char aa[] = {'A','A','A','A','A','A'};                      //utf8最多6个字符，当前方法未使用
    NSMutableData *md = [NSMutableData dataWithData:data];
    int loc = 0;
    while(loc < [md length])
    {
        char buffer;
        [md getBytes:&buffer range:NSMakeRange(loc, 1)];
        if((buffer & 0x80) == 0)
        {
            loc++;
            continue;
        }
        else if((buffer & 0xE0) == 0xC0)
        {
            loc++;
            [md getBytes:&buffer range:NSMakeRange(loc, 1)];
            if((buffer & 0xC0) == 0x80)
            {
                loc++;
                continue;
            }
            loc--;
            //非法字符，将这个字符（一个byte）替换为A
            [md replaceBytesInRange:NSMakeRange(loc, 1) withBytes:aa length:1];
            loc++;
            continue;
        }
        else if((buffer & 0xF0) == 0xE0)
        {
            loc++;
            [md getBytes:&buffer range:NSMakeRange(loc, 1)];
            if((buffer & 0xC0) == 0x80)
            {
                loc++;
                [md getBytes:&buffer range:NSMakeRange(loc, 1)];
                if((buffer & 0xC0) == 0x80)
                {
                    loc++;
                    continue;
                }
                loc--;
            }
            loc--;
            //非法字符，将这个字符（一个byte）替换为A
            [md replaceBytesInRange:NSMakeRange(loc, 1) withBytes:aa length:1];
            loc++;
            continue;
        }
        else
        {
            //非法字符，将这个字符（一个byte）替换为A
            [md replaceBytesInRange:NSMakeRange(loc, 1) withBytes:aa length:1];
            loc++;
            continue;
        }
    }
    
    return md;
}

- (NSString *)hexStringFromData:(NSData *)data {
    return [[[[NSString stringWithFormat:@"%@",data]
              stringByReplacingOccurrencesOfString: @"<" withString: @""]
             stringByReplacingOccurrencesOfString: @">" withString: @""]
            stringByReplacingOccurrencesOfString: @" " withString: @""];
}

- (NSData*)dataFormHexString:(NSString *)hexString {
    hexString=[[hexString uppercaseString] stringByReplacingOccurrencesOfString:@" " withString:@""];
    if (!(hexString && [hexString length] > 0 && [hexString length]%2 == 0)) {
        return nil;
    }
    Byte tempbyt[1]={0};
    NSMutableData* bytes=[NSMutableData data];
    for(int i=0;i<[hexString length];i++)
    {
        unichar hex_char1 = [hexString characterAtIndex:i]; ////两位16进制数中的第一位(高位*16)
        int int_ch1;
        if(hex_char1 >= '0' && hex_char1 <='9')
            int_ch1 = (hex_char1-48)*16;   //// 0 的Ascll - 48
        else if(hex_char1 >= 'A' && hex_char1 <='F')
            int_ch1 = (hex_char1-55)*16; //// A 的Ascll - 65
        else
            return nil;
        i++;
        
        unichar hex_char2 = [hexString characterAtIndex:i]; ///两位16进制数中的第二位(低位)
        int int_ch2;
        if(hex_char2 >= '0' && hex_char2 <='9')
            int_ch2 = (hex_char2-48); //// 0 的Ascll - 48
        else if(hex_char2 >= 'A' && hex_char2 <='F')
            int_ch2 = hex_char2-55; //// A 的Ascll - 65
        else
            return nil;
        
        tempbyt[0] = int_ch1+int_ch2;  ///将转化后的数放入Byte数组里
        [bytes appendBytes:tempbyt length:1];
    }
    return bytes;
}

@end
