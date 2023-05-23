/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "HyperAPIUtils.h"

@implementation HyperAPIUtils
RCT_EXPORT_MODULE()


- (NSArray<NSString *> *)supportedEvents {
  return @[@"HyperAPIUtils"];
}

RCT_EXPORT_METHOD(createCustomer:(NSString *)customerId mobile:(NSString *)mobile email:(NSString *)email apiKey:(NSString *)apiKey resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  @try {
    NSString *url = [NSString stringWithFormat:@"%@%@",@"https://sandbox.juspay.in",@"/customers"];

    NSString *authStr = [NSString stringWithFormat:@"%@:%@", apiKey, nil];
    NSData *authData = [authStr dataUsingEncoding:NSUTF8StringEncoding];
    NSString *authValue = [NSString stringWithFormat:@"Basic %@", [authData base64EncodedStringWithOptions:0]];

    NSString *customerPostData =[NSString stringWithFormat:@"object_reference_id=%@&mobile_number=%@&email_address=%@", customerId, mobile, email];

    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
                                                           cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
    [request setHTTPMethod:@"POST"];
    [request setValue:authValue forHTTPHeaderField:@"Authorization"];

    NSData *dataBody = [customerPostData dataUsingEncoding:NSUTF8StringEncoding];
    [request setHTTPBody:dataBody];

    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];

    [[session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
      if(data && !error) {
        NSError *dataError;
        NSDictionary *createCustomerResponse = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:&dataError];
        if(!dataError && [createCustomerResponse valueForKey:@"status"]) {
          resolve([[self class] dictionaryToString:createCustomerResponse]);
        } else {
          reject(@"no_events", @"There were no events", nil);
        }
      }
      if (error) {
        reject(@"no_events", @"There were no events", nil);
      }
    }] resume];
  } @catch (NSException *exception) {
    //TODO: Return an error for invalid data.
    reject(@"no_events", @"There were no events", nil);
  } @finally {}
}

RCT_EXPORT_METHOD(generateOrder:(NSString *)orderId :(NSString *)orderAmount :(NSString *)customerId mobile:(NSString *)mobile email:(NSString *)email apiKey:(NSString *)apiKey promise:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  @try {
    NSString *url = [NSString stringWithFormat:@"%@%@",@"https://sandbox.juspay.in",@"/order/create"];

    NSString *authStr = [NSString stringWithFormat:@"%@:%@", apiKey, nil];
    NSData *authData = [authStr dataUsingEncoding:NSUTF8StringEncoding];
    NSString *authValue = [NSString stringWithFormat:@"Basic %@", [authData base64EncodedStringWithOptions:0]];

    NSString *customerPostData =[NSString stringWithFormat:@"customer_id=%@&mobile_number=%@&email_address=%@&amount=%@&order_id=%@&options.get_client_auth_token=true", customerId, mobile, email, orderAmount, orderId];

    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
                                                           cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
    [request setHTTPMethod:@"POST"];
    [request setValue:@"2018-07-01" forHTTPHeaderField:@"version"];
    [request setValue:authValue forHTTPHeaderField:@"Authorization"];

    NSData *dataBody = [customerPostData dataUsingEncoding:NSUTF8StringEncoding];
    [request setHTTPBody:dataBody];

    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];


    [[session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
      if(data && !error) {
        NSError *dataError;
        NSDictionary *orderResponse = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:&dataError];
        if(!dataError && [orderResponse valueForKey:@"status"]) {
          resolve([[self class] dictionaryToString:orderResponse]);
        } else {
          reject(@"no_events", @"There were no events", nil);
        }
      }
      if(error) {
        reject(@"no_events", @"There were no events", nil);
      }
    }] resume];
  } @catch (NSException *exception) {
    reject(@"no_events", @"There were no events", nil);
  } @finally {}
}

RCT_EXPORT_METHOD(copyToClipBoard:(NSString *)header message:(NSString *)message) {
  UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
  pasteboard.string = [NSString stringWithFormat:@"Header:%@\nMessage:%@",header,message];
}

+ (NSString*)dictionaryToString:(id)dict{
  if (!dict || ![NSJSONSerialization isValidJSONObject:dict]) {
    return @"";
  }
  NSString *data = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:dict options:0 error:nil] encoding:NSUTF8StringEncoding];
  return data;
}

RCT_EXPORT_METHOD(generateSign:(NSString *)privateKey payloadString:(NSString *)payloadString
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  NSData *encodedData = [[NSData alloc] initWithBase64EncodedString:privateKey options:NSDataBase64DecodingIgnoreUnknownCharacters];
  SecKeyRef privateKeyRef = SecKeyCreateWithData((__bridge CFDataRef)encodedData, (__bridge CFDictionaryRef)@{(__bridge id)kSecAttrKeyType: (__bridge id)kSecAttrKeyTypeRSA, (__bridge id)kSecAttrKeyClass: (__bridge id)kSecAttrKeyClassPrivate}, nil);
  NSData *data = [payloadString dataUsingEncoding:NSUTF8StringEncoding];
  CFErrorRef error = NULL;
  NSData *sign = (NSData *)CFBridgingRelease(SecKeyCreateSignature(privateKeyRef, kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256, (__bridge CFDataRef)data, &error));
  if (!sign) {
    reject(@"no_events", @"sign-failed", nil);
  }
  NSString* signature = [sign base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed];
  resolve(signature);
}

@end
