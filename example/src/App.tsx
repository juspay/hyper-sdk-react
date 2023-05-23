/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import * as React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import HomeScreen from './HomeScreen';
import ProcessScreen from './ProcessScreen';

const AppNavigator = createStackNavigator();

const App = () => (
  <NavigationContainer>
    <AppNavigator.Navigator>
      <AppNavigator.Screen
        name="HomeScreen"
        component={HomeScreen}
        options={{ title: 'Home Screen' }}
      />
      <AppNavigator.Screen
        name="ProcessScreen"
        component={ProcessScreen}
        options={{ title: 'Process Screen' }}
      />
    </AppNavigator.Navigator>
  </NavigationContainer>
);

export default App;
