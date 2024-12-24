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
  DimensionValue,
} from 'react-native';

export interface HyperFragmentViewPropsPub {
  height: DimensionValue;
  width?: DimensionValue;
  namespace: string;
  payload: string;
  onEvent?: (data: HyperEvent) => void;
}

export type HyperEvent = { event: string; data: [string: any] };

export type HyperFragmentViewProps = HyperFragmentViewPropsPub & {
  onHyperEvent?: (event: {
    nativeEvent: { event: string; data: [string: any] };
  }) => void;
};

var HyperFragmentViewManager: any;

if (Platform.OS === 'android') {
  HyperFragmentViewManager = requireNativeComponent('HyperFragmentViewManager');
} else {
  HyperFragmentViewManager = requireNativeComponent(
    'HyperFragmentViewManagerIOS'
  );
}

const createFragment = (viewId: number, namespace: string, payload: string) => {
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
    ).Commands["process"];
    if (typeof commandId !== 'undefined') {
      UIManager.dispatchViewManagerCommand(viewId, commandId, [
        namespace,
        payload,
      ]);
    }
  }
};

const HyperFragmentView: React.FC<HyperFragmentViewProps> = (
  props: HyperFragmentViewProps
) => {
  const ref = React.useRef<View | null>(null);
  React.useEffect(() => {
    const viewId = findNodeHandle(ref.current);
    if (viewId) {
      createFragment(viewId, props.namespace, props.payload);
    }
  }, [props.namespace, props.payload]);

  if (!HyperFragmentViewManager) {
    return null;
  }
  const onHyperEvent = (event: {
    nativeEvent: { event: string; data: [string: any] };
  }) => {
    if (props.onEvent) props.onEvent(event.nativeEvent);
  };
  return (
    <View style={{ height: props.height, width: props.width }}>
      <HyperFragmentViewManager
        style={{ height: props.height, width: props.width }}
        ref={ref}
        payload={props.payload}
        namespace={props.namespace}
        onHyperEvent={onHyperEvent}
      />
    </View>
  );
};

export default HyperFragmentView;
