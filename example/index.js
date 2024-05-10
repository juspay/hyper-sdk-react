import { AppRegistry } from 'react-native';
import App from './src/App';
import JuspayTopView from './src/JuspayTopView';
import JuspayTopViewAttached from './src/JuspayTopViewAttached';
import { name as appName } from './app.json';
import HyperSdkReact from 'hyper-sdk-react';

AppRegistry.registerComponent(appName, () => App);

AppRegistry.registerComponent(
  HyperSdkReact.JuspayHeaderAttached,
  () => JuspayTopViewAttached
);
AppRegistry.registerComponent(HyperSdkReact.JuspayHeader, () => JuspayTopView);
