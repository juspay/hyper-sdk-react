import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import type { HostComponent, ViewProps } from 'react-native';
import { DirectEventHandler } from 'react-native/Libraries/Types/CodegenTypes';

export interface NativeProps extends ViewProps {
  _namespace: string;
  payload: string;
  onHyperEvent?: DirectEventHandler<HyperEvent>;
}

export type HyperEvent = Readonly<{
  event: Readonly<string>;
  data: Readonly<string>;
}>;

export default codegenNativeComponent<NativeProps>(
  'HyperSDKView'
) as HostComponent<NativeProps>;

type ComponentType = HostComponent<NativeProps>;

// Add NativeCommands interface including trigger as the new
// fabric native component method
interface NativeCommands {
  process: (
    viewRef: React.ElementRef<ComponentType>,
    _namespace: string,
    payload: string
  ) => void;
}
// Execute codegeNativeCommands function with proper supportedCommands
// as shown below and export it
export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['process'],
});
