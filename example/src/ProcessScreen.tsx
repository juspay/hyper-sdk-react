/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import 'react-native-gesture-handler';
import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeEventEmitter,
  NativeModules,
  TextInput,
  BackHandler,
  Animated,
  Dimensions,
  ScrollView,
} from 'react-native';
// import { Picker } from '@react-native-picker/picker';
import CheckBox from '@react-native-community/checkbox';
import HyperSdkReact, { HyperFragmentView } from 'hyper-sdk-react';
import HyperAPIUtils from './API';
import HyperUtils from './Utils';
import { useNavigation } from '@react-navigation/native';
import { setOrderDetails } from './DataStore';
import { Picker } from '@react-native-picker/picker';

class ProcessScreen extends React.Component {
  state = {
    pickerSelected: 'flyer',
    upiSdkPresent: false,
    saveToLocker: false,
    shouldLink: false,
    resultText: '',
    animation: new Animated.Value(0),
    isPayloadGenerated: false,
    hideWidget: false,
    ppPayload: '',
  };

  navigation: any;
  eventListener: any;
  isPopupVisible: boolean;

  merchantId: string;
  clientId: string;
  merchantKeyId: string;
  privateKey: string;
  service: string;
  customerId: string;
  mobile: string;
  email: string;
  apiKey: string;
  amount: string;

  orderId: string;
  clientAuthToken: string;
  orderDetails: {};
  signature: string;
  // nbTxn
  nbTxnBank: string;
  // cardTxn
  cardNumber: string;
  cardToken: string;
  cardNetwork: string;
  expMonth: string;
  expYear: string;
  cvv: string;
  authType: string;
  // upiTxn
  upiTxnApp: string;
  vpa: string;
  // createWallet
  walletName: string;
  //linkWallet
  walletId: string;
  otp: string;
  // walletTxn
  directWalletToken: string;
  sdkPresent: string;
  walletMobile: string;
  flyerPayload: string;

  constructor(props: { navigation: any; route: any }, context: any) {
    super(props, context);
    this.navigation = props.navigation;
    this.isPopupVisible = false;

    const params = props.route.params;
    this.merchantId = params.merchantId;
    this.clientId = params.clientId;
    this.merchantKeyId = params.merchantKeyId;
    this.privateKey = params.privateKey;
    this.service = params.service;
    this.customerId = params.customerId;
    this.mobile = params.mobile;
    this.email = params.email;
    this.apiKey = params.apiKey;
    this.amount = params.amount;

    this.orderId = '';
    this.clientAuthToken = '';
    this.orderDetails = {};
    this.signature = '';

    this.nbTxnBank = 'NB_SBI';
    this.cardNumber = '';
    this.cardToken = '';
    this.cardNetwork = '';
    this.expMonth = '';
    this.expYear = '';
    this.cvv = '';
    this.authType = '';
    this.upiTxnApp = 'net.one97.paytm';
    this.vpa = '';
    this.walletName = 'PAYTM';
    this.directWalletToken = '';
    this.walletId = '';
    this.otp = '';
    this.sdkPresent = '';
    this.walletMobile = '';
    this.flyerPayload = JSON.stringify({});

    if (this.service === 'pp') {
      this.state.pickerSelected = 'paymentPage';
    }
  }

  componentDidMount() {
    const eventEmitter = new NativeEventEmitter(NativeModules.HyperSdkReact);
    this.eventListener = eventEmitter.addListener(
      HyperSdkReact.HyperEvent,
      (resp) => {
        // HyperUtils.alertCallbackResponse('ProcessScreen', resp);
        this.setState({ resultText: resp });
      }
    );

    BackHandler.addEventListener('hardwareBackPress', () => {
      if (this.isPopupVisible) {
        this.handleClose();
        return true;
      }
      return !HyperSdkReact.isNull() && HyperSdkReact.onBackPressed();
    });
  }

  UpdateOrderData = () => {
    const [text, onChangeText] = React.useState('');
    const [number, onChangeNumber] = React.useState(this.amount);
    return (
      <View style={styles.containerNew}>
        <View style={styles.horizontal}>
          <TextInput
            style={styles.input2}
            onChangeText={onChangeText}
            value={text}
            placeholder="Coupon Code"
          />
          <TextInput
            style={styles.input2}
            onChangeText={onChangeNumber}
            value={number}
            placeholder="Amount"
            keyboardType="numeric"
          />
        </View>
        <CustomButton2
          title="Update Order"
          onPress={() => {
            let orderDetailsNew = {};
            if (text !== '') {
              orderDetailsNew = {
                merchant_id: this.merchantId,
                customer_id: this.customerId,
                order_id: this.orderId,
                amount: number,
                mobile_number: this.mobile,
                customer_email: this.email,
                timestamp: HyperUtils.getTimestamp(),
                offer_details: {
                  offer_applied: String(text !== ''),
                  offer_code: text,
                  amount: number,
                },
              };
            } else {
              orderDetailsNew = {
                merchant_id: this.merchantId,
                customer_id: this.customerId,
                order_id: this.orderId,
                amount: number,
                mobile_number: this.mobile,
                customer_email: this.email,
                timestamp: HyperUtils.getTimestamp(),
              };
            }
            let signNew = '';
            HyperAPIUtils.generateSign(
              this.privateKey,
              JSON.stringify(orderDetailsNew)
            )
              .then((resp) => {
                signNew = resp;
                // HyperUtils.showCopyAlert(
                //   'Payload signed',
                //   this.signature
                // );
              })
              .catch((err) => {
                console.warn(err);
              });
            var payload = HyperUtils.generateProcessPayloadPP(
              'updateOrder',
              this.clientId,
              this.merchantId,
              JSON.stringify(orderDetailsNew),
              signNew,
              this.merchantKeyId
            );
            HyperSdkReact.process(JSON.stringify(payload));
          }}
        />
      </View>
    );
  };

  componentWillUnmount() {
    this.eventListener.remove();
    BackHandler.removeEventListener('hardwareBackPress', () => null);
  }

  handleOpen = () => {
    Animated.timing(this.state.animation, {
      toValue: 1,
      duration: 300,
      useNativeDriver: true,
    }).start();

    this.isPopupVisible = true;
  };

  handleClose = () => {
    Animated.timing(this.state.animation, {
      toValue: 0,
      duration: 200,
      useNativeDriver: true,
    }).start();

    this.isPopupVisible = false;
  };
  toggleVisibility = () => {
    this.setState({ hideWidget: !this.state.hideWidget });
  };
  render() {
    const screenHeight = Dimensions.get('window').height;

    const backdrop = {
      transform: [
        {
          translateY: this.state.animation.interpolate({
            inputRange: [0, 0.01],
            outputRange: [screenHeight, 0],
            extrapolate: 'clamp',
          }),
        },
      ],
      opacity: this.state.animation.interpolate({
        inputRange: [0.01, 0.5],
        outputRange: [0, 1],
        extrapolate: 'clamp',
      }),
    };

    const slideUp = {
      transform: [
        {
          translateY: this.state.animation.interpolate({
            inputRange: [0.01, 1],
            outputRange: [0, -1 * screenHeight],
            extrapolate: 'clamp',
          }),
        },
      ],
    };

    return (
      <View style={styles.root}>
        <ScrollView>
          <View style={styles.container}>
            <this.UpdateOrderData />

            <CustomButton
              title={
                this.service === 'ec' ? 'Create Order' : 'Generate Order ID'
              }
              onPress={() => {
                this.orderId = HyperUtils.generateOrderId();
                // console.warn('merchantId:', this.merchantId);
                // console.warn('orderId:', this.orderId);
                if (this.service === 'ec') {
                  HyperAPIUtils.generateOrder(
                    this.orderId,
                    this.amount,
                    this.customerId,
                    this.mobile,
                    this.email,
                    this.apiKey
                  )
                    .then((resp) => {
                      console.log(resp);
                      HyperUtils.showCopyAlert('OrderID', this.orderId);
                      this.clientAuthToken =
                        HyperUtils.getClientAuthToken(resp);
                      // console.warn('clientAuthToken:', this.clientAuthToken);
                    })
                    .catch((err) => {
                      console.error(err);
                    });
                } else {
                  HyperUtils.showCopyAlert('OrderID', this.orderId);
                }
              }}
            />
            <View style={styles.pickerContainer}>
              {this.service === 'ec' ? (
                <Picker
                  style={styles.picker}
                  selectedValue={this.state.pickerSelected}
                  onValueChange={(val, index) => {
                    this.setState({ pickerSelected: val });
                    console.log(val, index);
                  }}
                >
                  <Picker.Item label="Get Payment Methods" value="getPM" />
                  <Picker.Item label="List Saved Cards" value="cardList" />
                  <Picker.Item label="Get UPI Apps" value="getUPI" />
                  <Picker.Item
                    label="Refresh Wallet Balances"
                    value="listWallet"
                  />
                  <Picker.Item
                    label="Check IsDeviceReady"
                    value="isDeviceReady"
                  />
                  <Picker.Item label="NB Txn" value="nbTxn" />
                  <Picker.Item label="Card Txn" value="cardTxn" />
                  <Picker.Item label="UPI Txn" value="upiTxn" />
                  <Picker.Item label="Create Wallet" value="createWallet" />
                  <Picker.Item label="Link Wallet" value="linkWallet" />
                  <Picker.Item label="Wallet Txn" value="walletTxn" />
                  <Picker.Item label="Delete Saved Card" value="deleteCard" />
                  <Picker.Item label="DeLink Wallet" value="delinkWallet" />
                  <Picker.Item label="Flyer" value="flyer" />
                </Picker>
              ) : (
                <Picker
                  style={styles.picker}
                  selectedValue={this.state.pickerSelected}
                  onValueChange={(val, index) => {
                    this.setState({ pickerSelected: val });
                    console.log(val, index);
                  }}
                >
                  <Picker.Item label="paymentPage" value="paymentPage" />
                  <Picker.Item label="quickPay" value="quickPay" />
                </Picker>
              )}
            </View>

            {this.state.pickerSelected === 'getPM' ? (
              <CustomButton
                title="Get Payment Methods"
                onPress={() => {
                  var payload = HyperUtils.generatePaymentMethodsPayload();
                  HyperSdkReact.process(JSON.stringify(payload));
                }}
              />
            ) : null}

            {this.state.pickerSelected === 'cardList' ? (
              <CustomButton
                title="List Saved Cards"
                onPress={() => {
                  var payload = HyperUtils.generateListCardsPayload(
                    this.clientAuthToken
                  );
                  HyperSdkReact.process(JSON.stringify(payload));
                }}
              />
            ) : null}

            {this.state.pickerSelected === 'getUPI' ? (
              <CustomButton
                title="Get UPI Apps"
                onPress={() => {
                  var payload = HyperUtils.generateGetUPIAppsPayload(
                    this.orderId
                  );
                  HyperSdkReact.process(JSON.stringify(payload));
                }}
              />
            ) : null}

            {this.state.pickerSelected === 'flyer' ? (
              <CustomButton
                title="Generate Flyer Payload"
                onPress={() => {
                  this.setState({ isPayloadGenerated: true });
                }}
              />
            ) : null}

            {this.state.pickerSelected === 'listWallet' ? (
              <CustomButton
                title="Refresh Wallet Balances"
                onPress={() => {
                  var payload = HyperUtils.generateListWalletsPayload(
                    this.clientAuthToken
                  );
                  console.log(payload);
                  HyperSdkReact.process(JSON.stringify(payload));
                }}
              />
            ) : null}

            {this.state.pickerSelected === 'isDeviceReady' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="sdkPresent"
                  onChangeText={(text) => {
                    this.sdkPresent = text;
                  }}
                  defaultValue={this.sdkPresent}
                />
                <CustomButton
                  title="Is Device Ready"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateDeviceReadyPayload(
                      this.sdkPresent
                    );
                    console.log(payload);
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'nbTxn' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="NB_SBI"
                  onChangeText={(text) => {
                    this.nbTxnBank = text;
                  }}
                  defaultValue={this.nbTxnBank}
                />
                <CustomButton
                  title="NB Txn"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateNBTxnPayload(
                      this.orderId,
                      this.clientAuthToken,
                      this.nbTxnBank
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'cardTxn' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="card number (new card)"
                  onChangeText={(text) => {
                    this.cardNumber = text;
                  }}
                  defaultValue={this.cardNumber}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="card token (saved card)"
                  onChangeText={(text) => {
                    this.cardToken = text;
                  }}
                  defaultValue={this.cardToken}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="expMonth (for new card)"
                  onChangeText={(text) => {
                    this.expMonth = text;
                  }}
                  defaultValue={this.expMonth}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="expYear (for new card)"
                  onChangeText={(text) => {
                    this.expYear = text;
                  }}
                  defaultValue={this.expYear}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="card network (e.g. VISA)"
                  onChangeText={(text) => {
                    this.cardNetwork = text;
                  }}
                  defaultValue={this.cardNetwork}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="cvv"
                  onChangeText={(text) => {
                    this.cvv = text;
                  }}
                  defaultValue={this.cvv}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="authType"
                  onChangeText={(text) => {
                    this.authType = text;
                  }}
                  defaultValue={this.authType}
                />
                <CustomCheckBox
                  value={this.state.saveToLocker}
                  onValueChange={(toggle: boolean) => {
                    console.log(toggle);
                    this.setState({ saveToLocker: toggle });
                  }}
                  text="Save to locker"
                />
                <CustomButton
                  title="Card Txn"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateCardTxnPayload(
                      this.orderId,
                      this.clientAuthToken,
                      this.cardNetwork,
                      this.cardToken,
                      this.cardNumber,
                      this.expMonth,
                      this.expYear,
                      this.cvv,
                      this.authType,
                      this.state.saveToLocker
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'upiTxn' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="package name"
                  onChangeText={(text) => {
                    this.upiTxnApp = text;
                  }}
                  defaultValue={this.upiTxnApp}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="vpa"
                  onChangeText={(text) => {
                    this.vpa = text;
                  }}
                  defaultValue={this.vpa}
                />
                <CustomCheckBox
                  value={this.state.upiSdkPresent}
                  onValueChange={(toggle: boolean) => {
                    console.log(toggle);
                    this.setState({ upiSdkPresent: toggle });
                  }}
                  text="UPI SDK Present"
                />
                <CustomButton
                  title="UPI Txn"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateUPIIntentTxnPayload(
                      this.orderId,
                      this.clientAuthToken,
                      this.upiTxnApp,
                      this.vpa,
                      this.state.upiSdkPresent
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'createWallet' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="PAYTM"
                  onChangeText={(text) => {
                    this.walletName = text;
                  }}
                  defaultValue={this.walletName}
                />
                <CustomButton
                  title="Create Wallet"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateCreateWalletPayload(
                      this.walletName,
                      this.clientAuthToken
                    );
                    console.log(payload);
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'linkWallet' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="walletId from createWallet"
                  onChangeText={(text) => {
                    this.walletId = text;
                  }}
                  defaultValue={this.walletId}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="Enter otp"
                  onChangeText={(text) => {
                    this.otp = text;
                  }}
                  defaultValue={this.otp}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="PAYTM"
                  onChangeText={(text) => {
                    this.walletName = text;
                  }}
                  defaultValue={this.walletName}
                />
                <CustomButton
                  title="Link Wallet"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateLinkWalletPayload(
                      this.walletName,
                      this.walletId,
                      this.otp,
                      this.clientAuthToken
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'walletTxn' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="paymentMethod"
                  onChangeText={(text) => {
                    this.walletName = text;
                  }}
                  defaultValue={this.walletName}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="directWalletToken"
                  onChangeText={(text) => {
                    this.directWalletToken = text;
                  }}
                  defaultValue={this.directWalletToken}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="sdkPresent"
                  onChangeText={(text) => {
                    this.sdkPresent = text;
                  }}
                  defaultValue={this.sdkPresent}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="walletMobileNumber"
                  onChangeText={(text) => {
                    this.walletMobile = text;
                  }}
                  defaultValue={this.walletMobile}
                />
                <CustomCheckBox
                  value={this.state.shouldLink}
                  onValueChange={(toggle: boolean) => {
                    console.log(toggle);
                    this.setState({ shouldLink: toggle });
                  }}
                  text="Should Link"
                />
                <CustomButton
                  title="Wallet Txn"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateWalletTxnPayload(
                      this.orderId,
                      this.clientAuthToken,
                      this.walletName,
                      this.directWalletToken,
                      this.sdkPresent
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'delinkWallet' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="walletName"
                  onChangeText={(text) => {
                    this.walletName = text;
                  }}
                  defaultValue={this.walletName}
                />
                <TextInput
                  style={styles.editText}
                  placeholder="walletId"
                  onChangeText={(text) => {
                    this.walletId = text;
                  }}
                  defaultValue={this.walletId}
                />
                <CustomButton
                  title="DeLink Wallet"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateDeLinkWalletPayload(
                      this.walletName,
                      this.walletId,
                      this.clientAuthToken
                    );
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.state.pickerSelected === 'deleteCard' ? (
              <View>
                <TextInput
                  style={styles.editText}
                  placeholder="cardToken"
                  onChangeText={(text) => {
                    this.cardToken = text;
                  }}
                  defaultValue={this.cardToken}
                />
                <CustomButton
                  title="Delete Card"
                  onPress={() => {
                    var payload: {} = HyperUtils.generateDeleteCardPayload(
                      this.cardToken,
                      this.clientAuthToken
                    );
                    console.log(payload);
                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
              </View>
            ) : null}

            {this.service === 'pp' ? (
              <View>
                <CustomButton
                  title="Sign Order Details"
                  onPress={() => {
                    let x: {
                      merchant_id: String;
                      customer_id: String;
                      order_id: String;
                      amount: String;
                      mobile_number: String;
                      customer_email: String;
                      timestamp: String;
                      features: any;
                    } = {
                      merchant_id: this.merchantId,
                      customer_id: this.customerId,
                      order_id: this.orderId,
                      amount: this.amount,
                      mobile_number: this.mobile,
                      customer_email: this.email,
                      timestamp: HyperUtils.getTimestamp(),
                      features: { paymentWidget: { enable: true } },
                    };
                    this.orderDetails = x;
                    setOrderDetails(x);
                    HyperAPIUtils.generateSign(
                      this.privateKey,
                      JSON.stringify(this.orderDetails)
                    )
                      .then((resp) => {
                        this.signature = resp;
                        HyperUtils.showCopyAlert(
                          'Payload signed',
                          this.signature
                        );
                      })
                      .catch((err) => {
                        console.warn(err);
                      });
                  }}
                />
                <CustomButton
                  title="Process"
                  onPress={() => {
                    var payload = HyperUtils.generateProcessPayloadPP(
                      this.state.pickerSelected,
                      this.clientId,
                      this.merchantId,
                      JSON.stringify(this.orderDetails),
                      this.signature,
                      this.merchantKeyId
                    );

                    HyperSdkReact.process(JSON.stringify(payload));
                  }}
                />
                <CustomButton
                  title="Process With New Activity"
                  onPress={() => {
                    var payload = HyperUtils.generateProcessPayloadPP(
                      this.state.pickerSelected,
                      this.clientId,
                      this.merchantId,
                      JSON.stringify(this.orderDetails),
                      this.signature,
                      this.merchantKeyId
                    );

                    HyperSdkReact.processWithActivity(JSON.stringify(payload));
                  }}
                />

                <CustomButton
                  title="Generate Process Payload"
                  onPress={() => {
                    this.setState({
                      isPayloadGenerated: true,
                      ppPayload: JSON.stringify(
                        HyperUtils.generateProcessPayloadPP(
                          this.state.pickerSelected,
                          this.clientId,
                          this.merchantId,
                          JSON.stringify(this.orderDetails),
                          this.signature,
                          this.merchantKeyId
                        )
                      ),
                    });
                  }}
                />
              </View>
            ) : null}

            {/* <CustomButton
              title="Is Initialised?"
              onPress={() => {
                HyperSdkReact.isInitialised().then((init: boolean) => {
                  // console.warn('isInitialised:', init);
                  HyperUtils.showCopyAlert('isInitialised', init + '');
                });
              }}
            /> */}
            <CustomButton
              title="Terminate"
              onPress={() => {
                HyperSdkReact.terminate();
              }}
            />
            {/* <CustomButton
              title="Check Result"
              onPress={() => {
                this.handleOpen();
              }}
            />
            <View style={styles.containerSon}>
              {this.state.isPayloadGenerated ? (
                <HyperFragmentView
                  height={250}
                  namespace={'quickPay'}
                  payload={
                    this.service === 'pp'
                      ? this.state.ppPayload
                      : this.flyerPayload
                  }
                />
              ) : null}
            /> */}
            <View style={styles.horizontal}>
              <CustomButton
                title="Hide Widget"
                onPress={() => {
                  this.toggleVisibility();
                }}
              />
              <CustomButton
                title="Check Offers"
                onPress={() => {
                  this.navigation.navigate('OfferScreen');
                }}
              />
            </View>
          </View>
        </ScrollView>

        <Animated.View
          style={[StyleSheet.absoluteFill, styles.cover, backdrop]}
        >
          <View style={[styles.sheet]}>
            <Animated.View style={[styles.popup, slideUp]}>
              <View>
                <TextInput
                  style={styles.textArea}
                  underlineColorAndroid="transparent"
                  placeholder="Type something"
                  placeholderTextColor="grey"
                  numberOfLines={100}
                  multiline={true}
                  value={this.state.resultText}
                />
                <CustomButton title="Close" onPress={this.handleClose} />
              </View>
            </Animated.View>
          </View>
        </Animated.View>
        <View
          style={[
            styles.horizontal2,
            this.state.hideWidget ? styles.horizontal2Gone : styles.horizontal2,
          ]}
        >
          <View
            style={[
              styles.newWala,
              // this.state.isPayloadGenerated ? styles.newWala : null,
            ]}
          >
            {this.state.isPayloadGenerated ? (
              <HyperFragmentView
                height={103}
                payload={
                  this.service === 'pp'
                    ? this.state.ppPayload
                    : this.flyerPayload
                }
                namespace={'paymentWidget'}
              />
            ) : null}
          </View>
          {/* <View style={styles.containerSon2}>
            <CustomButton3
              title="Proceed To Pay"
              onPress={() => {
                const data = {
                  requestId: 'testid',
                  service: 'in.juspay.hyperpay',
                  payload: {
                    action: 'updateDetails',
                    hidePaymentWidget: false,
                  },
                };
                HyperSdkReact.process(JSON.stringify(data));
              }}
            />
          </View> */}
        </View>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  containerNew: {
    height: 50,
    width: '100%',
    // backgroundColor: 'yellow',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  containerSon: {
    height: 'auto',
    // width: 'auto',
    flex: 0,
    backgroundColor: 'white',
  },
  newWala: {
    height: 'auto',
    // width: 'auto',
    flex: 1,
    backgroundColor: 'white',
  },
  containerSon2: {
    height: 'auto',
    // width: '100%',
    flex: 1,
    backgroundColor: 'white',
    alignItems: 'center',
    justifyContent: 'center',
  },
  horizontal: {
    flexDirection: 'row',
  },
  horizontal2: {
    flexDirection: 'row',
    height: 103,
    width: '100%',
    position: 'absolute',
    bottom: 0,
  },
  horizontal2Gone: {
    display: 'none',
    visibility: 'none',
  },
  input: {
    borderColor: 'black',
    borderWidth: 1,
    margin: 2,
  },
  input2: {
    borderColor: 'black',
    color: 'black',
    borderWidth: 1,
    margin: 2,
    // placeholderTextColor: 'black',
  },
  button: {
    backgroundColor: 'blue',
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 25,
    marginVertical: 12,
  },
  button2: {
    backgroundColor: 'blue',
    borderRadius: 25,
    width: 200,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  button3: {
    backgroundColor: '#FF3269',
    borderRadius: 25,
    width: 200,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    color: 'white',
    fontSize: 18,
  },
  editText: {
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 25,
    marginVertical: 12,
    marginEnd: 20,
  },
  checkbox: {
    width: 40,
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    paddingVertical: 12,
    borderRadius: 20,
    marginVertical: 12,
  },
  picker: {
    width: 250,
  },
  pickerContainer: {
    borderColor: 'gray',
    borderWidth: 1,
    borderRadius: 12,
  },
  centerAligned: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  marginEnd: {
    marginEnd: 20,
  },
  cover: {
    backgroundColor: 'rgba(0,0,0,.5)',
  },
  sheet: {
    position: 'absolute',
    top: Dimensions.get('window').height,
    left: 0,
    right: 0,
    height: '100%',
    justifyContent: 'flex-end',
  },
  popup: {
    backgroundColor: '#FFF',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 80,
  },
  textArea: {
    borderColor: 'gray',
    height: 150,
    justifyContent: 'flex-start',
    minHeight: 40,
    borderWidth: 1,
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 12,
    marginVertical: 12,
    marginHorizontal: 12,
  },
});

const CustomButton = (props: any) => {
  return (
    <TouchableOpacity onPress={props.onPress} style={styles.button}>
      <Text style={styles.buttonText}>{props.title}</Text>
    </TouchableOpacity>
  );
};

const CustomButton2 = (props: any) => {
  return (
    <TouchableOpacity onPress={props.onPress} style={styles.button2}>
      <Text style={styles.buttonText}>{props.title}</Text>
    </TouchableOpacity>
  );
};

// const CustomButton3 = (props: any) => {
//   return (
//     <TouchableOpacity onPress={props.onPress} style={styles.button3}>
//       <Text style={styles.buttonText}>{props.title}</Text>
//     </TouchableOpacity>
//   );
// };

const CustomCheckBox = (props: any) => {
  return (
    <View style={[styles.horizontal, styles.centerAligned]}>
      <CheckBox
        style={styles.checkbox}
        value={props.value}
        onValueChange={props.onValueChange}
        tintColors={{ true: 'blue' }} // only works for Android
      />
      <Text style={styles.marginEnd}>{props.text}</Text>
    </View>
  );
};

export default function WithNavigate(props: any) {
  const navigation = useNavigation();
  return <ProcessScreen {...props} navigate={navigation} />;
}
