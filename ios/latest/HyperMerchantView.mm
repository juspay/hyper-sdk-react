//
//  HyperMerchantView.mm
//  Pods
//
//  Created by Yaswanth Polisetti on 16/10/25.
//
// HyperMerchantView.mm
#import "HyperMerchantView.h"
#import <objc/runtime.h>
#if __has_include("RCTRootViewFactory.h")
#import "RCTRootViewFactory.h"
#define HAS_NEW_ARCH_SUPPORT 1
#else
#define HAS_NEW_ARCH_SUPPORT 0
#endif

@implementation HyperMerchantView
+ (UIView *)createReactNativeViewWithModuleName:(NSString *)moduleName {
    
    #if HAS_NEW_ARCH_SUPPORT
        id appDelegate = RCTSharedApplication().delegate;
        unsigned int ivarCount = 0;
        Ivar *ivars = class_copyIvarList([appDelegate class], &ivarCount);
        id factory = nil;
        for (unsigned int i = 0; i < ivarCount; i++) {
            const char *ivarName = ivar_getName(ivars[i]);
            // Swift mangles property names - look for reactNativeFactory or _reactNativeFactory
            if (strcmp(ivarName, "_reactNativeFactory") == 0 || strcmp(ivarName, "reactNativeFactory") == 0) {
                factory = object_getIvar(appDelegate, ivars[i]);
                break;
            }
        }
        free(ivars);
        if (!factory) {
            return nil;
        }
        // Now use the factory
        if (![factory respondsToSelector:@selector(rootViewFactory)]) {
            return nil;
        }
        id rootViewFactory = [factory performSelector:@selector(rootViewFactory)];
        if (![rootViewFactory respondsToSelector:@selector(viewWithModuleName:initialProperties:)]) {
            return nil;
        }
        UIView *rrv = [rootViewFactory performSelector:@selector(viewWithModuleName:initialProperties:)
                                            withObject:moduleName
                                            withObject:nil];
        
        return rrv;
    #else
        return nil;
    #endif
}
@end
