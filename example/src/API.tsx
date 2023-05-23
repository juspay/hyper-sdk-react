/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'HyperAPIUtils' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const HyperAPIUtils = NativeModules.HyperAPIUtils
  ? NativeModules.HyperAPIUtils
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

console.log('NativeModules', NativeModules);

type HyperAPIUtilsType = {
  createCustomer(
    customerId: string,
    mobile: string,
    email: string,
    apiKey: string
  ): Promise<string>;
  generateOrder(
    orderId: string,
    orderAmount: string,
    customerId: string,
    mobile: string,
    email: string,
    apiKey: string
  ): Promise<string>;
  copyToClipBoard(header: string, message: string): void;
  generateSign(keyString: string, payload: string): Promise<string>;
};

export default HyperAPIUtils as HyperAPIUtilsType;
