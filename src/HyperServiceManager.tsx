import { NativeModules } from 'react-native';

const { HyperSdkReactModule } = NativeModules;

if (!HyperSdkReactModule) {
  throw new Error('HyperSdkReactModule is not linked.');
}

class HyperServiceInstance {
  key: string;

  // constructor() {
  //     this.key = HyperSdkReactModule.createNewHyperServices("tenantIdPlaceholder", "clientIdPlaceholder");
  // }
  constructor(tenantId?: string, clientId?: string) {
    if (!HyperSdkReactModule) {
      throw new Error('HyperSdkReactModule is not linked.');
    }

    if (tenantId || clientId) {
      if (tenantId == undefined) tenantId = '';
      if (clientId == undefined) clientId = '';
      this.key = HyperSdkReactModule.createNewHyperServices(tenantId, clientId);
    } else {
      this.key = HyperSdkReactModule.createNewHyperServices('', '');
    }
  }

  initiate(data: string) {
    return HyperSdkReactModule.initiate(this.key, data);
  }

  process(data: string) {
    return HyperSdkReactModule.process(this.key, data);
  }

  terminate() {
    return HyperSdkReactModule.terminate(this.key);
  }

  prefetch(data: string) {
    return HyperSdkReactModule.preFetch(this.key, data);
  }

  processWithActivity(data: string) {
    return HyperSdkReactModule.processWithActivity(this.key, data);
  }

  isInitialised(): Promise<boolean> {
    return HyperSdkReactModule.isInitialised(this.key);
  }

  isNull(): boolean {
    return HyperSdkReactModule.isNull(this.key);
  }

  onBackPressed(): boolean {
    return HyperSdkReactModule.onBackPressed(this.key);
  }

  getHyperEventString(): string {
    return this.key;
  }
}

export default HyperServiceInstance;
