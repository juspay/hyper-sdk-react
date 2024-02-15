import { AppRegistry } from 'react-native';
import App from './src/App';
import HelloWorld from './src/JuspayTopView';
import TopViewAttached from './src/JuspayTopViewAttached';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);

AppRegistry.registerComponent('JuspayHeaderAttached', () => HelloWorld);
AppRegistry.registerComponent('JuspayHeader', () => TopViewAttached);
