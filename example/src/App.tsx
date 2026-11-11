/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import * as React from 'react';
import {
  BackHandler,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import HomeScreen from './HomeScreen';
import ProcessScreen from './ProcessScreen';

// Two-screen switch: HomeScreen stays mounted underneath ProcessScreen, like a stack.
const App = () => {
  const [processParams, setProcessParams] = React.useState<object | null>(null);
  const processParamsRef = React.useRef(processParams);
  processParamsRef.current = processParams;

  const navigation = React.useMemo(
    () => ({
      navigate: (screen: string, params: object = {}) => {
        if (screen === 'ProcessScreen') {
          setProcessParams(params);
        }
      },
      goBack: () => setProcessParams(null),
    }),
    []
  );

  React.useEffect(() => {
    // Registered once, before ProcessScreen's listener, so ProcessScreen still gets
    // the back press first (popups, SDK back press) and only then do we go back home.
    const subscription = BackHandler.addEventListener(
      'hardwareBackPress',
      () => {
        if (processParamsRef.current) {
          setProcessParams(null);
          return true;
        }
        return false;
      }
    );
    return () => subscription.remove();
  }, []);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.safeArea} edges={['top']}>
        <View style={styles.header}>
          {processParams && (
            <TouchableOpacity style={styles.back} onPress={navigation.goBack}>
              <Text style={styles.backText}>‹ Back</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.title}>
            {processParams ? 'Process Screen' : 'Home Screen'}
          </Text>
        </View>
        <View style={processParams ? styles.hidden : styles.content}>
          <HomeScreen navigation={navigation} />
        </View>
        {processParams && (
          <View style={styles.content}>
            <ProcessScreen
              navigation={navigation}
              route={{ params: processParams }}
            />
          </View>
        )}
      </SafeAreaView>
    </SafeAreaProvider>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  header: {
    height: 56,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#d8d8d8',
  },
  title: {
    fontSize: 17,
    fontWeight: '600',
    color: '#1c1c1e',
  },
  back: {
    position: 'absolute',
    left: 12,
    top: 0,
    bottom: 0,
    justifyContent: 'center',
  },
  backText: {
    fontSize: 17,
    color: '#007aff',
  },
  content: {
    flex: 1,
    backgroundColor: '#f2f2f2',
  },
  hidden: {
    display: 'none',
  },
});

export default App;
