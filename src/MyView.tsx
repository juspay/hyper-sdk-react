import * as React from 'react';
import {
    View,
    PixelRatio,
    UIManager,
    findNodeHandle,
    requireNativeComponent
} from 'react-native';

export interface MyViewProps {
    height?: number;
    width?: number;
}

export const MyViewManager =
    requireNativeComponent('MyViewManager');

const createFragment = (viewId: number) => {
    UIManager.dispatchViewManagerCommand(
        viewId,
        //@ts-ignore
        UIManager.MyViewManager.Commands.create.toString(),
        [viewId],
    );
}

const MyView: React.FC<MyViewProps> = ({ height, width }) => {
    const ref = React.useRef<View | null>(null);
    React.useEffect(() => {
        const viewId = findNodeHandle(ref.current);
        if (viewId) {
            createFragment(viewId);
        }
    }, [height, width]);

    return (
        <View style={{ height: height, width: width }}>
            <MyViewManager
                //@ts-ignore
                style={{
                    height: PixelRatio.getPixelSizeForLayoutSize(height || 0),
                    width: PixelRatio.getPixelSizeForLayoutSize(width || 0),
                }}
                ref={ref}
            />
        </View>
    )
}

export default MyView;
