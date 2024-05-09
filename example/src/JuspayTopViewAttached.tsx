import React from 'react';
import { Button, StyleSheet, Text, TextInput, View } from 'react-native';
import { getOrderDetails } from './DataStore';
import HyperAPIUtils from './API';
import HyperSdkReact from 'hyper-sdk-react';

function triggerUpdateOrder(text: String) {
  let ord = getOrderDetails();
  ord.amount = text;
  let ordS = JSON.stringify(ord);
  HyperAPIUtils.generateSign('', ordS).then((signature) => {
    let r = {
      requestId: 'dfkljfdskjlj',
      payload: {
        action: 'updateOrder',
        orderDetails: ordS,
        signature: signature,
      },
      service: 'in.juspay.hyperpay',
    };
    console.log('process called', r);
    HyperSdkReact.process(JSON.stringify(r));
  });
}

const JuspayTopViewAttached = () => {
  console.log(getOrderDetails());
  const [text, onChangeText] = React.useState(getOrderDetails().amount);
  return (
    <View style={styles.container}>
      <Text style={styles.hello}>amount</Text>
      <TextInput style={styles.input} onChangeText={onChangeText} />
      <Button title="Update Amount" onPress={() => triggerUpdateOrder(text)} />
    </View>
  );
};
const styles = StyleSheet.create({
  container: {
    height: 'auto',
    justifyContent: 'center',
  },
  hello: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
    height: 24,
  },
  input: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
});

export default JuspayTopViewAttached;
