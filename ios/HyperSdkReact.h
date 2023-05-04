
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNHyperSdkReactSpec.h"
#import "React/RCTEventEmitter.h"

@interface HyperSdkReact : RCTEventEmitter <NativeHyperSdkReactSpec>
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <HyperSDK/HyperSDK.h>

@interface HyperSdkReact : RCTEventEmitter <RCTBridgeModule>
#endif

#import <HyperSDK/HyperSDK.h>
@property HyperServices *hyperInstance;

@end
