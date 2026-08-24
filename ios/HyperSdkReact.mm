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

#import "HyperMerchantView.h"

__weak static HyperServices *_hyperServicesReference;


@implementation MerchantViewRoot

- (void)layoutSubviews {
  [super layoutSubviews];
}

- (void)didMoveToSuperview {
  [super didMoveToSuperview];

  if (_leading.isActive) _leading.active = NO;
  if (_trailing.isActive) _trailing.active = NO;

  if (self.superview) {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    _leading = [self.leadingAnchor constraintEqualToAnchor:self.superview.leadingAnchor];
    _trailing = [self.trailingAnchor constraintEqualToAnchor:self.superview.trailingAnchor];
    _leading.active = YES;
    _trailing.active = YES;
  }

  for (UIView *subview in self.subviews) {
    subview.translatesAutoresizingMaskIntoConstraints = NO;
    [subview.leadingAnchor constraintEqualToAnchor:self.leadingAnchor].active = YES;
    [subview.trailingAnchor constraintEqualToAnchor:self.trailingAnchor].active = YES;
    [subview.topAnchor constraintEqualToAnchor:self.topAnchor].active = YES;
    [subview.heightAnchor constraintEqualToAnchor:self.heightAnchor].active = YES;
  }
}

@end

// Overriding the RCTRootView to add contraints to align with the views superview
@implementation SDKRootView

-(void)didMoveToSuperview {
    // Remove old leading anchor
    if (self.leading.isActive) {
        self.leading.active = false;
    }
    // Remove old trailing anchor
    if (self.trailing.isActive) {
        self.trailing.active = false;
    }

    //Checking superview just to be sure that it is not nil
    if(self.superview) {
        // Create contraints to replicate wrapcontent
        self.leading = [self.leadingAnchor constraintEqualToAnchor:self.superview.leadingAnchor];
        self.trailing = [self.trailingAnchor constraintEqualToAnchor:self.superview.trailingAnchor];
        // Save contraints so that it can be removed if there is superview is changed.
        // This should not happen as per usecase
        self.leading.active = true;
        self.trailing.active = true;
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
            heightConstraint.active = false;
        }
        // Set a new constraint with the latest height
        NSLayoutConstraint *newHeightConstraint = [rootView.heightAnchor constraintEqualToConstant: [height doubleValue]];
        newHeightConstraint.active = true;
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
    NSString *standardName = nil;
    if ([viewType isEqual:@"HEADER"]) {
        standardName = @"JuspayHeader";
    } else if ([viewType isEqual:@"HEADER_ATTACHED"]) {
        standardName = @"JuspayHeaderAttached";
    } else if ([viewType isEqual:@"FOOTER"]) {
        standardName = @"JuspayFooter";
    } else if ([viewType isEqual:@"FOOTER_ATTACHED"]) {
        standardName = @"JuspayFooterAttached";
    }

    NSString *moduleName = @"JP_003";
    if (standardName != nil) {
        // A component registered for this specific instance wins over the process-wide registration.
        NSString *instanceComponent = self.componentMapping[standardName];
        if (instanceComponent != nil) {
            moduleName = instanceComponent;
        } else if ([registeredComponents containsObject:standardName]) {
            moduleName = standardName;
        }
    }

    void (^addHeightConstraint)(UIView *);
    
    addHeightConstraint = ^void(UIView *merchantView) {
        NSNumber *height = [self.heightHolder objectForKey:moduleName];
        if (height && [height isKindOfClass:[NSNumber class]]) {
            NSLayoutConstraint *heightConstriant = [merchantView.heightAnchor constraintEqualToConstant: [height doubleValue]];
            heightConstriant.active = true;
            [self.heightConstraintHolder setObject:heightConstriant forKey:moduleName];
        }
    };

    UIView *(^oldArchCall)();

    oldArchCall = ^UIView *() {
        // Save a reference of the react root view
        // This will be used to update height constraint if a newer value is sent by the merchant
        RCTRootView *rrv = [SDKRootView alloc];
        [self.rootHolder setObject:rrv forKey:moduleName];
        rrv = [rrv initWithBridge: self.bridge
                       moduleName:moduleName
                initialProperties:nil
        ];
        
        // Remove background colour. Default colour white is getting applied to the merchant view
        rrv.backgroundColor = UIColor.clearColor ;

        // Remove height 0, width 0 constraints added by default.
        rrv.translatesAutoresizingMaskIntoConstraints = false;

        addHeightConstraint(rrv);

        // This is sent to hypersdk. Hyper sdk adds the view to it's heirarchy and set's superview's top and bottom to match rrv's top and bottom
        return rrv;
    };

    UIView *rrv = [HyperMerchantView createReactNativeViewWithModuleName:moduleName];

    if (rrv == nil) {
        return oldArchCall();
    }
   MerchantViewRoot *wrapper = [[MerchantViewRoot alloc] init];
   [wrapper addSubview:rrv];

    
    // Remove background colour. Default colour white is getting applied to the merchant view
    wrapper.backgroundColor = UIColor.clearColor ;
    
    // Remove height 0, width 0 constraints added by default.
    wrapper.translatesAutoresizingMaskIntoConstraints = false;
    
    rrv.translatesAutoresizingMaskIntoConstraints = false;

    [self.rootHolder setObject:wrapper forKey:moduleName];
    addHeightConstraint(wrapper);


    // This is sent to hypersdk. Hyper sdk adds the view to it's heirarchy and set's superview's top and bottom to match rrv's top and bottom
    return wrapper;
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

- (instancetype)init {
  if (self = [super init]) {
    _hyperServicesDict = [NSMutableDictionary new];
    _hyperDelegatesDict = [NSMutableDictionary new];
    _keyedComponentsDict = [NSMutableDictionary new];
    _knownEventKeys = [NSMutableSet new];
  }
  return self;
}

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup{
    return YES;
}

- (void)invalidate {
    NSArray *keyedInstances;
    @synchronized (self.hyperServicesDict) {
        keyedInstances = self.hyperServicesDict.allValues;
        [self.hyperServicesDict removeAllObjects];
        [self.hyperDelegatesDict removeAllObjects];
        [self.keyedComponentsDict removeAllObjects];
    }
    // Terminate on the main queue: the SDK drives UIKit. The legacy hyperInstance is left
    // untouched, matching the Android module and the pre-existing single-instance behaviour.
    dispatch_async(dispatch_get_main_queue(), ^{
        for (HyperServices *hyperServices in keyedInstances) {
            @try {
                [hyperServices terminate];
            } @catch (NSException *exception) {
                // Ignored
            }
        }
    });
    [super invalidate];
}

- (NSArray<NSString *> *)supportedEvents {
    NSMutableArray<NSString *> *events = [NSMutableArray arrayWithObject:@"HyperEvent"];
    @synchronized (self.hyperServicesDict) {
        [events addObjectsFromArray:self.knownEventKeys.allObjects];
    }
    return events;
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

RCT_EXPORT_METHOD(createHyperServicesWithKey:(NSString *)key tenantId:(NSString *)tenantId clientId:(NSString *)clientId) {
    @synchronized (self.hyperServicesDict) {
        if (key == nil || [key isEqualToString:@"HyperEvent"] || self.hyperServicesDict[key] != nil) {
            return;
        }
        HyperServices *hyperServices;
        if (tenantId.length > 0 && clientId.length > 0) {
            hyperServices = [[HyperServices new] initWithTenantId:tenantId clientId:clientId];
        } else {
            hyperServices = [HyperServices new];
        }
        [self.hyperServicesDict setObject:hyperServices forKey:key];
        [self.knownEventKeys addObject:key];
        if (self.keyedComponentsDict[key] == nil) {
            [self.keyedComponentsDict setObject:[NSMutableDictionary new] forKey:key];
        }
    }
}

RCT_EXPORT_METHOD(createHyperServicesWithTenantId:(NSString *)tenantId clientId:(NSString *)clientId) {
    if (self.hyperInstance == NULL) {
      self.hyperInstance = [[HyperServices new] initWithTenantId:tenantId clientId:clientId];
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

RCT_EXPORT_METHOD(initiateWithKey:(NSString *)data key:(NSString *)key) {
    if (data && data.length>0) {
        @try {
            HyperServices *hyperServices;
            @synchronized (self.hyperServicesDict) {
                hyperServices = key != nil ? self.hyperServicesDict[key] : nil;
            }
            if (hyperServices == nil) {
                return;
            }
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {

                UIViewController *baseViewController = RCTPresentedViewController();
                __weak HyperSdkReact *weakSelf = self;
                SdkDelegate *delegate = [[SdkDelegate alloc] initWithBridge:self.bridge];
                @synchronized (self.hyperServicesDict) {
                    delegate.componentMapping = self.keyedComponentsDict[key];
                    [self.hyperDelegatesDict setObject:delegate forKey:key];
                }
                [hyperServices setHyperDelegate: delegate];
                [hyperServices initiate:baseViewController payload:jsonData callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    [weakSelf sendEventWithName:key body:[[self class] dictionaryToString:data]];
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
            if (self.hyperInstance.baseViewController == nil || self.hyperInstance.baseViewController.view.window == nil || [HyperSdkReact isRCTModalHostViewController:self.hyperInstance.baseViewController]) {
                // Getting topViewController
                id baseViewController = RCTPresentedViewController();
                
                // Set the presenting ViewController as baseViewController if the topViewController is RCTModalHostViewController.
                if ([HyperSdkReact isRCTModalHostViewController:baseViewController] && [baseViewController presentingViewController]) {
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

RCT_EXPORT_METHOD(processWithKey:(NSString *)data key:(NSString *)key) {
    if (data && data.length>0) {
        @try {
            HyperServices *hyperServices;
            @synchronized (self.hyperServicesDict) {
                hyperServices = key != nil ? self.hyperServicesDict[key] : nil;
            }
            if (hyperServices == nil) {
                return;
            }
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:data];
            // Update baseViewController if it's nil or not in the view hierarchy.
            if (hyperServices.baseViewController == nil || hyperServices.baseViewController.view.window == nil || [HyperSdkReact isRCTModalHostViewController:hyperServices.baseViewController]) {
                // Getting topViewController
                id baseViewController = RCTPresentedViewController();

                // Set the presenting ViewController as baseViewController if the topViewController is RCTModalHostViewController.
                if ([HyperSdkReact isRCTModalHostViewController:baseViewController] && [baseViewController presentingViewController]) {
                    [hyperServices setBaseViewController:[baseViewController presentingViewController]];
                } else {
                    [hyperServices setBaseViewController:baseViewController];
                }
            }
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                [hyperServices process:jsonData];
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

RCT_EXPORT_METHOD(openPaymentPage:(NSString *)data) {
    if (data && data.length>0) {
        @try {
            NSDictionary *sdkPayload = [HyperSdkReact stringToDictionary:data];
            // Update baseViewController if it's nil or not in the view hierarchy.
            if (sdkPayload && [sdkPayload isKindOfClass:[NSDictionary class]] && sdkPayload.allKeys.count>0) {

                id baseViewController = RCTPresentedViewController();
                              
                __weak HyperSdkReact *weakSelf = self;
                self.delegate = [[SdkDelegate alloc] initWithBridge:self.bridge];
                [_hyperInstance setHyperDelegate: _delegate];
                [HyperCheckoutLite openPaymentPage:baseViewController payload:sdkPayload callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    [weakSelf sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
                }];
            } else {
//                 Define proper error code and return proper error
//                 [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
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

RCT_EXPORT_METHOD(openPaymentPageWithKey:(NSString *)data key:(NSString *)key) {
    if (data && data.length>0) {
        @try {
            HyperServices *hyperServices;
            @synchronized (self.hyperServicesDict) {
                hyperServices = key != nil ? self.hyperServicesDict[key] : nil;
            }
            NSDictionary *sdkPayload = [HyperSdkReact stringToDictionary:data];
            // Update baseViewController if it's nil or not in the view hierarchy.
            if (sdkPayload && [sdkPayload isKindOfClass:[NSDictionary class]] && sdkPayload.allKeys.count>0) {

                id baseViewController = RCTPresentedViewController();

                __weak HyperSdkReact *weakSelf = self;
                SdkDelegate *delegate = [[SdkDelegate alloc] initWithBridge:self.bridge];
                @synchronized (self.hyperServicesDict) {
                    delegate.componentMapping = self.keyedComponentsDict[key];
                    [self.hyperDelegatesDict setObject:delegate forKey:key];
                }
                [hyperServices setHyperDelegate: delegate];
                [HyperCheckoutLite openPaymentPage:baseViewController payload:sdkPayload callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    [weakSelf sendEventWithName:key body:[[self class] dictionaryToString:data]];
                }];
            } else {
//                 Define proper error code and return proper error
//                 [self sendEventWithName:@"HyperEvent" body:[[self class] dictionaryToString:data]];
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

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isNullWithKey:(NSString *)key) {
    HyperServices *hyperServices;
    @synchronized (self.hyperServicesDict) {
        hyperServices = key != nil ? self.hyperServicesDict[key] : nil;
    }
    return hyperServices == NULL? @true : @false;
}

RCT_EXPORT_METHOD(terminateWithKey:(NSString *)key) {
    if (key == nil) {
        return;
    }
    HyperServices *hyperServices;
    @synchronized (self.hyperServicesDict) {
        hyperServices = self.hyperServicesDict[key];
        [self.hyperServicesDict removeObjectForKey:key];
        [self.hyperDelegatesDict removeObjectForKey:key];
        [self.keyedComponentsDict removeObjectForKey:key];
    }
    if (hyperServices) {
        [hyperServices terminate];
    }
}

RCT_EXPORT_METHOD(terminate) {
    if (_hyperInstance) {
        [_hyperInstance terminate];
    }
}

RCT_EXPORT_METHOD(notifyAboutRegisterComponent:(NSString *)viewType) {
    [registeredComponents addObject:viewType];
}

RCT_EXPORT_METHOD(notifyAboutRegisterComponentWithKey:(NSString *)viewType componentName:(NSString *)componentName key:(NSString *)key) {
    if (key == nil || viewType == nil) {
        return;
    }
    @synchronized (self.hyperServicesDict) {
        NSMutableDictionary *mapping = self.keyedComponentsDict[key];
        if (mapping == nil) {
            mapping = [NSMutableDictionary new];
            [self.keyedComponentsDict setObject:mapping forKey:key];
        }
        mapping[viewType] = componentName.length > 0 ? componentName : viewType;
    }
}

RCT_EXPORT_METHOD(isInitialised:(RCTPromiseResolveBlock)resolve  reject:(RCTPromiseRejectBlock)reject) {
    if (self.hyperInstance) {
        resolve(self.hyperInstance.isInitialised? @true : @false);
    } else {
        resolve(@false);
    }
}

RCT_EXPORT_METHOD(isInitialisedWithKey:(NSString *)key resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    HyperServices *hyperServices;
    @synchronized (self.hyperServicesDict) {
        hyperServices = key != nil ? self.hyperServicesDict[key] : nil;
    }
    if (hyperServices) {
        resolve(hyperServices.isInitialised? @true : @false);
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

RCT_EXPORT_METHOD(updateMerchantViewHeightWithKey: (NSString * _Nonnull) tag height: (NSNumber * _Nonnull) h key: (NSString * _Nonnull) key) {
    SdkDelegate *delegate;
    @synchronized (self.hyperServicesDict) {
        delegate = self.hyperDelegatesDict[key];
    }
    if (delegate) {
        [delegate setHeight:h forTag:tag];
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

+ (BOOL)isRCTModalHostViewController:(UIViewController *)viewController {
    Class rctModalClass = NSClassFromString(@"RCTModalHostViewController");
    return rctModalClass && [viewController isMemberOfClass:rctModalClass];
}

@end

@implementation HyperFragmentViewManagerIOS


RCT_EXPORT_MODULE(HyperFragmentViewManagerIOS)

// Props tracked per view, so multiple HyperFragmentViews (e.g. one per instance) don't clobber
// each other. Weak view keys let entries die with their views. Main-queue only.
static NSMapTable<UIView *, NSMutableDictionary *> *fragmentViewProps;

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


RCT_CUSTOM_VIEW_PROPERTY(ns, NSString, UIView)
{
    [self setNs:json forView:view];
}

RCT_CUSTOM_VIEW_PROPERTY(payload, NSString, UIView)
{
    [self setPayload:json forView:view];
}

RCT_CUSTOM_VIEW_PROPERTY(hyperKey, NSString, UIView)
{
    [self setHyperKey:[json isKindOfClass:[NSString class]] ? json : nil forView:view];
}


- (void) setHeight:(NSString*)ns forView:(UIView*)view {
    
}

- (void) setWidth:(NSString*)ns forView:(UIView*)view {
    
}
- (NSMutableDictionary *)propsForView:(UIView *)view
{
    if (fragmentViewProps == nil) {
        fragmentViewProps = [NSMapTable weakToStrongObjectsMapTable];
    }
    NSMutableDictionary *props = [fragmentViewProps objectForKey:view];
    if (props == nil) {
        props = [NSMutableDictionary new];
        [fragmentViewProps setObject:props forKey:view];
    }
    return props;
}

- (void)setNs:(NSString *)ns forView:(UIView *)view
{
    [self propsForView:view][@"ns"] = ns;
    [self tryProcessPropsForView:view];
}


- (void)setPayload:(NSString *)payload forView:(UIView *)view
{
    [self propsForView:view][@"payload"] = payload;
    [self tryProcessPropsForView:view];
}

- (void)setHyperKey:(NSString *)hyperKey forView:(UIView *)view
{
    // Stored only: ns/payload drive processing, so setting the key never causes an extra process.
    NSMutableDictionary *props = [self propsForView:view];
    if (hyperKey.length > 0) {
        props[@"hyperKey"] = hyperKey;
    } else {
        [props removeObjectForKey:@"hyperKey"];
    }
}

- (void)tryProcessPropsForView:(UIView *)view
{
    NSMutableDictionary *props = [self propsForView:view];
    if (props[@"ns"] && props[@"payload"]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            NSMutableDictionary *current = [fragmentViewProps objectForKey:view];
            if (current && current[@"ns"] && current[@"payload"]) {
                [self processWithPropsForView:view ns:current[@"ns"] payload:current[@"payload"] key:current[@"hyperKey"]];
            }
        });
    }
}

- (HyperServices *)hyperServicesForKey:(NSString *)key
{
    if (key.length > 0) {
        // A keyed view must never fall back to the legacy singleton: a dead key means no-op.
        HyperSdkReact *module = [self.bridge moduleForClass:[HyperSdkReact class]];
        @synchronized (module.hyperServicesDict) {
            return module.hyperServicesDict[key];
        }
    }
    return _hyperServicesReference;
}

- (void)processWithPropsForView:(UIView *)view ns:(NSString *)ns payload:(NSString *)payload key:(NSString *)key
{
    HyperServices *hyperServicesInstance = [self hyperServicesForKey:key];
    if (payload && payload.length > 0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:payload];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count > 0) {
                if (hyperServicesInstance.baseViewController == nil || hyperServicesInstance.baseViewController.view.window == nil) {
                    id baseViewController = RCTPresentedViewController();
                    if ([HyperSdkReact isRCTModalHostViewController:baseViewController] && [baseViewController presentingViewController]) {
                        [hyperServicesInstance setBaseViewController:[baseViewController presentingViewController]];
                    } else {
                        [hyperServicesInstance setBaseViewController:baseViewController];
                    }
                }
                
                [self manuallyLayoutChildren:view];
                
                NSMutableDictionary *nestedPayload = [jsonData[@"payload"] mutableCopy];
                NSDictionary *fragmentViewGroup = @{ns: view};
                nestedPayload[@"fragmentViewGroups"] = fragmentViewGroup;
                NSMutableDictionary *updatedJsonData = [jsonData mutableCopy];
                updatedJsonData[@"payload"] = nestedPayload;
                [hyperServicesInstance process:[updatedJsonData copy]];
            }
        } @catch (NSException *exception) {
            // Handle exception silently
        }
    }
}

RCT_EXPORT_METHOD(process:(nonnull NSNumber *)viewTag ns:(NSString *)ns payload:(NSString *)payload key:(NSString *)key)
{
    HyperServices *hyperServicesInstance = [self hyperServicesForKey:key];
    if (payload && payload.length>0) {
        @try {
            NSDictionary *jsonData = [HyperSdkReact stringToDictionary:payload];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
                    if (hyperServicesInstance.baseViewController == nil || hyperServicesInstance.baseViewController.view.window == nil) {
                        id baseViewController = RCTPresentedViewController();
                        if ([HyperSdkReact isRCTModalHostViewController:baseViewController] && [baseViewController presentingViewController]) {
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
                    NSDictionary *fragmentViewGroup = @{ns: view};
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
