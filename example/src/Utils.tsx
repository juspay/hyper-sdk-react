/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import { Alert } from 'react-native';
import HyperAPIUtils from './API';

const uuidv4 = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    // eslint-disable-next-line no-bitwise
    var r = (Math.random() * 16) | 0,
      // eslint-disable-next-line no-bitwise
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const getTimestamp = () => {
  return new Date().getTime().toString();
};

const generateOrderId = () => {
  var result = 'hyperOrder-' + Math.floor(Math.random() * 10000) + '-';
  var characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
  var charactersLength = characters.length;
  for (var i = 0; i < 4; i++) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }
  return result;
};

const getClientAuthToken = (resp: string) => {
  try {
    var payload = JSON.parse(resp);
    if (payload.hasOwnProperty('juspay')) {
      var auth = payload.juspay;
      return auth.client_auth_token || '';
    }
  } catch (err) {
    console.error(err);
  }
  return '';
};

const services = {
  ec: 'in.juspay.hyperapi',
  pp: 'in.juspay.hyperpay',
};

const generatePreFetchPayload = (clientId: string, service: string) => {
  return {
    service: service === 'ec' ? services.ec : services.pp,
    betaAssets: false,
    payload: {
      clientId,
    },
  };
};

const generateECInitiatePayload = (
  merchantId: string,
  clientId: string,
  customerId: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    betaAssets: false,
    payload: {
      action: 'initiate',
      merchantId,
      clientId,
      customerId,
      environment: 'sandbox',
    },
  };
};

const generatePPInitiatePayload = (
  clientId: string,
  merchantId: string,
  signaturePayload: string,
  signature: string,
  merchantKeyId: string
) => {
  return {
    requestId: uuidv4(),
    service: services.pp,
    betaAssets: false,
    payload: {
      action: 'initiate',
      clientId,
      merchantId,
      signaturePayload,
      signature,
      merchantKeyId,
      environment: 'sandbox',
    },
  };
};

const generateProcessPayloadPP = (
  action: string,
  clientId: string,
  merchantId: string,
  orderDetails: string,
  signature: string,
  merchantKeyId: string
) => {
  return {
    requestId: uuidv4(),
    service: services.pp,
    payload: {
      action,
      clientId,
      merchantId,
      orderDetails,
      signature,
      merchantKeyId,
    },
  };
};

const generateNBTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  bank: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'nbTxn',
      orderId,
      clientAuthToken,
      paymentMethodType: 'NB',
      paymentMethod: bank,
      endUrls: [
        '.*sandbox.juspay.in\\/thankyou.*',
        '.*sandbox.juspay.in\\/end.*',
        '.*api.juspay.in\\/end.*',
      ],
    },
  };
};

const generateCardTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  cardNetwork: string, // VISA etc
  cardToken: string,
  cardNumber: string,
  expMonth: string,
  expYear: string,
  cardSecurityCode: string,
  authType: string,
  saveToLocker: boolean
) => {
  var payload: any = {
    action: 'cardTxn',
    orderId,
    clientAuthToken,
    cardSecurityCode,
    saveToLocker,
    endUrls: [
      '.*sandbox.juspay.in\\/thankyou.*',
      '.*sandbox.juspay.in\\/end.*',
      '.*api.juspay.in\\/end.*',
    ],
  };

  if (cardNetwork !== '') {
    payload.paymentMethod = cardNetwork;
  }
  if (cardToken !== '') {
    payload.cardToken = cardToken;
  }
  if (cardNumber !== '') {
    payload.cardNumber = cardNumber;
  }
  if (expMonth !== '') {
    payload.cardExpMonth = expMonth;
  }
  if (expYear !== '') {
    payload.cardExpYear = expYear;
  }
  if (authType !== '') {
    payload.authType = authType;
  }

  return {
    requestId: uuidv4(),
    service: services.ec,
    payload,
  };
};

const generateWalletTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  walletName: string,
  directToken: string,
  sdkPresent: String
) => {
  var payload: any = {
    action: 'walletTxn',
    orderId,
    clientAuthToken,
    paymentMethod: walletName,
    endUrls: [
      '.*sandbox.juspay.in\\/thankyou.*',
      '.*sandbox.juspay.in\\/end.*',
      '.*api.juspay.in\\/end.*',
    ],
  };

  if (directToken !== '') {
    payload.directWalletToken = directToken;
  }

  if (sdkPresent !== '') {
    payload.sdkPresent = sdkPresent;
  }

  return {
    requestId: uuidv4(),
    service: services.ec,
    payload,
  };
};

const generateUPIIntentTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  app: string,
  vpa: string,
  upiSdkPresent: boolean
) => {
  var payload: any = {
    action: 'upiTxn',
    orderId,
    clientAuthToken,
    paymentMethod: 'UPI',
    upiSdkPresent,
    showLoader: false,
    endUrls: [
      '.*sandbox.juspay.in\\/thankyou.*',
      '.*sandbox.juspay.in\\/end.*',
      '.*api.juspay.in\\/end.*',
    ],
  };

  if (app !== '') {
    payload.payWithApp = app;
  }
  if (vpa !== '') {
    payload.custVpa = vpa;
  }

  return {
    requestId: uuidv4(),
    service: services.ec,
    payload,
  };
};

const generatePaymentMethodsPayload = () => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'getPaymentMethods',
    },
  };
};

const generateJuspaySafePayload = (orderId: string) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'startJuspaySafe',
      url: 'https://www.airtel.in',
      orderId: orderId,
    },
  };
};

const generateGetUPIAppsPayload = (orderId: string) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'upiTxn',
      orderId,
      getAvailableApps: true,
    },
  };
};

const generateListWalletsPayload = (clientAuthToken: string) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'refreshWalletBalances',
      clientAuthToken,
    },
  };
};

const generateListCardsPayload = (clientAuthToken: string) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'cardList',
      clientAuthToken,
    },
  };
};

const generateCreateWalletPayload = (
  walletName: string,
  clientAuthToken: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'createWallet',
      walletName,
      clientAuthToken,
    },
  };
};

const generateLinkWalletPayload = (
  walletName: string,
  walletId: string,
  otp: string,
  clientAuthToken: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'linkWallet',
      walletName,
      walletId,
      otp,
      clientAuthToken,
    },
  };
};

const generateDeLinkWalletPayload = (
  walletName: string,
  walletId: string,
  clientAuthToken: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'delinkWallet',
      walletName,
      walletId,
      clientAuthToken,
    },
  };
};

const generateDeleteCardPayload = (
  cardToken: string,
  clientAuthToken: string
) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'deleteCard',
      cardToken,
      clientAuthToken,
    },
  };
};

const generateDeviceReadyPayload = (sdkPresent: string) => {
  return {
    requestId: uuidv4(),
    service: services.ec,
    payload: {
      action: 'isDeviceReady',
      sdkPresent,
    },
  };
};

const showCopyAlert = (title: string, body: any) => {
  Alert.alert(title, body, [
    {
      text: 'OK',
      onPress: () => null,
    },
    {
      text: 'COPY',
      onPress: () => {
        HyperAPIUtils.copyToClipBoard(title, body);
      },
    },
  ]);
};

const alertCallbackResponse = (screen: string, resp: any) => {
  var data = JSON.parse(resp);
  var event: string = data.event || '';
  var payload = JSON.stringify(data.payload) || '';
  console.log(screen + ': callback response' + payload);
  showCopyAlert(screen + ': ' + event, payload);
  // console.warn(screen, resp);
};

type HyperUtils = {
  uuidv4(): string;
  getTimestamp(): string;
  generateECInitiatePayload(
    merchantId: string,
    clientId: string,
    customerId: string
  ): {};
  generatePPInitiatePayload(
    clientId: string,
    merchantId: string,
    signaturePayload: string,
    signature: string,
    merchantKeyId: string
  ): {};
  generateProcessPayloadPP(
    action: string,
    clientId: string,
    merchantId: string,
    orderDetails: string,
    signature: string,
    merchantKeyId: string
  ): {};
  generatePreFetchPayload(clientId: string, service: string): {};
  generateOrderId(): string;
  getClientAuthToken(resp: string): string;
  generateNBTxnPayload(
    orderId: string,
    clientAuthToken: string,
    bank: string
  ): {};
  generateCardTxnPayload(
    orderId: string,
    clientAuthToken: string,
    cardNetwork: string, // VISA etc
    cardToken: string,
    cardNumber: string,
    expMonth: string,
    expYear: string,
    cardSecurityCode: string,
    authType: string,
    saveToLocker: boolean
  ): {};
  generateWalletTxnPayload(
    orderId: string,
    clientAuthToken: string,
    walletName: string,
    directToken: string,
    sdkPresent: String
  ): {};
  generateUPIIntentTxnPayload(
    orderId: string,
    clientAuthToken: string,
    app: string,
    vpa: string,
    upiSdkPresent: boolean
  ): {};
  generatePaymentMethodsPayload(): {};
  generateJuspaySafePayload(orderId: string): {};
  generateGetUPIAppsPayload(orderId: string): {};
  generateListWalletsPayload(clientAuthToken: string): {};
  generateListCardsPayload(clientAuthToken: string): {};
  generateCreateWalletPayload(walletName: string, clientAuthToken: string): {};
  generateLinkWalletPayload(
    walletName: string,
    walletId: string,
    otp: string,
    clientAuthToken: string
  ): {};
  generateDeLinkWalletPayload(
    walletName: string,
    walletId: string,
    clientAuthToken: string
  ): {};
  generateDeleteCardPayload(cardToken: string, clientAuthToken: string): {};
  generateDeviceReadyPayload(sdkPresent: string): {};
  showCopyAlert(title: string, body: any): void;
  alertCallbackResponse(screen: string, resp: any): void;
};

export default {
  uuidv4,
  getTimestamp,
  generatePreFetchPayload,
  generateECInitiatePayload,
  generatePPInitiatePayload,
  generateProcessPayloadPP,
  generateOrderId,
  getClientAuthToken,
  generateNBTxnPayload,
  generateCardTxnPayload,
  generateWalletTxnPayload,
  generateUPIIntentTxnPayload,
  generatePaymentMethodsPayload,
  generateJuspaySafePayload,
  generateGetUPIAppsPayload,
  generateListWalletsPayload,
  generateListCardsPayload,
  generateCreateWalletPayload,
  generateLinkWalletPayload,
  generateDeLinkWalletPayload,
  generateDeleteCardPayload,
  generateDeviceReadyPayload,
  showCopyAlert,
  alertCallbackResponse,
} as HyperUtils;
