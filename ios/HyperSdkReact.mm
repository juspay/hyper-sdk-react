/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "HyperSdkReact.h"

#import <Foundation/Foundation.h>

#import <React/RCTLog.h>
#import <React/RCTConvert.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTModalHostViewController.h>
#import <React/RCTRootView.h>

#import <HyperSDK/HyperSDK.h>

__weak static HyperServices *_hyperServicesReference;

// Overriding the RCTRootView to add contraints to align with the views superview
@implementation SDKRootView

-(void)didMoveToSuperview {
    // Remove old leading anchor
    if (self.leading.isActive) {
        self.leading.active = @NO;
    }
    // Remove old trailing anchor
    if (self.trailing.isActive) {
        self.trailing.active = @NO;
    }
    
    //Checking superview just to be sure that it is not nil
    if(self.superview) {
        // Create contraints to replicate wrapcontent
        self.leading = [self.leadingAnchor constraintEqualToAnchor:self.superview.leadingAnchor];
        self.trailing = [self.trailingAnchor constraintEqualToAnchor:self.superview.trailingAnchor];
        // Save contraints so that it can be removed if there is superview is changed.
        // This should not happen as per usecase
        self.leading.active = @YES;
        self.trailing.active = @YES;
    }
}

@end


@implementation SdkDelegate

NSMutableSet<NSString *> *registeredComponents = [[NSMutableSet alloc] init];

- (id)initWithBridge:(RCTBridge *)bridge {
    // Hold references to all merchant views provided to the sdk
    self.rootHolder = [[NSMutableDictionary alloc] init];
    // Hold latest vaule of height provided by react
    self.heightHolder = [[NSMutableDictionary alloc] init];
    // Hold reference to latest constraints so that they can be replaced if height is modified
    self.heightConstraintHolder = [[NSMutableDictionary alloc] init];
    // Hold reference to bridge so that RCTRootViews can share JS VM
    self.bridge = bridge;
    return self;
}

/**
 Create / replace height constraint given to set height of the view provided by the merchant
 */
- (void) setHeight: (NSNumber*)height forTag: (NSString * _Nonnull)tag {
    // Update the latest value of the height holder for the given tag
    // This will be used to set the height of view if view is created at a later point
    [self.heightHolder setObject: height forKey:tag];
    
    // Fetch previous height constraint so that it can be set to inactive
    NSLayoutConstraint *heightConstraint = [self.heightConstraintHolder objectForKey:tag];
    // Fetch rootview to update set constraints if view is already created
    UIView *rootView = [self.rootHolder objectForKey:tag];
    
    // Check if view is already present
    if (rootView && [rootView isKindOfClass: [UIView class]]) {
        // If present set earlier constraint to inactive
        if (heightConstraint && [heightConstraint isKindOfClass:[NSLayoutConstraint class]]) {
            heightConstraint.active = @NO;
        }
        // Set a new constraint with the latest height
        NSLayoutConstraint *newHeightConstraint = [rootView.heightAnchor constraintEqualToConstant: [height doubleValue]];
        newHeightConstraint.active = @YES;
        // Save the constraint so that it can be made inactive if a new constraint is created
        [self.heightConstraintHolder setObject:newHeightConstraint forKey:tag];
    }
}

/**
 Create a react root view
 Set height if available
 Use bridge to share the same JS VM
 */
- (UIView * _Nullable)merchantViewForViewType:(NSString * _Nonnull)viewType {
    
    // Create a SDKRootView so that we can attach width constraints once it is attached to it's parent
    RCTRootView *rrv = [SDKRootView alloc];
    NSString *moduleName = @"JP_003";
    if ([viewType isEqual:@"HEADER"] && [registeredComponents containsObject:@"JuspayHeader"]) {
        moduleName = @"JuspayHeader";
    } else if ([viewType isEqual:@"HEADER_ATTACHED"] && [registeredComponents containsObject:@"JuspayHeaderAttached"]) {
        moduleName = @"JuspayHeaderAttached";
    } else if ([viewType isEqual:@"FOOTER"] && [registeredComponents containsObject:@"JuspayFooter"]) {
        moduleName = @"JuspayFooter";
    } else if ([viewType isEqual:@"FOOTER_ATTACHED"] && [registeredComponents containsObject:@"JuspayFooterAttached"]) {
        moduleName = @"JuspayFooterAttached";
    }
    
    // Save a reference of the react root view
    // This will be used to update height constraint if a newer value is sent by the merchant
    [self.rootHolder setObject:rrv forKey:moduleName];
    
    
    rrv = [rrv initWithBridge: self.bridge
                   moduleName:moduleName
            initialProperties:nil
    ];
    
    // Remove background colour. Default colour white is getting applied to the merchant view
    rrv.backgroundColor = UIColor.clearColor ;
    
    // Remove height 0, width 0 constraints added by default.
    rrv.translatesAutoresizingMaskIntoConstraints = false;
    
    // If height is available set the height
    NSNumber *height = [self.heightHolder objectForKey:moduleName];
    if (height && [height isKindOfClass:[NSNumber class]]) {
        NSLayoutConstraint *heightConstriant = [rrv.heightAnchor constraintEqualToConstant: [height doubleValue]];
        heightConstriant.active = @YES;
        [self.heightConstraintHolder setObject:heightConstriant forKey:moduleName];
    }
    // This is sent to hypersdk. Hyper sdk adds the view to it's heirarchy and set's superview's top and bottom to match rrv's top and bottom
    return rrv;
}

- (void) onWebViewReady:(WKWebView *)webView {
    //Ignored
}

@end

@implementation HyperSdkReact
RCT_EXPORT_MODULE()

NSString *HYPER_EVENT = @"HyperEvent";
NSString *JUSPAY_HEADER = @"JuspayHeader";
NSString *JUSPAY_FOOTER = @"JuspayFooter";
NSString *JUSPAY_HEADER_ATTACHED = @"JuspayHeaderAttached";
NSString *JUSPAY_FOOTER_ATTACHED = @"JuspayFooterAttached";

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
    return @{ HYPER_EVENT: HYPER_EVENT
              , JUSPAY_HEADER : JUSPAY_HEADER
              , JUSPAY_HEADER_ATTACHED : JUSPAY_HEADER_ATTACHED
              , JUSPAY_FOOTER : JUSPAY_FOOTER
              , JUSPAY_FOOTER_ATTACHED : JUSPAY_FOOTER_ATTACHED
    };
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
    if (self.hyperInstance == NULL) {
        self.hyperInstance = [HyperServices new];
        _hyperServicesReference = self.hyperInstance;
    }
}

RCT_EXPORT_METHOD(initiate:(NSString *)data) {
    if (data && data.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                
                UIViewController *baseViewController = RCTPresentedViewController();
                __weak HyperSdkReact *weakSelf = self;
                self.delegate = [[SdkDelegate alloc] initWithBridge:self.bridge];
                [_hyperInstance setHyperDelegate: _delegate];
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
            // Update baseViewController if it's nil or not in the view hierarchy.
            if (self.hyperInstance.baseViewController == nil || self.hyperInstance.baseViewController.view.window == nil) {
                // Getting topViewController
                id baseViewController = RCTPresentedViewController();
                
                // Set the presenting ViewController as baseViewController if the topViewController is RCTModalHostViewController.
                if ([baseViewController isMemberOfClass:RCTModalHostViewController.class] && [baseViewController presentingViewController]) {
                    [self.hyperInstance setBaseViewController:[baseViewController presentingViewController]];
                } else {
                    [self.hyperInstance setBaseViewController:baseViewController];
                }
            }
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

RCT_EXPORT_METHOD(notifyAboutRegisterComponent:(NSString *)viewType) {
    [registeredComponents addObject:viewType];
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

RCT_EXPORT_METHOD(updateMerchantViewHeight: (NSString * _Nonnull) tag height: (NSNumber * _Nonnull) h) {
    if (self.delegate) {
        [((SdkDelegate *) self.delegate) setHeight:h forTag:tag];
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

@implementation HyperFragmentViewManagerIOS
RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup{
    return YES;
}

- (UIView *)view
{
    return [[UIView alloc] init];
}

RCT_EXPORT_METHOD(receiveCommand:(nonnull NSNumber *)viewTag nameSpace:(NSString *)nameSpace payload:(NSString *)payload)
{
    HyperServices *hyperServicesInstance = _hyperServicesReference;
    if (payload && payload.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:payload];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                    if (hyperServicesInstance.baseViewController == nil || hyperServicesInstance.baseViewController.view.window == nil) {
                        id baseViewController = RCTPresentedViewController();
                        if ([baseViewController isMemberOfClass:RCTModalHostViewController.class] && [baseViewController presentingViewController]) {
                            [hyperServicesInstance setBaseViewController:[baseViewController presentingViewController]];
                        } else {
                            [hyperServicesInstance setBaseViewController:baseViewController];
                        }
                    }
                    UIView *view = viewRegistry[viewTag];
                    [self manuallyLayoutChildren:view];
                    if (!view || ![view isKindOfClass:[UIView class]]) {
                        RCTLogError(@"Cannot find NativeViewManager with tag #%@", viewTag);
                        return;
                    }
                    NSMutableDictionary *nestedPayload = [jsonData[@"payload"] mutableCopy];
                    NSDictionary *fragmentViewGroup = @{nameSpace: view};
                    nestedPayload[@"fragmentViewGroups"] = fragmentViewGroup;
                    NSMutableDictionary *updatedJsonData = [jsonData mutableCopy];
                    updatedJsonData[@"payload"] = nestedPayload;
                    [hyperServicesInstance process:[updatedJsonData copy]];
                }];
            } else {}
        } @catch (NSException *exception) {}
    } else {}
}

- (void)manuallyLayoutChildren:(UIView *)view {
    UIView *parent = view.superview;
    if (!parent) return;
    
    view.frame = parent.bounds;
}

@end
