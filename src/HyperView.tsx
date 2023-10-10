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
} from 'react-native';

export interface HyperViewProps {
  height?: number;
  width?: number;
  namespace: string;
  payload: string;
}

const HyperViewManager = requireNativeComponent('HyperViewManager');

const createFragment = (viewId: number, namespace: string, payload: string) => {
  UIManager.dispatchViewManagerCommand(
    viewId,
    //@ts-ignore
    UIManager.HyperViewManager.Commands.process.toString(),
    [viewId, namespace, payload]
  );
};

const HyperView: React.FC<HyperViewProps> = ({
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

  return (
    <View style={{ height: height, width: width }}>
      <HyperViewManager ref={ref} />
    </View>
  );
};

export default HyperView;
