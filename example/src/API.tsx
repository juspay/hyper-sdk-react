import { NativeModules } from 'react-native';

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
};

const { HyperAPIUtils } = NativeModules;

export default HyperAPIUtils as HyperAPIUtilsType;
