import uuid from 'react-native-uuid';

import { NativeModules, Platform } from 'react-native';

const HyperSdkReactModule = NativeModules.HyperSdkReact;

if (!HyperSdkReactModule) {
  throw new Error('HyperSdkReactModule is not linked.');
}

export default class HyperServiceInstance {
  key: string;

  // constructor() {
  //     this.key = HyperSdkReactModule.createNewHyperServices("tenantIdPlaceholder", "clientIdPlaceholder");
  // }
  constructor(tenantId?: string, clientId?: string) {
    if (!HyperSdkReactModule) {
      throw new Error('HyperSdkReactModule is not linked.');
    }
    this.key = uuid.v4();
    if (tenantId || clientId) {
      if (tenantId == undefined) tenantId = '';
      if (clientId == undefined) clientId = '';
      HyperSdkReactModule.createHyperServicesWithKey(
        this.key,
        tenantId,
        clientId
      );
    } else {
      HyperSdkReactModule.createHyperServicesWithKey(this.key, '', '');
    }
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
    if (Platform.OS === 'ios') {
      HyperSdkReactModule.processWithKey(data, this.key);
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
    return HyperSdkReactModule.onBackPressedWithKey(this.key);
  }

  getHyperEventString(): string {
    return this.key;
  }
}
