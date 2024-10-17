#ifdef RCT_NEW_ARCH_ENABLED
#import "HyperSDKView.h"

#import <react/renderer/components/RNHyperSdkViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNHyperSDKViewSpec/EventEmitters.h>
#import <react/renderer/components/RNHyperSDKViewSpec/Props.h>
#import <react/renderer/components/RNHyperSdkViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import "Utils.h"
#import <HyperSDK/HyperSDK.h>

using namespace facebook::react;

@interface HyperSDKView () <RCTHyperSDKViewViewProtocol>
    @property HyperServices *hyperInstance;
    @property id <HyperDelegate> delegate;
@end

@implementation HyperSDKView {
    UIView * _view;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<HyperSDKViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const HyperSDKViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] init];
      if (self.hyperInstance == NULL) {
          self.hyperInstance = [HyperServices new];
      }

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &newViewProps = *std::static_pointer_cast<HyperSDKViewProps const>(props);
    NSString *_namespace = [NSString stringWithCString:newViewProps._namespace.c_str()
                                       encoding:[NSString defaultCStringEncoding]];
    NSString *payload = [NSString stringWithCString:newViewProps.payload.c_str()
                                       encoding:[NSString defaultCStringEncoding]];
    if ([self.hyperInstance isInitialised]) {
        [self process:_namespace payload:payload];
    } else {
        [self initiate:_namespace payload:payload];
    }
    [super updateProps:props oldProps:oldProps];
}


- (std::shared_ptr<const HyperSDKViewEventEmitter>)getEventEmitter
    {
    if (!self->_eventEmitter) {
      return nullptr;
    }

        assert(std::dynamic_pointer_cast<HyperSDKViewEventEmitter const>(self->_eventEmitter));
    return std::static_pointer_cast<HyperSDKViewEventEmitter const>(self->_eventEmitter);
    }

- (void)handleCommand:(nonnull const NSString *)commandName args:(nonnull const NSArray *)args { 
    NSLog(@"handleCommand");
    [self process:args[0] payload:args[1]];
}

Class<RCTComponentViewProtocol> HyperSDKViewCls(void)
{
    return HyperSDKView.class;
}

- (void)initiate:(nonnull NSString *)_namespace payload:(nonnull NSString *)payload {
    if (payload && payload.length>0) {
        @try {
            NSDictionary *jsonData = [Utils stringToDictionary:payload];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                
                UIViewController *baseViewController = RCTPresentedViewController();
                [_hyperInstance initiate:baseViewController payload:jsonData callback:^(NSDictionary<NSString *,id> * _Nullable data) {
                    NSString* event  = data[@"event"];
                    if ([event isEqualToString:@"initiate_result"]) {
                        [self process:_namespace payload:payload];
                    }
                    const auto eventEmitter = [self getEventEmitter];
                    if (eventEmitter) {
                      eventEmitter->onHyperEvent(HyperSDKViewEventEmitter::OnHyperEvent{
                          .event = nsStringToStdString(event),
                          .data =  nsStringToStdString([Utils dictionaryToString:data])
                      });
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

- (void)process:(nonnull NSString *)_namespace payload:(nonnull NSString *)payload {
    NSLog(@"handleCommand %@ %@", _namespace, payload);
    HyperServices *hyperServicesInstance = self.hyperInstance;
    if (payload && payload.length>0) {
        @try {
            NSDictionary *jsonData = [Utils stringToDictionary:payload];
            if (jsonData && [jsonData isKindOfClass:[NSDictionary class]] && jsonData.allKeys.count>0) {
                    if (hyperServicesInstance.baseViewController == nil || hyperServicesInstance.baseViewController.view.window == nil) {
                        UIViewController* baseViewController = RCTPresentedViewController();
                        [hyperServicesInstance setBaseViewController:baseViewController];
                    }
                    UIView *view = self;
                    [self manuallyLayoutChildren:view];
                    NSMutableDictionary *nestedPayload = [jsonData[@"payload"] mutableCopy];
                    NSDictionary *fragmentViewGroup = @{_namespace: view};
                    nestedPayload[@"fragmentViewGroups"] = fragmentViewGroup;
                    NSMutableDictionary *updatedJsonData = [jsonData mutableCopy];
                    updatedJsonData[@"payload"] = nestedPayload;
                    [hyperServicesInstance process:[updatedJsonData copy]];
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
#endif
