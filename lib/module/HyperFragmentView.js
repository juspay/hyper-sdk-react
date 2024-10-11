/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

import * as React from 'react';
import { View, UIManager, findNodeHandle, requireNativeComponent, Platform } from 'react-native';
var HyperFragmentViewManager;
if (Platform.OS === 'android') {
  HyperFragmentViewManager = requireNativeComponent('HyperFragmentViewManager');
} else {
  HyperFragmentViewManager = requireNativeComponent('HyperFragmentViewManagerIOS');
}
const createFragment = (viewId, namespace, payload) => {
  if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(viewId,
    //@ts-ignore
    UIManager.HyperFragmentViewManager.Commands.process.toString(), [viewId, namespace, payload]);
  } else {
    const commandId = UIManager.getViewManagerConfig('HyperFragmentViewManagerIOS').Commands.process;
    if (typeof commandId !== 'undefined') {
      UIManager.dispatchViewManagerCommand(viewId, commandId, [namespace, payload]);
    }
  }
};
const HyperFragmentView = ({
  height,
  width,
  namespace,
  payload
}) => {
  const ref = React.useRef(null);
  React.useEffect(() => {
    const viewId = findNodeHandle(ref.current);
    if (viewId) {
      createFragment(viewId, namespace, payload);
    }
  }, [namespace, payload]);
  if (!HyperFragmentViewManager) {
    return null;
  }
  return /*#__PURE__*/React.createElement(View, {
    style: {
      height: height,
      width: width
    }
  }, /*#__PURE__*/React.createElement(HyperFragmentViewManager, {
    ref: ref
  }));
};
export default HyperFragmentView;
//# sourceMappingURL=HyperFragmentView.js.map