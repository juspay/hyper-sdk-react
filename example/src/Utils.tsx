import { Alert } from 'react-native';
import HyperAPIUtils from './API';

const uuidv4 = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const service: any = {};
service.ec = 'in.juspay.ec';

const generatePreFetchPayload = (clientId: string) => {
  return {
    service: service.ec,
    betaAssets: false,
    payload: {
      clientId,
    },
  };
};

const generateInitiatePayload = (
  merchantId: string,
  clientId: string,
  customerId: string
) => {
  return {
    requestId: uuidv4(),
    service: service.ec,
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

const generateNBTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  bank: string
) => {
  return {
    requestId: uuidv4(),
    service: service.ec,
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
    service: service.ec,
    payload,
  };
};

const generateWalletTxnPayload = (
  orderId: string,
  clientAuthToken: string,
  walletName: string,
  directToken: string
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

  return {
    requestId: uuidv4(),
    service: service.ec,
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
    service: service.ec,
    payload,
  };
};

const generatePaymentMethodsPayload = () => {
  return {
    requestId: uuidv4(),
    service: service.ec,
    payload: {
      action: 'getPaymentMethods',
    },
  };
};

const generateGetUPIAppsPayload = (orderId: string) => {
  return {
    requestId: uuidv4(),
    service: service.ec,
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
    service: service.ec,
    payload: {
      action: 'refreshWalletBalances',
      clientAuthToken,
    },
  };
};

const generateListCardsPayload = (clientAuthToken: string) => {
  return {
    requestId: uuidv4(),
    service: service.ec,
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
    service: service.ec,
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
    service: service.ec,
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
    service: service.ec,
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
    service: service.ec,
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
    service: service.ec,
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
  showCopyAlert(screen + ': ' + event, payload);
  console.warn(screen, resp);
};

type HyperUtils = {
  uuidv4(): string;
  generateInitiatePayload(
    merchantId: string,
    clientId: string,
    customerId: string
  ): {};
  generatePreFetchPayload(clientId: string): {};
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
    directToken: string
  ): {};
  generateUPIIntentTxnPayload(
    orderId: string,
    clientAuthToken: string,
    app: string,
    vpa: string,
    upiSdkPresent: boolean
  ): {};
  generatePaymentMethodsPayload(): {};
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
  generatePreFetchPayload,
  generateInitiatePayload,
  generateOrderId,
  getClientAuthToken,
  generateNBTxnPayload,
  generateCardTxnPayload,
  generateWalletTxnPayload,
  generateUPIIntentTxnPayload,
  generatePaymentMethodsPayload,
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
