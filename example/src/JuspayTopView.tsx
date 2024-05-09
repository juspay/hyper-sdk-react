import React from 'react';
import { getOrderDetails } from './DataStore';
import { Dimensions, Image, StyleSheet, View } from 'react-native';
import HyperSdkReact from 'hyper-sdk-react';

const deviceWidth = Dimensions.get('window').width;

const JuspayTopView = () => {
  console.log(getOrderDetails());
  return (
    <View
      style={styles.container}
      onLayout={(event) => {
        const { height, width } = event.nativeEvent.layout;
        console.log('image height', height, width);
        HyperSdkReact.updateMerchantViewHeight('JuspayHeader', height);
      }}
    >
      <Image source={require('../images/promotion.png')} style={styles.image} />
    </View>
  );
};
const styles = StyleSheet.create({
  container: {
    justifyContent: 'center',
    width: '100%',
    height: (deviceWidth * 720) / 1022,
  },
  image: {
    flex: 1,
    width: '100%',
    resizeMode: 'contain',
  },
});

export default JuspayTopView;
