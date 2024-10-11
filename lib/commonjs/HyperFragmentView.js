"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var React = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
function _getRequireWildcardCache(e) { if ("function" != typeof WeakMap) return null; var r = new WeakMap(), t = new WeakMap(); return (_getRequireWildcardCache = function (e) { return e ? t : r; })(e); }
function _interopRequireWildcard(e, r) { if (!r && e && e.__esModule) return e; if (null === e || "object" != typeof e && "function" != typeof e) return { default: e }; var t = _getRequireWildcardCache(r); if (t && t.has(e)) return t.get(e); var n = { __proto__: null }, a = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var u in e) if ("default" !== u && {}.hasOwnProperty.call(e, u)) { var i = a ? Object.getOwnPropertyDescriptor(e, u) : null; i && (i.get || i.set) ? Object.defineProperty(n, u, i) : n[u] = e[u]; } return n.default = e, t && t.set(e, n), n; }
/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

var HyperFragmentViewManager;
if (_reactNative.Platform.OS === 'android') {
  HyperFragmentViewManager = (0, _reactNative.requireNativeComponent)('HyperFragmentViewManager');
} else {
  HyperFragmentViewManager = (0, _reactNative.requireNativeComponent)('HyperFragmentViewManagerIOS');
}
const createFragment = (viewId, namespace, payload) => {
  if (_reactNative.Platform.OS === 'android') {
    _reactNative.UIManager.dispatchViewManagerCommand(viewId,
    //@ts-ignore
    _reactNative.UIManager.HyperFragmentViewManager.Commands.process.toString(), [viewId, namespace, payload]);
  } else {
    const commandId = _reactNative.UIManager.getViewManagerConfig('HyperFragmentViewManagerIOS').Commands.process;
    if (typeof commandId !== 'undefined') {
      _reactNative.UIManager.dispatchViewManagerCommand(viewId, commandId, [namespace, payload]);
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
    const viewId = (0, _reactNative.findNodeHandle)(ref.current);
    if (viewId) {
      createFragment(viewId, namespace, payload);
    }
  }, [namespace, payload]);
  if (!HyperFragmentViewManager) {
    return null;
  }
  return /*#__PURE__*/React.createElement(_reactNative.View, {
    style: {
      height: height,
      width: width
    }
  }, /*#__PURE__*/React.createElement(HyperFragmentViewManager, {
    ref: ref
  }));
};
var _default = exports.default = HyperFragmentView;
//# sourceMappingURL=HyperFragmentView.js.map