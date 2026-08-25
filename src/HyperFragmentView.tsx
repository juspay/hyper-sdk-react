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
import type HyperServiceInstance from './HyperServiceManager';

export interface HyperFragmentViewProps {
  height?: number;
  width?: number;
  namespace: string;
  payload: string;
  /**
   * The HyperServiceInstance this view belongs to. Omit for the single-instance
   * (module-level) API.
   */
  instance?: HyperServiceInstance;
}
var HyperFragmentViewManager: any;

if (Platform.OS === 'android') {
  HyperFragmentViewManager = requireNativeComponent('HyperFragmentViewManager');
} else {
  HyperFragmentViewManager = requireNativeComponent(
    'HyperFragmentViewManagerIOS'
  );
}

const newArchEnabled = (global as any)?.nativeFabricUIManager ? true : false;

const createFragment = (
  viewId: number,
  namespace: string,
  payload: string,
  instanceKey: string
) => {
  if (!newArchEnabled) {
    if (Platform.OS === 'android') {
      UIManager.dispatchViewManagerCommand(
        viewId,
        //@ts-ignore
        UIManager.HyperFragmentViewManager.Commands.process.toString(),
        [viewId, namespace, payload, instanceKey]
      );
    } else {
      const commandId = UIManager.getViewManagerConfig(
        'HyperFragmentViewManagerIOS'
      ).Commands.process;
      if (typeof commandId !== 'undefined') {
        UIManager.dispatchViewManagerCommand(viewId, commandId, [
          namespace,
          payload,
          instanceKey,
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
  instance,
}) => {
  const ref = React.useRef<View | null>(null);
  const instanceKey = instance ? instance.getHyperEventString() : '';
  React.useEffect(() => {
    const viewId = findNodeHandle(ref.current);
    if (viewId) {
      createFragment(viewId, namespace, payload, instanceKey);
    }
  }, [namespace, payload, instanceKey]);

  if (!HyperFragmentViewManager) {
    return null;
  }

  return (
    <View style={{ height: height, width: width }}>
      {newArchEnabled ? (
        <HyperFragmentViewManager
          ref={ref}
          style={{ flex: 1 }}
          ns={namespace}
          payload={payload}
          hyperKey={instanceKey}
        />
      ) : (
        <HyperFragmentViewManager ref={ref} style={{ flex: 1 }} />
      )}
    </View>
  );
};

export default HyperFragmentView;
