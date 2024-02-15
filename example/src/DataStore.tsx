let x: {
  merchant_id: String;
  customer_id: String;
  order_id: String;
  amount: String;
  mobile_number: String;
  customer_email: String;
  timestamp: String;
} = {
  merchant_id: '',
  customer_id: '',
  order_id: '',
  amount: '1',
  mobile_number: '',
  customer_email: '',
  timestamp: '',
};

export function setOrderDetails(value: {
  merchant_id: String;
  customer_id: String;
  order_id: String;
  amount: String;
  mobile_number: String;
  customer_email: String;
  timestamp: String;
}) {
  x = value;
}

export function getOrderDetails() {
  return x;
}
