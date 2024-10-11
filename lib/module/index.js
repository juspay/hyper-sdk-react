/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR = `The package 'hyper-sdk-react' doesn't seem to be linked. Make sure: \n\n` + Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const HyperSdkReact = NativeModules.HyperSdkReact ? NativeModules.HyperSdkReact : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
export { default as HyperFragmentView } from './HyperFragmentView';
if (Platform.OS === 'android') {
  HyperSdkReact.updateBaseViewController = () => {};
  HyperSdkReact.updateMerchantViewHeight = (_tag, _height) => {
    console.log('UpdateMerchantViewHeight not available for android');
  };
}
if (Platform.OS === 'ios') {
  HyperSdkReact.processWithActivity = data => {
    HyperSdkReact.process(data);
  };
}
export default HyperSdkReact;
//# sourceMappingURL=index.js.map