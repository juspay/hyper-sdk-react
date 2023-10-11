#import <React/RCTViewManager.h>
#import <React/RCTConvert.h>
#import <React/RCTUIManager.h>
#import <React/RCTView.h>

@interface MyViewManagerManager : RCTViewManager

@end

@implementation MyViewManagerManager

RCT_EXPORT_MODULE()

- (UIView *)view
{
  UIView *myView = [[UIView alloc] init];
  UILabel *label = [[UILabel alloc] init];
  label.text = @"Welcome to IOS UIView with React Native.";
  label.textAlignment = NSTextAlignmentLeft;
  label.backgroundColor = UIColor.blueColor;
  label.textColor = UIColor.whiteColor;
  [label sizeToFit];
  [myView addSubview:label];
    
  return myView;
}

RCT_CUSTOM_VIEW_PROPERTY(width, float, UIView)
{
  view.frame = CGRectMake(view.frame.origin.x, view.frame.origin.y, [RCTConvert CGFloat:json], view.frame.size.height);
}

RCT_CUSTOM_VIEW_PROPERTY(height, float, UIView)
{
  view.frame = CGRectMake(view.frame.origin.x, view.frame.origin.y, view.frame.size.width, [RCTConvert CGFloat:json]);
}

@end
