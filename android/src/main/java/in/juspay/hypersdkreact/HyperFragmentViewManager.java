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
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.juspay.hypersdk.core.MerchantViewType;
import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallback;
import in.juspay.mobility.app.InAppNotification;

public class HyperFragmentViewManager extends ViewGroupManager<HyperSDKView> {

    private static final String NAME = "HyperFragmentViewManager";
    private static final int COMMAND_PROCESS = 175;
    private HyperSDKView root;

    private final ReactApplicationContext reactContext;

    public HyperFragmentViewManager(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }


    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void addEventEmitters(@NonNull ThemedReactContext reactContext, @NonNull HyperSDKView view) {
        super.addEventEmitters(reactContext, view);
    }

    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        Map<String, Object> outerMap = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("registrationName", "onHyperEvent");

        outerMap.put(HyperOnEvent.EVENT_NAME, innerMap);

        return outerMap;
    }

    @NonNull
    @Override
    protected HyperSDKView createViewInstance(@NonNull ThemedReactContext context) {
        if (root == null) {
            root = new HyperSDKView(context);
        }
        return root;
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("process", COMMAND_PROCESS);
    }
    @Override
    public void receiveCommand(@NonNull HyperSDKView root, int commandId, @Nullable ReadableArray args) {
        receiveCommand(root,Integer.toString(commandId), args);
    }

    @Override
    public void receiveCommand(@NonNull HyperSDKView root, String commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);
        int reactTag = args != null ? args.getInt(0) : 0;
        String namespace = args != null ? args.getString(1) : "";
        String payloadStr = args != null ? args.getString(2) : "{}";
        if (root.getHyperServices().isInitialised()) {
            process(root, reactTag, namespace,payloadStr);
        } else {
            try {
                JSONObject payload = new JSONObject(payloadStr);
                root.getHyperServices().initiate(payload, new HyperPaymentsCallback() {
                    @Override
                    public void onStartWaitingDialogCreated(@Nullable View view) {

                    }

                    @Override
                    public void onEvent(JSONObject jsonObject, JuspayResponseHandler juspayResponseHandler) {
                        String event = jsonObject.optString("event", "");
                        switch (event) {
                            case "initiate_result": {
                                process(root,args != null ? args.getInt(0) : 0,args != null ? args.getString(1) : "",args != null ? args.getString(2) : "{}");
                            }
                            case "in_app_notification": {
                                try {
                                    InAppNotification.getInstance(reactContext.getCurrentActivity(), root).generateNotification(jsonObject);
                                } catch (JSONException e) {
                                    System.out.println("InAppNotification failed" + e);
                                }
                            }
                        }
                        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(reactTag,HyperOnEvent.EVENT_NAME,new HyperOnEvent(event,jsonObject).getEventData());
                    }

                    @Nullable
                    @Override
                    public View getMerchantView(ViewGroup viewGroup, MerchantViewType merchantViewType) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public WebViewClient createJuspaySafeWebViewClient() {
                        return null;
                    }
                });
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
    }

    private void process(@NonNull HyperSDKView root, int viewID, String namespace, String payloadStr) {
        try {
        // arg[0] - root view Id
            ViewGroup rootViewGroup = root.findViewById(viewID);
            setupLayout(rootViewGroup);

            // arg[1] - namespace for the fragmentViewGroup
            JSONObject fragments = new JSONObject();
            fragments.put(namespace, rootViewGroup);

            // arg[2] - process payload as Stringified JSON
            JSONObject payload = new JSONObject(payloadStr);
            payload.getJSONObject("payload").put("fragmentViewGroups", fragments);
            FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
            if (activity == null) {
                SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_ERROR,
                    LogConstants.SDK_TRACKER_LABEL,
                    "process_with_hyper_view",
                    "activity is null");
                return;
            }

            if (root.getHyperServices() == null) {
                SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_ERROR,
                    LogConstants.SDK_TRACKER_LABEL,
                    "process_with_hyper_view",
                    "hyperServices is null");
                return;
            }

            root.getHyperServices().process(activity, payload);
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
