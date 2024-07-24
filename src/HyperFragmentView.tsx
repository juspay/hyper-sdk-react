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
}

const createFragment = (viewId: number, namespace: string, payload: string) => {
  UIManager.dispatchViewManagerCommand(
    viewId,
    //@ts-ignore
    UIManager.HyperFragmentViewManager.Commands.process.toString(),
    [viewId, namespace, payload]
  );
};

const HyperFragmentView: React.FC<HyperFragmentViewProps> = ({
  height,
  width,
  namespace,
  payload,
}) => {
  const ref = React.useRef<View | null>(null);
  React.useEffect(() => {
    if (Platform.OS == 'android') {
      const viewId = findNodeHandle(ref.current);
      if (viewId) {
        createFragment(viewId, namespace, payload);
      }
    }
  }, [namespace, payload]);

  if (!HyperFragmentViewManager) {
    return null;
  }

  return (
    <View style={{ height: height, width: width }}>
      <HyperFragmentViewManager ref={ref} />
    </View>
  );
};

export default HyperFragmentView;
