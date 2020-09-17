import { NativeModules } from 'react-native';

type HyperSdkReactType = {
  multiply(a: number, b: number): Promise<number>;
  preFetch(data: string): void;
  createHyperServices(): void;
  initiate(data: string): void;
  process(data: string): void;
  terminate(): void;
  onBackPressed(): boolean;
  isNull(): boolean;
  isInitialised(): Promise<boolean>;
};

const { HyperSdkReact } = NativeModules;

export default HyperSdkReact as HyperSdkReactType;
