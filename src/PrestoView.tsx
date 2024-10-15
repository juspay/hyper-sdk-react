import * as React from 'react';
import HyperSDKView, { NativeProps } from './HyperSDKViewNativeComponent';
import { View } from 'react-native';

type ComponentRef = InstanceType<typeof HyperSDKView>;

type Props = NativeProps & {
  height: number;
  width: number;
};

const PrestoView: React.FC<Props> = (props: Props) => {
  const ref = React.useRef<ComponentRef>(null);
  React.useEffect(() => {
    console.log('afterRender');
  }, [props._namespace, props.payload]);

  return (
    <View style={{ height: props.height, width: props.width }}>
      <HyperSDKView
        ref={ref}
        _namespace={props._namespace}
        payload={props.payload}
      />
    </View>
  );
};

export default PrestoView;
