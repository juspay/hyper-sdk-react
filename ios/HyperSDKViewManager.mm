#import <React/RCTViewManager.h>
#import <React/RCTUIManager.h>
#import "RCTBridge.h"
#import "Utils.h"

@interface HyperSDKViewManager : RCTViewManager
@end

@implementation HyperSDKViewManager

RCT_EXPORT_MODULE(HyperSDKView)

- (UIView *)view
{
  return [[UIView alloc] init];
}

@end
