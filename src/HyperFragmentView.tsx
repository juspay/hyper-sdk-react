/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import * as React from 'react';
import {
  View,
  UIManager,
  findNodeHandle,
  requireNativeComponent,
  Platform,
} from 'react-native';

export interface HyperFragmentViewProps {
  height?: number;
  width?: number;
  namespace: string;
  payload: string;
}
var HyperFragmentViewManager: any;

if (Platform.OS === 'android') {
  HyperFragmentViewManager = requireNativeComponent('HyperFragmentViewManager');
} else {
  HyperFragmentViewManager = requireNativeComponent(
    'HyperFragmentViewManagerIOS'
  );
}

const newArchEnabled = global?.nativeFabricUIManager ? true : false;

const createFragment = (viewId: number, namespace: string, payload: string) => {
  if(!newArchEnabled) {
    if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(
      viewId,
      //@ts-ignore
      UIManager.HyperFragmentViewManager.Commands.process.toString(),
      [viewId, namespace, payload]
    );
  } else {
    const commandId = UIManager.getViewManagerConfig(
      'HyperFragmentViewManagerIOS'
    ).Commands.process;
    if (typeof commandId !== 'undefined') {
      UIManager.dispatchViewManagerCommand(viewId, commandId, [
        namespace,
        payload,
      ]);
    }
  }

  }
  
};

const HyperFragmentView: React.FC<HyperFragmentViewProps> = ({
  height,
  width,
  namespace,
  payload,
}) => {
  const ref = React.useRef<View | null>(null);
  React.useEffect(() => {
    const viewId = findNodeHandle(ref.current);
    if (viewId) {
      createFragment(viewId, namespace, payload);
    }
  }, [namespace, payload]);

  if (!HyperFragmentViewManager) {
    return null;
  }

  return (
    <View style={{ height: height, width: width }}>
      {newArchEnabled?<HyperFragmentViewManager 
        ref={ref}
        style={{ flex: 1 }}
        ns={namespace}
        payload={payload}
      />:<HyperFragmentViewManager 
        ref={ref}
        style={{ flex: 1 }}
      />}
    </View>
  );
};

export default HyperFragmentView;
