/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.services.HyperServices;


/**
 * Module that exposes Hyper SDK to React Native's JavaScript code. Merchants only need to deal with
 * the one static method {@link #onRequestPermissionsResult(int, String[], int[])} by calling it
 * when the React Activity gets a permissions result.
 */
@ReactModule(name = HyperSdkReactModule.NAME)
public class HyperSdkReactModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static final String NAME = "HyperSdkReact";
    private static final String HYPER_EVENT = "HyperEvent";

    /**
     * All the React methods in here should be synchronized on this specific object because there
     * was no guarantee that all React methods will be called on the same thread, and can cause
     * concurrency issues.
     */
    private static final Object lock = new Object();

    private static final RequestPermissionsResultDelegate requestPermissionsResultDelegate = new RequestPermissionsResultDelegate();
    private static final ActivityResultDelegate activityResultDelegate = new ActivityResultDelegate();

    @Nullable
    private HyperServices hyperServices;

    private final ReactApplicationContext context;

    private boolean wasProcessWithActivity = false;

    @NonNull
    private WeakReference<Activity> processActivityRef = new WeakReference<>(null);

    HyperSdkReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Set up any upstream listeners or background tasks as necessary
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Remove upstream listeners, stop unnecessary background tasks
    }


    /**
     * Notifies HyperSDK that a response for permissions is here. Merchants are required to call
     * this method from their activity as by default {@link com.facebook.react.ReactActivity} will
     * not forward any results to the fragments running inside it.
     *
     * @param requestCode  The requestCode that was received in your activity's
     *                     {@code onRequestPermissionsResult} method.
     * @param permissions  The set of permissions received in your activity's
     *                     {@code onRequestPermissionsResult} method.
     * @param grantResults The results of each permission received in your activity's
     *                     {@code onRequestPermissionsResult} method.
     */
    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        synchronized (lock) {
            requestPermissionsResultDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Used to forward the intent result to HyperSDK,
     * for the cases when it is not forwarded by React Native implicitly.
     *
     * @param requestCode The requestCode that was received in your activity's
     *                    {@code onActivityResult} method.
     * @param resultCode  The requestCode that was received in your activity's
     *                    {@code onActivityResult} method.
     * @param data        The intent data that was received in your activity's
     *                    {@code onActivityResult} method.
     */
    @Keep
    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        synchronized (lock) {
            activityResultDelegate.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Used to get the list of requestCodes for which intent was started by HyperSDK.
     *
     * @return {@link Set set} of {@link Integer} requestCodes.
     */
    public static Set<Integer> getIntentRequestCodes() {
        return ReactLaunchDelegate.getIntentRequestCodes();
    }

    /**
     * Used to get the list of requestCodes for which permission was requested by HyperSDK.
     *
     * @return {@link Set set} of {@link Integer} requestCodes.
     */
    public static Set<Integer> getPermissionRequestCodes() {
        return ReactRequestDelegate.getPermissionRequestCodes();
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return new HashMap<String, Object>() {{
            put(HYPER_EVENT, HYPER_EVENT);
        }};
    }

    @ReactMethod
    public void preFetch(String data) {
        try {
            JSONObject payload = new JSONObject(data);
            HyperServices.preFetch(getReactApplicationContext(), payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void createHyperServices() {
        synchronized (lock) {
            FragmentActivity activity = (FragmentActivity) getCurrentActivity();

            if (activity == null) {
                SdkTracker.trackBootLifecycle(
                        Constants.SUBCATEGORY_HYPER_SDK,
                        Constants.LEVEL_ERROR,
                        Constants.SDK_TRACKER_LABEL,
                        "createHyperServices",
                        "activity is null");
                return;
            }

            hyperServices = new HyperServices(activity);
            hyperServices.setActivityLaunchDelegate(new ReactLaunchDelegate(context));
            hyperServices.setRequestPermissionDelegate(new ReactRequestDelegate(activity));

            requestPermissionsResultDelegate.set(hyperServices);
            activityResultDelegate.set(hyperServices);
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean onBackPressed() {
        synchronized (lock) {
            return hyperServices != null && hyperServices.onBackPressed();
        }
    }

    @ReactMethod
    public void initiate(String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    SdkTracker.trackBootLifecycle(
                            Constants.SUBCATEGORY_HYPER_SDK,
                            Constants.LEVEL_ERROR,
                            Constants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    SdkTracker.trackBootLifecycle(
                            Constants.SUBCATEGORY_HYPER_SDK,
                            Constants.LEVEL_ERROR,
                            Constants.SDK_TRACKER_LABEL,
                            "initiate",
                            "hyperServices is null");
                    return;
                }

                hyperServices.initiate(activity, payload, new HyperPaymentsCallbackAdapter() {
                    @Override
                    public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                        // Send out the event to the merchant on JS side
                        if (data.optString("event").equals("process_result") && wasProcessWithActivity) {
                            Activity processActivity = processActivityRef.get();
                            if (processActivity != null) {
                                processActivity.finish();
                            }
                            wasProcessWithActivity = false;
                        }
                        sendEventToJS(data);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEventToJS(JSONObject data) {
        DeviceEventManagerModule.RCTDeviceEventEmitter jsModule = getJSModule();
        if (jsModule == null) {
            Handler handler = new Handler();
            handler.postDelayed(() -> sendEventToJS(data), 200);
            return;
        }

        jsModule.emit(HYPER_EVENT, data.toString());
    }

    private DeviceEventManagerModule.RCTDeviceEventEmitter getJSModule() {
        return getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    }

    @ReactMethod
    public void process(String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    SdkTracker.trackBootLifecycle(
                            Constants.SUBCATEGORY_HYPER_SDK,
                            Constants.LEVEL_ERROR,
                            Constants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    SdkTracker.trackBootLifecycle(
                            Constants.SUBCATEGORY_HYPER_SDK,
                            Constants.LEVEL_ERROR,
                            Constants.SDK_TRACKER_LABEL,
                            "initiate",
                            "hyperServices is null");
                    return;
                }

                hyperServices.process(activity, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @ReactMethod
    public void processWithActivity(String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    SdkTracker.trackBootLifecycle(
                            Constants.SUBCATEGORY_HYPER_SDK,
                            Constants.LEVEL_ERROR,
                            Constants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                Intent i = new Intent(activity, ProcessActivity.class);
                i.putExtra(Constants.ACTIVITY_CALLBACK, new ActivityCallback() {
                    @Override
                    public void onCreated(FragmentActivity fragmentActivity) {
                        if (hyperServices == null) {
                            SdkTracker.trackBootLifecycle(
                                    Constants.SUBCATEGORY_HYPER_SDK,
                                    Constants.LEVEL_ERROR,
                                    Constants.SDK_TRACKER_LABEL,
                                    "initiate",
                                    "hyperServices is null");
                            return;
                        }

                        wasProcessWithActivity = true;
                        processActivityRef = new WeakReference<>(fragmentActivity);
                        hyperServices.process(fragmentActivity, payload);
                    }

                    @Override
                    public boolean onBackPressed() {
                        return HyperSdkReactModule.this.onBackPressed();
                    }

                    @Override
                    public void resetActivity(FragmentActivity activity) {
                        if (hyperServices != null) {
                            hyperServices.resetActivity(activity);
                        }
                    }
                });
                activity.startActivity(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @ReactMethod
    public void terminate() {
        synchronized (lock) {
            if (hyperServices != null) {
                hyperServices.terminate();
            }

            hyperServices = null;
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isNull() {
        return hyperServices == null;
    }

    @ReactMethod
    public void isInitialised(Promise promise) {
        boolean isInitialized = false;

        synchronized (lock) {
            if (hyperServices != null) {
                try {
                    isInitialized = hyperServices.isInitialised();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        promise.resolve(isInitialized);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        synchronized (lock) {
            if (hyperServices == null) {
                SdkTracker.trackBootLifecycle(
                        Constants.SUBCATEGORY_HYPER_SDK,
                        Constants.LEVEL_ERROR,
                        Constants.SDK_TRACKER_LABEL,
                        "onActivityResult",
                        "hyperServices is null");
                return;
            }

            hyperServices.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    /**
     * A holder class that allows us to maintain HyperServices instance statically without causing a
     * memory leak. This was required because HyperServices class maintains a reference to the
     * activity internally.
     */
    private static class RequestPermissionsResultDelegate {
        @NonNull
        private WeakReference<HyperServices> hyperServicesHolder = new WeakReference<>(null);

        synchronized void set(@NonNull HyperServices hyperServices) {
            this.hyperServicesHolder = new WeakReference<>(hyperServices);
        }

        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            HyperServices hyperServices = hyperServicesHolder.get();

            if (hyperServices == null) {
                SdkTracker.trackBootLifecycle(
                        Constants.SUBCATEGORY_HYPER_SDK,
                        Constants.LEVEL_ERROR,
                        Constants.SDK_TRACKER_LABEL,
                        "onRequestPermissionsResult",
                        "hyperServices is null");
                return;
            }

            SdkTracker.trackBootLifecycle(
                    Constants.SUBCATEGORY_HYPER_SDK,
                    Constants.LEVEL_INFO,
                    Constants.SDK_TRACKER_LABEL,
                    "onRequestPermissionsResult",
                    "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + Arrays.toString(permissions) + "], grantResults = [" + Arrays.toString(grantResults) + "]");

            hyperServices.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static class ActivityResultDelegate {
        @NonNull
        private WeakReference<HyperServices> hyperServicesHolder = new WeakReference<>(null);

        synchronized void set(@NonNull HyperServices hyperServices) {
            this.hyperServicesHolder = new WeakReference<>(hyperServices);
        }

        void onActivityResult(int requestCode, int resultCode, Intent data) {
            HyperServices hyperServices = hyperServicesHolder.get();

            if (hyperServices == null) {
                SdkTracker.trackBootLifecycle(
                        Constants.SUBCATEGORY_HYPER_SDK,
                        Constants.LEVEL_ERROR,
                        Constants.SDK_TRACKER_LABEL,
                        "onActivityResult",
                        "hyperServices is null");
                return;
            }

            SdkTracker.trackBootLifecycle(
                    Constants.SUBCATEGORY_HYPER_SDK,
                    Constants.LEVEL_INFO,
                    Constants.SDK_TRACKER_LABEL,
                    "onActivityResult",
                    "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]"
            );
            hyperServices.onActivityResult(requestCode, resultCode, data);
        }
    }
}
