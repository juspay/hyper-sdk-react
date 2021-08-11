//
//  HyperSDKReact.m
//  HyperSDKReact
//
//  Copyright © Juspay. All rights reserved.
//

#import <Foundation/Foundation.h>

#import <React/RCTLog.h>
#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#import <HyperSDK/HyperSDK.h>

@interface HyperSdkReact : RCTEventEmitter <RCTBridgeModule>
@property HyperServices *hyperInstance;
@property UINavigationController *baseNavigationController;
@property UIViewController *baseViewController;
@end

@implementation RCT_EXTERN_MODULE(HyperSdkReact, RCTEventEmitter)

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup{
  return YES;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"HyperEvent"];
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

                self.baseViewController = [[UIViewController alloc] init];
                self.baseNavigationController = [[UINavigationController alloc] initWithRootViewController:self.baseViewController];
                self.baseNavigationController.modalPresentationStyle = UIModalPresentationOverFullScreen;
                self.baseNavigationController.navigationBar.hidden = true;
                __weak HyperSdkReact *weakSelf = self;
                [_hyperInstance initiate:self.baseViewController payload:jsonData callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    NSString *event = data[@"event"];
                    if ([event isEqualToString:@"process_result"]) {
                        [weakSelf.baseNavigationController dismissViewControllerAnimated:false completion:^{
                            [weakSelf sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
                        }];
                    } else {
                        [weakSelf sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
                    }
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
                if ([self.hyperInstance isInitialised] && self.baseNavigationController && ![self.baseNavigationController presentingViewController]) {
                    [RCTPresentedViewController() presentViewController:self.baseNavigationController animated:false completion:^{
                        [self.hyperInstance process:jsonData];
                    }];
                } else {
                    // Define proper error code and return proper error
                    // [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
                }
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

@end
