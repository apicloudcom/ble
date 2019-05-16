//
//  BLESingle.h
//  UZApp
//
//  Created by 孙政篡 on 2017/12/15.
//  Copyright © 2017年 APICloud. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface BLESingle : NSObject

@property (assign, nonatomic) BOOL isSingleton;

+ (BLESingle *)sharedInstance;

- (void)initManager:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSString * state))callback;
- (void)scan:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success))callback;
- (void)getPeripheral:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSDictionary *sendDict))callback;
- (void)getPeripheralRssi:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, int errorCode, NSNumber *rssi))callback;
- (void)isScanning:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL status))callback;
- (void)stopScan;
- (void)connect:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL status, int erroCode, NSString *uuid))callback;
- (void)disconnect:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL status, NSString *perid))callback;
- (void)getPeripheralState:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSString *perid))callback;
- (void)isConnected:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL status, NSString *perid))callback;
- (void)retrievePeripheral:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSDictionary *perDict))callback;
- (void)retrieveConnectedPeripheral:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSDictionary *perDict))callback;
- (void)discoverService:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSArray *services, int errorCode))callback;
- (void)discoverCharacteristics:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSArray *characteristics, int errorCode))callback;
- (void)discoverDescriptorsForCharacteristic:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSArray *descriptors, int errorCode))callback;
- (void)stopNotify;
- (void)setNotify:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback;
- (void)readValueForCharacteristic:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback;
- (void)readValueForDescriptor:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSDictionary *descriptor, int errorCode))callback;
- (void)writeValueForCharacteristic:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSDictionary *characteristic, int errorCode))callback;
- (void)writeValueForDescriptor:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSDictionary *descriptor, int errorCode))callback;
- (void)connectPeripherals:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, NSString *peripheralUUID))callback;
- (void)setSimpleNotify:(NSDictionary *)paramsDict callbackBlock:(void(^)(BOOL success, int code))callback;
- (void)getAllSimpleNotifyData:(NSDictionary *)paramsDict callbackBlock:(void(^)(NSDictionary *data))callback;

- (void)clearAllSimpleNotifyData;

- (void)clean;

@end
