/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */
import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'hyper-sdk-react' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const HyperSdkReactModule = NativeModules.HyperSdkReact
  ? NativeModules.HyperSdkReact
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

let instanceCounter = 0;

/**
 * Generates the key that identifies an instance natively and names its event
 * channel. Only in-process uniqueness is needed, so a counter plus entropy is
 * sufficient and avoids an external UUID dependency.
 */
const generateInstanceKey = (): string =>
  `hyper-instance-${++instanceCounter}-${Date.now().toString(
    36
  )}-${Math.random().toString(36).slice(2)}`;

export default class HyperServiceInstance {
  key: string;

  constructor(tenantId?: string, clientId?: string) {
    this.key = generateInstanceKey();
    HyperSdkReactModule.createHyperServicesWithKey(
      this.key,
      tenantId ?? '',
      clientId ?? ''
    );
  }

  initiate(data: string) {
    return HyperSdkReactModule.initiateWithKey(data, this.key);
  }

  process(data: string) {
    return HyperSdkReactModule.processWithKey(data, this.key);
  }

  terminate() {
    return HyperSdkReactModule.terminateWithKey(this.key);
  }

  processWithActivity(data: string) {
    // Activities are Android-only; on iOS this is the same as process.
    if (Platform.OS === 'ios') {
      return HyperSdkReactModule.processWithKey(data, this.key);
    }
    return HyperSdkReactModule.processWithActivityWithKey(data, this.key);
  }

  openPaymentPage(data: string) {
    return HyperSdkReactModule.openPaymentPageWithKey(data, this.key);
  }

  isInitialised(): Promise<boolean> {
    return HyperSdkReactModule.isInitialisedWithKey(this.key);
  }

  isNull(): boolean {
    return HyperSdkReactModule.isNullWithKey(this.key);
  }

  onBackPressed(): boolean {
    // Hardware back-press only exists on Android.
    if (Platform.OS !== 'android') {
      return false;
    }
    return HyperSdkReactModule.onBackPressedWithKey(this.key);
  }

  /**
   * Registers a merchant view component for this instance only. Register the component itself
   * with AppRegistry under `componentName` (defaults to the view tag, which then behaves like the
   * process-wide registration).
   */
  notifyAboutRegisterComponent(viewType: string, componentName?: string) {
    return HyperSdkReactModule.notifyAboutRegisterComponentWithKey(
      viewType,
      componentName ?? viewType,
      this.key
    );
  }

  updateMerchantViewHeight(tag: string, height: number) {
    // Merchant view height is only needed on iOS; Android measures the view itself.
    if (Platform.OS === 'ios') {
      return HyperSdkReactModule.updateMerchantViewHeightWithKey(
        tag,
        height,
        this.key
      );
    }
    console.log('UpdateMerchantViewHeight not available for android');
  }

  getHyperEventString(): string {
    return this.key;
  }
}
