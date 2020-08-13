import { NativeModules } from 'react-native';

type HyperSdkReactType = {
  multiply(a: number, b: number): Promise<number>;
};

const { HyperSdkReact } = NativeModules;

export default HyperSdkReact as HyperSdkReactType;
