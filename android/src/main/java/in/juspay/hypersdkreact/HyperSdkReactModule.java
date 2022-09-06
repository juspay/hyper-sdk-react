package in.juspay.hypersdkreact;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import in.juspay.hypersdk.core.PaymentConstants;
import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.services.HyperServices;

/**
 * Module that exposes Hyper SDK to React Native's JavaScript code. Merchants only need to deal with
 * the one static method {@link #onRequestPermissionsResult(int, String[], int[])} by calling it
 * when the React Activity gets a permissions result.
 */
public class HyperSdkReactModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final String HYPER_EVENT = "HyperEvent";
    protected static final String SDK_TRACKER_LABEL = "hyper_sdk_react";

    /**
     * All the React methods in here should be synchronized on this specific object because there
     * was no guarantee that all React methods will be called on the same thread, and can cause
     * concurrency issues.
     */
    private static final Object lock = new Object();

    private static final RequestPermissionsResultDelegate requestPermissionsResultDelegate = new RequestPermissionsResultDelegate();

    @Nullable
    private HyperServices hyperServices;

    HyperSdkReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
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

    @NonNull
    @Override
    public String getName() {
        return "HyperSdkReact";
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
                    PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                    PaymentConstants.LogLevel.ERROR,
                    SDK_TRACKER_LABEL,
                    "createHyperServices",
                    "activity is null");
                return;
            }

            hyperServices = new HyperServices(activity);
            hyperServices.setActivityLaunchDelegate(new ReactLaunchDelegate(getReactApplicationContext()));
            hyperServices.setRequestPermissionDelegate(new ReactRequestDelegate(activity));
            hyperServices.resetActivity();

            requestPermissionsResultDelegate.set(hyperServices);
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
                        PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                        PaymentConstants.LogLevel.ERROR,
                        SDK_TRACKER_LABEL,
                        "initiate",
                        "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    SdkTracker.trackBootLifecycle(
                        PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                        PaymentConstants.LogLevel.ERROR,
                        SDK_TRACKER_LABEL,
                        "initiate",
                        "hyperServices is null");
                    return;
                }

                hyperServices.initiate(activity, payload, new HyperPaymentsCallbackAdapter() {
                    @Override
                    public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                        // Send out the event to the merchant on JS side
                        try {
                            sendEventToJS(data);
                        } catch (InterruptedException e) {
                            SdkTracker.trackAndLogBootException(
                                PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                                PaymentConstants.LogLevel.ERROR,
                                SDK_TRACKER_LABEL,
                                "initiate",
                                "Interrupted while sending event to JS",
                                e
                            );
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEventToJS(JSONObject data) throws InterruptedException {
        DeviceEventManagerModule.RCTDeviceEventEmitter jsModule = getJSModule();
        if (jsModule == null) {
            Thread.sleep(200);
            sendEventToJS(data);
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
                        PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                        PaymentConstants.LogLevel.ERROR,
                        SDK_TRACKER_LABEL,
                        "initiate",
                        "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    SdkTracker.trackBootLifecycle(
                        PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                        PaymentConstants.LogLevel.ERROR,
                        SDK_TRACKER_LABEL,
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
                    PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                    PaymentConstants.LogLevel.ERROR,
                    SDK_TRACKER_LABEL,
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
                    PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                    PaymentConstants.LogLevel.ERROR,
                    SDK_TRACKER_LABEL,
                    "onRequestPermissionsResult",
                    "hyperServices is null");
                return;
            }

            SdkTracker.trackBootLifecycle(
                PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
                PaymentConstants.LogLevel.INFO,
                SDK_TRACKER_LABEL,
                "onRequestPermissionsResult",
                "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + Arrays.toString(permissions) + "], grantResults = [" + Arrays.toString(grantResults) + "]");

            hyperServices.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
