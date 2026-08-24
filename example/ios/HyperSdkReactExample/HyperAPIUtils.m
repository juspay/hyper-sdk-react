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

/// Removes PEM header/footer lines and whitespace so keys pasted straight from a
/// .pem file decode cleanly.
static NSString *stripPemArmor(NSString *key) {
  NSString *stripped = [key stringByReplacingOccurrencesOfString:@"-----[^-]+-----"
                                                      withString:@""
                                                         options:NSRegularExpressionSearch
                                                           range:NSMakeRange(0, key.length)];
  return [[stripped componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]]
          componentsJoinedByString:@""];
}

/// SecKey expects PKCS#1 (RSAPrivateKey) DER. Merchant keys are usually shipped as
/// PKCS#8 ("BEGIN PRIVATE KEY"); this strips the fixed PKCS#8 RSA envelope when present.
static NSData *pkcs1FromKeyData(NSData *keyData) {
  static const uint8_t rsaAlgId[] = {0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01};
  if (keyData.length < 32) {
    return keyData;
  }
  NSData *needle = [NSData dataWithBytes:rsaAlgId length:sizeof(rsaAlgId)];
  NSRange algRange = [keyData rangeOfData:needle options:0 range:NSMakeRange(0, MIN(keyData.length, (NSUInteger)32))];
  if (algRange.location == NSNotFound) {
    return keyData; // already PKCS#1
  }
  // Skip: NULL param (2 bytes) then the OCTET STRING tag + DER length that wraps the PKCS#1 blob
  const uint8_t *bytes = keyData.bytes;
  NSUInteger i = algRange.location + algRange.length + 2;
  if (i >= keyData.length || bytes[i] != 0x04) {
    return keyData;
  }
  i++;
  NSUInteger len = bytes[i];
  if (len == 0x81) {
    i += 2;
  } else if (len == 0x82) {
    i += 3;
  } else {
    i += 1;
  }
  if (i >= keyData.length) {
    return keyData;
  }
  return [keyData subdataWithRange:NSMakeRange(i, keyData.length - i)];
}

RCT_EXPORT_METHOD(generateSign:(NSString *)privateKey payloadString:(NSString *)payloadString
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  if (privateKey == nil || [privateKey stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]].length == 0) {
    reject(@"generateSign", @"sign-failed: privateKey is empty. Provide it via Set Params or merchant_config.json", nil);
    return;
  }
  NSData *encodedData = [[NSData alloc] initWithBase64EncodedString:stripPemArmor(privateKey) options:NSDataBase64DecodingIgnoreUnknownCharacters];
  if (encodedData == nil) {
    reject(@"generateSign", @"sign-failed: privateKey is not valid base64", nil);
    return;
  }
  NSData *pkcs1 = pkcs1FromKeyData(encodedData);
  CFErrorRef keyError = NULL;
  SecKeyRef privateKeyRef = SecKeyCreateWithData((__bridge CFDataRef)pkcs1, (__bridge CFDictionaryRef)@{(__bridge id)kSecAttrKeyType: (__bridge id)kSecAttrKeyTypeRSA, (__bridge id)kSecAttrKeyClass: (__bridge id)kSecAttrKeyClassPrivate}, &keyError);
  if (privateKeyRef == NULL) {
    if (keyError != NULL) { CFRelease(keyError); }
    reject(@"generateSign", @"sign-failed: could not parse the RSA private key (PKCS#8 and PKCS#1 supported)", nil);
    return;
  }
  NSData *data = [payloadString dataUsingEncoding:NSUTF8StringEncoding];
  CFErrorRef error = NULL;
  NSData *sign = (NSData *)CFBridgingRelease(SecKeyCreateSignature(privateKeyRef, kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256, (__bridge CFDataRef)data, &error));
  CFRelease(privateKeyRef);
  if (!sign) {
    if (error != NULL) { CFRelease(error); }
    reject(@"generateSign", @"sign-failed: signing operation failed", nil);
    return;
  }
  NSString* signature = [sign base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed];
  resolve(signature);
}

@end
