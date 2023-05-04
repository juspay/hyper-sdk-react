import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'hyper-sdk-react' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const HyperSdkReact = NativeModules.HyperSdkReact
  ? NativeModules.HyperSdkReact
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

console.log('HyperSDKReactVersion', HyperSdkReact);

if (Platform.OS === 'android') {
  HyperSdkReact.updateBaseViewController = () => {};
}

type HyperSdkReactType = {
  HyperEvent: string;
  preFetch(data: string): void;
  createHyperServices(): void;
  initiate(data: string): void;
  process(data: string): void;
  terminate(): void;
  onBackPressed(): boolean;
  isNull(): boolean;
  isInitialised(): Promise<boolean>;
  updateBaseViewController(): void;
};

export default HyperSdkReact as HyperSdkReactType;
