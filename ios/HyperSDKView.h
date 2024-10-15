// This guard prevent this file to be compiled in the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

#ifndef HyperSdkViewNativeComponent_h
#define HyperSdkViewNativeComponent_h

NS_ASSUME_NONNULL_BEGIN

@interface HyperSDKView : RCTViewComponentView
@end

NS_ASSUME_NONNULL_END

#endif /* HyperSdkViewNativeComponent_h */
#endif /* RCT_NEW_ARCH_ENABLED */
