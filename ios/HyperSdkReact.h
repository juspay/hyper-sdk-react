/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */


#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <HyperSDK/HyperSDK.h>

@interface HyperSdkReact : RCTEventEmitter <RCTBridgeModule>

#import <HyperSDK/HyperSDK.h>
@property HyperServices *hyperInstance;

@end
