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
import java.util.HashMap;
import java.util.Map;

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

    /**
     * All the React methods in here should be synchronized on this specific object because there
     * was no guarantee that all React methods will be called on the same thread, and can cause
     * concurrency issues.
     */
    private static final Object lock = new Object();

    private static final HyperServicesHolder hyperServicesHolder = new HyperServicesHolder();

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
            HyperServices hyperServices = hyperServicesHolder.get();

            if (hyperServices == null) {
                return;
            }

            hyperServices.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

            HyperServices hyperServices = new HyperServices(activity);

            // FIXME: The next line remains to be commented until a fix is made in HyperSDK for this
            //        which can take time as this won't be the highest priority.
            //
            // In React plugin, we want to have the HyperServices instance in multi-activity mode.
            // Now, since we won't be needing this reference to activity until `initiate` is called
            // again, we have to call resetActivity() method. However, in current HyperSDK, this
            // call will cause a NullPointerException since fragment instance will be null until
            // initiate happens.
            //
            // Having this uncommented won't cause any issues, but if merchant changes activity
            // after calling `createHyperServices` a memory leak will occur until initiate is called.

            // hyperServices.resetActivity();

            hyperServicesHolder.set(hyperServices);
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean onBackPressed() {
        synchronized (lock) {
            HyperServices hyperServices = hyperServicesHolder.get();
            return hyperServices != null && hyperServices.onBackPressed();
        }
    }

    @ReactMethod
    public void initiate(String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();
                HyperServices hyperServices = hyperServicesHolder.get();

                hyperServices.initiate(activity, payload, new HyperPaymentsCallbackAdapter() {
                    @Override
                    public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                        // Prevent leaks, reset activity references once a result is returned
                        switch (data.optString("event", "")) {
                            case "initiate_result":
                            case "process_result":
                                hyperServices.resetActivity();
                        }

                        // Send out the event to the merchant on JS side
                        getReactApplicationContext()
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit(HYPER_EVENT, data.toString());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ReactMethod
    public void process(String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                hyperServicesHolder.get().process(activity, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @ReactMethod
    public void terminate() {
        synchronized (lock) {
            HyperServices hyperServices = hyperServicesHolder.get();

            if (hyperServices != null) {
                hyperServices.terminate();
            }
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isNull() {
        return hyperServicesHolder.get() == null;
    }

    @ReactMethod
    public void isInitialised(Promise promise) {
        boolean isInitialized = false;

        synchronized (lock) {
            HyperServices hyperServices = hyperServicesHolder.get();

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
            HyperServices hyperServices = hyperServicesHolder.get();

            if (hyperServices == null) {
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
    private static class HyperServicesHolder {
        @NonNull private WeakReference<HyperServices> hyperServices = new WeakReference<>(null);

        synchronized void set(@NonNull HyperServices hyperServices) {
            this.hyperServices = new WeakReference<>(hyperServices);
        }

        @Nullable
        synchronized HyperServices get() {
            return hyperServices.get();
        }
    }
}
