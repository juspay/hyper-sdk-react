import { AppRegistry } from 'react-native';
import App from './src/App';
import JuspayTopView from './src/JuspayTopView';
import JuspayTopViewAttached from './src/JuspayTopViewAttached';
import { name as appName } from './app.json';
import HyperSdkReact from 'hyper-sdk-react';

AppRegistry.registerComponent(appName, () => App);

HyperSdkReact.notifyAboutRegisterComponent(HyperSdkReact.JuspayHeaderAttached);
AppRegistry.registerComponent(
  HyperSdkReact.JuspayHeaderAttached,
  () => JuspayTopViewAttached
);
HyperSdkReact.notifyAboutRegisterComponent(HyperSdkReact.JuspayHeader);
AppRegistry.registerComponent(HyperSdkReact.JuspayHeader, () => JuspayTopView);
