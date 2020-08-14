import * as React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';
import HyperSdkReact from 'hyper-sdk-react';
import HyperUtils from './Utils';

class App extends React.Component {
  preFetchPayload: {};
  initiatePayload: {};
  eventListener: any;

  constructor(props: {}, context: any) {
    super(props, context);
    this.preFetchPayload = HyperUtils.generatePreFetchPayload();
    this.initiatePayload = {};
  }

  componentDidMount() {
    const eventEmitter = new NativeEventEmitter(NativeModules.HyperSdkReact);
    this.eventListener = eventEmitter.addListener('HyperEvent', (event) => {
      console.warn(event);
    });
  }

  componentWillUnmount() {
    this.eventListener.remove();
  }

  render() {
    return (
      <View style={styles.container}>
        <CustomButton
          title="preFetch"
          onPress={() => {
            console.warn('preFetchPayload:', this.preFetchPayload);
            HyperSdkReact.preFetch(JSON.stringify(this.preFetchPayload));
          }}
        />
        <CustomButton
          title="Create HyperService Object"
          onPress={() => {
            HyperSdkReact.createHyperServices();
          }}
        />
        <CustomButton
          title="Generate Initiate Payload"
          onPress={() => {
            this.initiatePayload = HyperUtils.generateInitiatePayload();
          }}
        />
        <CustomButton
          title="Initiate"
          onPress={() => {
            console.warn('initiatePayload:', this.initiatePayload);
            HyperSdkReact.initiate(JSON.stringify(this.initiatePayload));
          }}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  button: {
    backgroundColor: 'blue',
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 25,
    marginVertical: 12,
  },
  buttonText: {
    color: 'white',
    fontSize: 18,
  },
});

const CustomButton = (props: any) => {
  return (
    <TouchableOpacity onPress={props.onPress} style={styles.button}>
      <Text style={styles.buttonText}>{props.title}</Text>
    </TouchableOpacity>
  );
};

export default App;
