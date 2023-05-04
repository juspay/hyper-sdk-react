//
//  HyperSDKReact.m
//  HyperSDKReact
//
//  Copyright © Juspay. All rights reserved.
//

#import "HyperSdkReact.h"

#import <Foundation/Foundation.h>

#import <React/RCTLog.h>
#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#import <HyperSDK/HyperSDK.h>

@implementation HyperSdkReact
RCT_EXPORT_MODULE()

NSString *HYPER_EVENT = @"HyperEvent";

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup{
    return YES;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"HyperEvent"];
}

- (NSDictionary *)constantsToExport
{
 return @{ HYPER_EVENT: HYPER_EVENT };
}

// Will be called when this module's first listener is added.
-(void)startObserving {
    // Set up any upstream listeners or background tasks as necessary
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
    // Remove upstream listeners, stop unnecessary background tasks
}

RCT_EXPORT_METHOD(preFetch:(NSString *)data) {
    if (data && data.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                [HyperServices preFetch:jsonData];
            } else {

            }
        } @catch (NSException *exception) {
            //Parsing failure.
        }
    }
}

RCT_EXPORT_METHOD(createHyperServices) {
    self.hyperInstance = [HyperServices new];
}

RCT_EXPORT_METHOD(initiate:(NSString *)data) {
    if (data && data.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {

                UIViewController *baseViewController = RCTPresentedViewController();
                __weak HyperSdkReact *weakSelf = self;
                [_hyperInstance initiate:baseViewController payload:jsonData callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    [weakSelf sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
                }];
            } else {
                // Define proper error code and return proper error
                // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
            }
        } @catch (NSException *exception) {
            // Define proper error code and return proper error
            // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
        }
    } else {
        // Define proper error code and return proper error
        // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
    }
}

RCT_EXPORT_METHOD(process:(NSString *)data) {
    if (data && data.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                [self.hyperInstance process:jsonData];
            } else {
                // Define proper error code and return proper error
                // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
            }
        } @catch (NSException *exception) {
            // Define proper error code and return proper error
            // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
        }
    } else {
        // Define proper error code and return proper error
        // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
    }
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isNull) {
    return self.hyperInstance == NULL? @true : @false;
}

RCT_EXPORT_METHOD(terminate) {
    if (_hyperInstance) {
        [_hyperInstance terminate];
    }
}

RCT_EXPORT_METHOD(isInitialised:(RCTPromiseResolveBlock)resolve  reject:(RCTPromiseRejectBlock)reject) {
    if (self.hyperInstance) {
        resolve(self.hyperInstance.isInitialised? @true : @false);
    } else {
        resolve(@false);
    }
}

RCT_EXPORT_METHOD(updateBaseViewController) {
    if (self.hyperInstance && [self.hyperInstance isInitialised]) {
        self.hyperInstance.baseViewController = RCTPresentedViewController();
    }
}

+ (NSDictionary*)stringToDictionary:(NSString*)string{
    if (string.length<1) {
        return @{};
    }
    NSError *error;
    NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
    id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    if (error) {}
    return json;
}

+ (NSString*)dictionaryToString:(id)dict{
    if (!dict || ![NSJSONSerialization isValidJSONObject:dict]) {
        return @"";
    }
    NSString *data = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:dict options:0 error:nil] encoding:NSUTF8StringEncoding];
    return data;
}


// Don't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
(const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeHyperSdkReactSpecJSI>(params);
}
#endif

@end
