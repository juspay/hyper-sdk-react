/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

import org.json.JSONObject;

import java.util.Map;

import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.services.HyperServices;

public class HyperFragmentViewManager extends ViewGroupManager<FrameLayout> {

    private static final String NAME = "HyperFragmentViewManager";
    private static final int COMMAND_PROCESS = 175;

    private final ReactApplicationContext reactContext;

    public HyperFragmentViewManager(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }


    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected FrameLayout createViewInstance(@NonNull ThemedReactContext context) {
        return new FrameLayout(context);
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("process", COMMAND_PROCESS);
    }

    @Override
    public void receiveCommand(@NonNull FrameLayout root, String commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);

        try {
            int commandIdInt = Integer.parseInt(commandId);

            if (commandIdInt == COMMAND_PROCESS) {
                // arg[0] - root view Id
                int reactNativeViewId = args != null ? args.getInt(0) : 0;
                ViewGroup rootViewGroup = root.findViewById(reactNativeViewId);
                setupLayout(rootViewGroup);

                // arg[1] - namespace for the fragmentViewGroup
                String namespace = args != null ? args.getString(1) : "";
                JSONObject fragments = new JSONObject();
                fragments.put(namespace, rootViewGroup);

                // arg[2] - process payload as Stringified JSON
                String payloadStr = args != null ? args.getString(2) : "{}";
                JSONObject payload = new JSONObject(payloadStr);
                payload.getJSONObject("payload").put("fragmentViewGroups", fragments);
                FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
                HyperServices hyperServices = HyperSdkReactModule.getHyperServices();
                if (activity == null) {
                    SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_ERROR,
                        LogConstants.SDK_TRACKER_LABEL,
                        "process_with_hyper_view",
                        "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_ERROR,
                        LogConstants.SDK_TRACKER_LABEL,
                        "process_with_hyper_view",
                        "hyperServices is null");
                    return;
                }

                hyperServices.process(activity, payload);
            }
        } catch (Exception e) {
            SdkTracker.trackAndLogBootException(
                NAME,
                LogConstants.CATEGORY_LIFECYCLE,
                LogConstants.SUBCATEGORY_HYPER_SDK,
                LogConstants.SDK_TRACKER_LABEL,
                "Exception in HyperFragmentViewManager.createCommand",
                e
            );
        }
    }

    private void setupLayout(View view) {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                try {
                    manuallyLayoutChildren(view);
                    view.getViewTreeObserver().dispatchOnGlobalLayout();
                    Choreographer.getInstance().postFrameCallback(this);
                } catch (Exception e) {
                    SdkTracker.trackAndLogBootException(
                        NAME,
                        LogConstants.CATEGORY_LIFECYCLE,
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.SDK_TRACKER_LABEL,
                        "Exception in HyperFragmentViewManager.doFrame",
                        e
                    );
                }
            }
        });
    }

    private void manuallyLayoutChildren(@NonNull View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent == null) return;
        int height = parent.getMeasuredHeight();
        int width = parent.getMeasuredWidth();

        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        );

        view.layout(0, 0, width, height);
    }
}