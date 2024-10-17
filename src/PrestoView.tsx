import * as React from 'react';
import HyperSDKView, {
  HyperEvent,
  NativeProps,
} from './HyperSDKViewNativeComponent';
import { DimensionValue, NativeSyntheticEvent, View } from 'react-native';

type ComponentRef = InstanceType<typeof HyperSDKView>;

type Props = NativeProps & {
  height: DimensionValue;
  width: DimensionValue;
  onEvent: (data: HyperEvent) => void;
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
        onHyperEvent={function (
          event: NativeSyntheticEvent<HyperEvent>
        ): void | Promise<void> {
          const eventData = {
            event: event.nativeEvent.event,
            data: JSON.parse(event.nativeEvent.data),
          };
          props.onEvent(eventData);
        }}
      />
    </View>
  );
};

export default PrestoView;
