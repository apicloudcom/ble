//
//  UZModule.h
//  UZEngine
//
//  Created by broad on 13-11-6.
//  Copyright (c) 2013年 APICloud. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

typedef enum _KUZStringType {
    kUZStringType_JSON = 0,
    kUZStringType_TEXT,
} KUZStringType;

@class UZWebView;
@protocol UIViewControllerRotation;
@protocol UZWebViewSupportLayer;

@interface UZModule : NSObject

@property (nonatomic, weak, readonly) UZWebView <UZWebViewSupportLayer>*uzWebView;
@property (nonatomic, weak, readonly) UIViewController *viewController;
@property (nonatomic, weak, readonly) UINavigationController *navigationController;
@property (nonatomic, readonly) NSString *widgetId;

#pragma mark - lifeCycle
// 重载下面方法来执行初始化和释放操作
- (id)initWithUZWebView:(id)webView;
- (void)dispose;

#pragma mark - callback
/**
 执行回调方法返回数据
 
 @param cbId 回调函数id
 
 @param dataDict 返回的数据
 
 @param errDict 错误信息
 
 @param doDelete 执行回调后是否删除回调函数对象
 */
- (void)sendResultEventWithCallbackId:(NSInteger)cbId
                             dataDict:(NSDictionary *)dataDict
                              errDict:(NSDictionary *)errDict
                             doDelete:(BOOL)doDelete;

/**
 执行回调方法返回数据，回调函数里面的ret可以为字符串或JSON对象
 
 @param cbId 回调函数id
 
 @param dataString 返回的数据
 
 @param strType 返回数据类型
 
 @param errDict 错误信息
 
 @param doDelete 执行回调后是否删除函数对象
 */
- (void)sendResultEventWithCallbackId:(NSInteger)cbId
                           dataString:(NSString *)dataString
                           stringType:(KUZStringType)strType
                              errDict:(NSDictionary *)errDict
                             doDelete:(BOOL)doDelete;

/**
 删除不再使用的回调函数对象
 
 @param cbId 要删除的回调函数id
 */
- (void)deleteCallback:(NSInteger)cbId;

/**
 在当前页面执行一段javascript代码
 
 @param js 要执行的javascript代码
 */
- (void)evalJs:(NSString *)js;
- (void)evalJs:(NSString *)js completionHandler:(void (^)(id result, NSError *error))completionHandler;

#pragma mark - methods

/**
 将包含自定义协议的路径转换成绝对路径
 
 @param url 源路径，可能包含APICloud自定义协议路径，如'fs://', 'widget://', 'cache://'等
 
 @return 转换后的绝对路径
 */
- (NSString *)getPathWithUZSchemeURL:(NSString *)url;

//isAssetsLibrary判断是否为资源库里引用路径
- (NSString *)getPathWithUZSchemeURL:(NSString *)url isAssetsLibrary:(BOOL*)isAssetsLibrary;

/**
 从加密的key.xml文件中获取解密后的数据
 
 @param key 加密字段
 
 @return 解密后的数据，如果获取失败则返回nil
 */
- (NSString *)securityValueForKey:(NSString *)key;

/**
 获取config.xml里面指定模块的配置信息
 
 @return 模块配置信息
 */
- (NSDictionary *)getFeatureByName:(NSString *)name;

/**
 当前应用是否支持沉浸式效果，可以在config.xml里面进行配置：<preference name="statusBarAppearance" value="true" />
 
 @return 是否支持沉浸式效果
 */
- (BOOL)statusBarAppearance;

/**
 获取指定窗口对象
 
 @param name 窗口名字
 
 @return 窗口对象
 */
- (UIView *)getViewByName:(NSString *)name;

/**
 在指定窗口上面添加视图
 
 @param view 视图，
 
 @param fixedOn 目标窗口名字，默认为主窗口名字
 
 @param fixed 视图是否固定，为NO时跟随目标窗口内容滚动而滚动
 
 @return 添加视图是否成功，若fixedOn对应子窗口未找到则返回失败
 */
- (BOOL)addSubview:(UIView *)view fixedOn:(NSString *)fixedOn fixed:(BOOL)fixed;

/**
 设置视图约束。调用此方法时请确保视图已经添加到父视图上面。
 
 @param view 视图
 
 @param rect 视图位置信息，参考api.openFrame方法的rect参数，https://docs.apicloud.com/Client-API/api#27
 */
- (void)view:(UIView *)view addConstraintsWithRect:(NSDictionary *)rect;
- (void)view:(UIView *)view updateConstraintsWithRect:(NSDictionary *)rect;

/**
 设置视图是否屏蔽侧滑布局滑动手势
 
 @param view 视图
 */
- (void)view:(UIView *)view preventSlidPaneGesture:(BOOL)prevent;

/**
 设置视图是否屏蔽滑动返回手势
 
 @param view 视图
 */
- (void)view:(UIView *)view preventSlidBackGesture:(BOOL)prevent;

/**
 发送自定义事件，页面可以通过api.addEventListener方法监听
 
 @param name 事件名称
 
 @param extra 附加信息
 */
- (void)sendCustomEvent:(NSString *)name extra:(id)extra;

/**
 调试模式下显示错误信息，需在config.xml配置<preference name="debug" value="true"/>
 
 @param message 错误信息
 */
+ (void)showErrorMessage:(NSString *)message;

/**
 如果需要在应用启动时做一些操作，子类可以实现该方法来处理。
 
 @param launchOptions 应用启动时的参数信息
 */
+ (void)onAppLaunch:(NSDictionary *)launchOptions;

@end

@interface UZModule (Scroll)

// 当前页面的scrollView，注意不要直接设置scrollView的delegate，通过setWebViewScrollDelegate:方法来设置
@property (nonatomic, readonly) UIScrollView *scrollView;

/**
 设置页面滚动代理，可以通过实现UIScrollViewDelegate的相关滚动方法来实现自定义上拉、下拉刷新等功能
 
 @param delegate 滚动代理
 */
- (void)setWebViewScrollDelegate:(id<UIScrollViewDelegate>)delegate;

/** 前端html页面里面可以指定要使用的下拉刷新模块，下拉刷新模块里面需要实现以下三个方法，当在html页面里面调用api.setCustomRefreshHeaderInfo()、api.refreshHeaderLoading()、api.refreshHeaderLoadDone()方法时，以下几个方法将会被调用，模块需要自行处理事件及回调。请勿主动调用这些方法。
 */
- (void)setCustomRefreshHeaderInfo:(NSDictionary *)param;
- (void)refreshHeaderLoading:(NSDictionary *)param;
- (void)refreshHeaderLoadDone:(NSDictionary *)param;

@end

@interface UZModule (Rotation)

/**
 设置是否允许旋转，该设置为全局设置
 
 @param autorotate 是否允许旋转，默认为YES
 */
- (void)setShouldAutorotate:(BOOL)autorotate;

/**
 设置屏幕旋转方向
 
 @param param 旋转方向，和api.setScreenOrientation方法参数一致，详情参考文档http://docs.apicloud.com/%E7%AB%AFAPI/api#66
 */
- (void)setScreenOrientation:(NSDictionary *)param;

/**
 设置控制器旋转代理，监听控制器旋转事件
 
 @param delegate 控制器旋转代理
 */
- (void)setViewControllerRotationDelegate:(id<UIViewControllerRotation>)delegate;

@end


@interface UZModuleMethodContext : NSObject

@property (nonatomic, weak, readonly) UZModule *module;
@property (nonatomic, strong, readonly) NSDictionary *param;
@property (nonatomic, readonly) NSInteger cbId;

- (id)initWithModule:(UZModule *)module param:(NSDictionary *)param callback:(NSInteger)cbId;
- (void)callbackWithRet:(id)ret err:(id)err delete:(BOOL)del;

@end


#define JS_METHOD(method)   \
- (void)jsmethod_##method

#define JS_METHOD_SYNC(method)   \
- (id)jsmethod_sync_##method

#define JS_PROPERTY(method)   \
- (id)jsproperty_##method

extern NSString *const UZModuleMethodPrefix;
extern NSString *const UZModuleSyncMethodPrefix;
extern NSString *const UZModulePropertyPrefix;
