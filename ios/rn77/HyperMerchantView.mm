//
//  HyperMerchantView.mm
//  Pods
//
//  Created by Yaswanth Polisetti on 16/10/25.
//
// HyperMerchantView.mm
#import "HyperMerchantView.h"
#import <objc/runtime.h>

#if __has_include("RCTAppDelegate.h") && __has_include("RCTRootViewFactory.h")
#import "RCTAppDelegate.h"
#import "RCTRootViewFactory.h"
#define HAS_NEW_ARCH_SUPPORT 1
#else
#define HAS_NEW_ARCH_SUPPORT 0
#endif

@implementation HyperMerchantView
+ (UIView *)createReactNativeViewWithModuleName:(NSString *)moduleName {
    
    #if HAS_NEW_ARCH_SUPPORT
    
        bool rootFactoryAvailable = false;
        id appDelegate = RCTSharedApplication().delegate;
        rootFactoryAvailable = [appDelegate respondsToSelector:@selector(rootViewFactory)];
        if (!rootFactoryAvailable) {
            return nil;
        }
        RCTRootViewFactory *factory = ((RCTAppDelegate *)appDelegate).rootViewFactory;
        UIView *rrv = [factory viewWithModuleName:moduleName initialProperties:nil];
        return rrv;
    #else
        return nil;
    #endif
}
@end
