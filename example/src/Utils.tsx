const uuidv4 = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const generatePreFetchPayload = () => {
  return {
    service: 'in.juspay.ec',
    betaAssets: false,
    payload: {
      clientId: 'picasso_android',
    },
  };
};

const generateInitiatePayload = () => {
  return {
    requestId: uuidv4(),
    service: 'in.juspay.ec',
    betaAssets: false,
    payload: {
      action: 'initiate',
      merchantId: 'picasso',
      clientId: 'picasso_android',
      customerId: '9634393464',
      environment: 'sandbox',
    },
  };
};

type HyperUtils = {
  uuidv4(): string;
  generateInitiatePayload(): {};
  generatePreFetchPayload(): {};
};

export default {
  uuidv4,
  generatePreFetchPayload,
  generateInitiatePayload,
} as HyperUtils;
