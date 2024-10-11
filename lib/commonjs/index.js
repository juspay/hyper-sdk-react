"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
Object.defineProperty(exports, "HyperFragmentView", {
  enumerable: true,
  get: function () {
    return _HyperFragmentView.default;
  }
});
exports.default = void 0;
var _reactNative = require("react-native");
var _HyperFragmentView = _interopRequireDefault(require("./HyperFragmentView"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

const LINKING_ERROR = `The package 'hyper-sdk-react' doesn't seem to be linked. Make sure: \n\n` + _reactNative.Platform.select({
  ios: "- You have run 'pod install'\n",
  default: ''
}) + '- You rebuilt the app after installing the package\n' + '- You are not using Expo Go\n';
const HyperSdkReact = _reactNative.NativeModules.HyperSdkReact ? _reactNative.NativeModules.HyperSdkReact : new Proxy({}, {
  get() {
    throw new Error(LINKING_ERROR);
  }
});
if (_reactNative.Platform.OS === 'android') {
  HyperSdkReact.updateBaseViewController = () => {};
  HyperSdkReact.updateMerchantViewHeight = (_tag, _height) => {
    console.log('UpdateMerchantViewHeight not available for android');
  };
}
if (_reactNative.Platform.OS === 'ios') {
  HyperSdkReact.processWithActivity = data => {
    HyperSdkReact.process(data);
  };
}
var _default = exports.default = HyperSdkReact;
//# sourceMappingURL=index.js.map