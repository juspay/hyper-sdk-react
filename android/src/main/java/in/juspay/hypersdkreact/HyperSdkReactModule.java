/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import in.juspay.hypercheckoutlite.HyperCheckoutLite;
import in.juspay.hypersdk.core.MerchantViewType;
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

    @Nullable
    private ReactInstanceManager reactInstanceManager;

    @Nullable
    private ReactApplication app;

    private final Boolean newArchEnabled = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;

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

    private static final Map<String, HyperServices> hyperServicesMap = new ConcurrentHashMap<>();

    /**
     * Per-instance merchant view registrations: instance key -> (view tag -> registered component name).
     */
    private final Map<String, Map<String, String>> keyedComponents = new ConcurrentHashMap<>();

    /**
     * Activities started by processWithActivity, keyed by the instance key (HYPER_EVENT for the
     * single-instance API).
     */
    private final Map<String, WeakReference<Activity>> processActivities = new ConcurrentHashMap<>();

    private static WeakReference<HyperServices> hyperServicesReference = new WeakReference<>(null);

    private final ReactApplicationContext context;

    private boolean useNewApprochForMerchantView = false;

    private Set<String> registeredComponents = new HashSet<>();

    HyperSdkReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
        useNewApprochForMerchantView = setUseNewApprochForMerchantView();
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
    @Keep
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
            put(MerchantViewConstants.JUSPAY_HEADER, MerchantViewConstants.JUSPAY_HEADER);
            put(MerchantViewConstants.JUSPAY_HEADER_ATTACHED, MerchantViewConstants.JUSPAY_HEADER_ATTACHED);
            put(MerchantViewConstants.JUSPAY_FOOTER, MerchantViewConstants.JUSPAY_FOOTER);
            put(MerchantViewConstants.JUSPAY_FOOTER_ATTACHED, MerchantViewConstants.JUSPAY_FOOTER_ATTACHED);
        }};
    }

    @Nullable
    public static HyperServices getHyperServices() {
        return hyperServicesReference.get();
    }

    @Nullable
    public static HyperServices getHyperServices(@Nullable String key) {
        return key == null ? null : hyperServicesMap.get(key);
    }

    @ReactMethod
    public void preFetch(String data) {
        try {
            JSONObject payload = new JSONObject(data);
            HyperServices.preFetch(getReactApplicationContext(), payload);
        } catch (JSONException e) {
            SdkTracker.trackAndLogBootException(
                    NAME,
                    LogConstants.CATEGORY_LIFECYCLE,
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.SDK_TRACKER_LABEL,
                    "Exception in prefetch",
                    e
            );
        }
    }

    @ReactMethod
    public void createHyperServices() {
        synchronized (lock) {
            if (hyperServices != null) {
                Log.w(NAME, "createHyperServices: hyperServices instance already exists");
                SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_WARN,
                        LogConstants.SDK_TRACKER_LABEL,
                        "createHyperServices",
                        "hyperServices instance already exists");
                return;
            }
            createHyperService(null, null);
        }
    }

    @ReactMethod
    public void createHyperServicesWithKey(String key, String tenantId, String clientId) {
        if (key == null || key.isEmpty() || HYPER_EVENT.equals(key) || hyperServicesMap.containsKey(key)) {
            String reason = (key == null || key.isEmpty())
                    ? "key is null or empty"
                    : "key is reserved or already in use";
            Log.w(NAME, "createHyperServicesWithKey skipped: " + reason);
            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_WARN,
                    LogConstants.SDK_TRACKER_LABEL,
                    "createHyperServicesWithKey",
                    reason);
            return;
        }
        if (tenantId == null || tenantId.isEmpty() || clientId == null || clientId.isEmpty()) {
            createHyperServiceWithKey(key, null, null);
        } else {
            createHyperServiceWithKey(key, tenantId, clientId);
        }
    }

    private void createHyperServiceWithKey(String key, @Nullable String tenantId, @Nullable String clientId){
        FragmentActivity activity = (FragmentActivity) getCurrentActivity();
        if (activity == null) {
            Log.w(NAME, "createHyperServiceWithKey: activity is null");
            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_ERROR,
                    LogConstants.SDK_TRACKER_LABEL,
                    "createHyperServiceWithKey",
                    "activity is null");
            return;
        }
        Application application = activity.getApplication();
        if (application instanceof ReactApplication) {
            this.app = ((ReactApplication) application);
        }
        HyperServices hyperServices;
        if (tenantId != null && clientId != null) {
            hyperServices = new HyperServices(activity, tenantId, clientId);
        } else {
            hyperServices = new HyperServices(activity);
        }

        hyperServicesMap.put(key, hyperServices);
    }

    @ReactMethod
    public void createHyperServicesWithTenantId(String tenantId, String clientId) {
        synchronized (lock) {
            if (hyperServices != null) {
                Log.w(NAME, "createHyperServicesWithTenantId: hyperServices instance already exists");
                SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_WARN,
                        LogConstants.SDK_TRACKER_LABEL,
                        "createHyperServicesWithTenantId",
                        "hyperServices instance already exists");
                return;
            }
            createHyperService(tenantId, clientId);
        }
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean onBackPressed() {
        return onBackPressed(hyperServices);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean onBackPressedWithKey(String key) {
        if (key == null) {
            Log.w(NAME, "onBackPressedWithKey skipped: key is null");
            return false;
        }

        return onBackPressed(hyperServicesMap.get(key));
    }

    private boolean onBackPressed(HyperServices hyperServices) {
        synchronized (lock) {
            return hyperServices != null && hyperServices.onBackPressed();
        }
    }

    @ReactMethod
    public void initiate(String data) {
        initiate(HYPER_EVENT, hyperServices, data);
    }

    @ReactMethod
    public void initiateWithKey(String data, String key) {
        if (key == null) {
            Log.w(NAME, "initiateWithKey skipped: key is null");
            return;
        }
        initiate(key, hyperServicesMap.get(key), data);
    }

    private void initiate(String key, HyperServices hyperServices, String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    Log.w(NAME, "initiate: activity is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                if (hyperServices == null) {
                    Log.w(NAME, "initiate: hyperServices is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "hyperServices is null");
                    return;
                }

                hyperServices.initiate(activity, payload, new HyperPaymentsCallbackAdapter() {
                    @Override
                    public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                        // Send out the event to the merchant on JS side
                        if (data.optString("event").equals("process_result")) {
                            WeakReference<Activity> processActivityRef = processActivities.remove(key);
                            if (processActivityRef != null) {
                                Activity processActivity = processActivityRef.get();
                                if (processActivity != null) {
                                    processActivity.finish();
                                    processActivity.overridePendingTransition(0, android.R.anim.fade_out);
                                }
                                ProcessActivity.setActivityCallback(key, null);
                            }
                        }
                        sendEventToJS(key, data);
                    }

                    private Object getReactHostOrInstanceManager() {
                        try {
                            Method getReactHostMethod = app.getClass().getMethod("getReactHost");
                            return getReactHostMethod.invoke(app);

                        } catch (Exception e) {
                            return null;
                        }
                    }


                    public View createReactSurfaceView(String viewName) {
                        try {
                            Class<?> reactSurfaceClass = Class.forName("com.facebook.react.interfaces.fabric.ReactSurface");
                            Object host = getReactHostOrInstanceManager();
                            if (host == null) {
                                return null;
                            }
                            Object surface = host.getClass().getMethod("createSurface", Context.class, String.class, Bundle.class).invoke(host, context, viewName, null);

                            reactSurfaceClass.getMethod("start").invoke(surface);
                            return (View) reactSurfaceClass.getMethod("getView").invoke(surface);

                        } catch (Exception e) {
                            return null;
                        }
                    }

                    private View createMerchantView(String viewName) {

                        if (!useNewApprochForMerchantView) {
                            if (reactInstanceManager == null) {
                                Application app = activity.getApplication();
                                if (app instanceof ReactApplication) {
                                    reactInstanceManager = ((ReactApplication) app).getReactNativeHost().getReactInstanceManager();
                                }
                            }
                            if (newArchEnabled) {
                                return createReactSurfaceView(viewName);
                            }
                            if (reactInstanceManager == null) {
                                return null;
                            }
                            ReactRootView reactRootView = new ReactRootView(activity);
                            reactRootView.startReactApplication(reactInstanceManager, viewName);
                            return reactRootView;
                        }
                        try {
                            // ReactHost is only present on RN >= 0.74, so resolve it reflectively to
                            // keep this class compiling against older React Native versions.
                            Object reactHost = getReactHostOrInstanceManager();
                            if (reactHost != null) {
                                Object surface = reactHost.getClass()
                                        .getMethod("createSurface", Context.class, String.class, Bundle.class)
                                        .invoke(reactHost, activity, viewName, null);
                                surface.getClass().getMethod("start").invoke(surface);

                                return (View) surface.getClass().getMethod("getView").invoke(surface);
                            }
                            return null;
                        } catch (Exception e) {
                            SdkTracker.trackAndLogBootException(
                                    NAME,
                                    LogConstants.CATEGORY_LIFECYCLE,
                                    LogConstants.SUBCATEGORY_HYPER_SDK,
                                    LogConstants.SDK_TRACKER_LABEL,
                                    "Exception in createMerchantView",
                                    e
                            );
                            return null;
                        }
                    }


                    @Nullable
                    @Override
                    public View getMerchantView(ViewGroup viewGroup, MerchantViewType merchantViewType) {
                        Activity activity = (Activity) getCurrentActivity();
                        if (activity == null) {
                            return super.getMerchantView(viewGroup, merchantViewType);
                        } else {
                            String componentName = null;
                            switch (merchantViewType) {
                                case HEADER:
                                    componentName = resolveComponent(key, MerchantViewConstants.JUSPAY_HEADER);
                                    break;
                                case FOOTER:
                                    componentName = resolveComponent(key, MerchantViewConstants.JUSPAY_FOOTER);
                                    break;
                                case FOOTER_ATTACHED:
                                    componentName = resolveComponent(key, MerchantViewConstants.JUSPAY_FOOTER_ATTACHED);
                                    break;
                                case HEADER_ATTACHED:
                                    componentName = resolveComponent(key, MerchantViewConstants.JUSPAY_HEADER_ATTACHED);
                                    break;
                            }
                            return componentName != null ? createMerchantView(componentName) : null;
                        }
                    }
                });
            } catch (Exception e) {
                SdkTracker.trackAndLogBootException(
                        NAME,
                        LogConstants.CATEGORY_LIFECYCLE,
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.SDK_TRACKER_LABEL,
                        "Exception in initiate",
                        e
                );
            }
        }
    }

    private Boolean setUseNewApprochForMerchantView() {
        String version = BuildConfig.REACT_NATIVE_VERSION;
        String[] parts = version.split("\\.");
        if (parts.length > 1) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > 0 || (major == 0 && minor >= 82);
        }
        return false;
    }

    private void createHyperService(@Nullable String tenantId, @Nullable String clientId) {
        FragmentActivity activity = (FragmentActivity) getCurrentActivity();
        if (activity == null) {
            Log.w(NAME, "createHyperServices: activity is null");
            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_ERROR,
                    LogConstants.SDK_TRACKER_LABEL,
                    "createHyperServices",
                    "activity is null");
            return;
        }
        Application app = activity.getApplication();
        if (app instanceof ReactApplication) {
            this.app = ((ReactApplication) app);
        }
        if (tenantId != null && clientId != null) {
            hyperServices = new HyperServices(activity, tenantId, clientId);
        } else {
            hyperServices = new HyperServices(activity);
        }
        hyperServicesReference = new WeakReference<>(hyperServices);

        requestPermissionsResultDelegate.set(hyperServices);
        activityResultDelegate.set(hyperServices);
    }

    private boolean isViewRegistered(String tag) {
        return registeredComponents.contains(tag);
    }

    /**
     * Resolves the component name to render for a merchant view slot. A component registered for
     * this specific instance via {@link #notifyAboutRegisterComponentWithKey} wins; otherwise the
     * process-wide registration made via {@link #notifyAboutRegisterComponent} is used.
     */
    @Nullable
    private String resolveComponent(String key, String tag) {
        Map<String, String> components = keyedComponents.get(key);
        if (components != null) {
            String componentName = components.get(tag);
            if (componentName != null) {
                return componentName;
            }
        }
        return isViewRegistered(tag) ? tag : null;
    }

    private void sendEventToJS(String key, JSONObject data) {
        DeviceEventManagerModule.RCTDeviceEventEmitter jsModule = getJSModule();
        if (jsModule == null) {
            Handler handler = new Handler();
            handler.postDelayed(() -> sendEventToJS(key, data), 200);
            return;
        }

        jsModule.emit(key, data.toString());
    }

    private DeviceEventManagerModule.RCTDeviceEventEmitter getJSModule() {
        return getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    }

    @ReactMethod
    public void process(String data) {
        process(hyperServices, data);
    }
    @ReactMethod
    public void processWithKey(String data, String key) {
        if (key == null) {
            Log.w(NAME, "processWithKey skipped: key is null");
            return;
        }
        process(hyperServicesMap.get(key), data);
    }

    private void process(HyperServices hyperService, String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    Log.w(NAME, "process: activity is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                if (hyperService == null) {
                    Log.w(NAME, "process: hyperService is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "hyperServices is null");
                    return;
                }
                hyperService.setActivityLaunchDelegate(new ReactLaunchDelegate(context));
                hyperService.setRequestPermissionDelegate(new ReactRequestDelegate(activity));

                hyperService.process(activity, payload);
            } catch (JSONException e) {
                SdkTracker.trackAndLogBootException(
                        NAME,
                        LogConstants.CATEGORY_LIFECYCLE,
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.SDK_TRACKER_LABEL,
                        "Exception in process",
                        e
                );
            }
        }
    }


    @ReactMethod
    public void processWithActivity(String data) {
        processWithActivity(HYPER_EVENT, hyperServices, data);
    }
    @ReactMethod
    public void processWithActivityWithKey(String data, String key) {
        if (key == null) {
            Log.w(NAME, "processWithActivityWithKey skipped: key is null");
            return;
        }
        HyperServices hyperService = hyperServicesMap.get(key);
        if (hyperService == null) {
            Log.w(NAME, "processWithActivityWithKey: hyperServices is null");
            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_ERROR,
                    LogConstants.SDK_TRACKER_LABEL,
                    "processWithActivityWithKey",
                    "hyperServices is null");
            return;
        }
        processWithActivity(key, hyperService, data);
    }


    private void processWithActivity(String key, HyperServices hyperService, String data) {
        synchronized (lock) {
            try {
                JSONObject payload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    Log.w(NAME, "processWithActivity: activity is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                Intent i = new Intent(activity, ProcessActivity.class);
                boolean statusBarLight = payload.optJSONObject("payload").optBoolean("statusBarLight", false);
                i.putExtra("statusBarLight", statusBarLight);
                i.putExtra(ProcessActivity.EXTRA_HYPER_KEY, key);
                ProcessActivity.setActivityCallback(key, new ActivityCallback() {
                    @Override
                    public void onCreated(@NonNull FragmentActivity fragmentActivity) {
                        if (hyperService == null) {
                            Log.w(NAME, "processWithActivity: hyperService is null");
                            SdkTracker.trackBootLifecycle(
                                    LogConstants.SUBCATEGORY_HYPER_SDK,
                                    LogConstants.LEVEL_ERROR,
                                    LogConstants.SDK_TRACKER_LABEL,
                                    "initiate",
                                    "hyperServices is null");
                            return;
                        }

                        processActivities.put(key, new WeakReference<>(fragmentActivity));
                        hyperService.process(fragmentActivity, payload);
                    }

                    @Override
                    public boolean onBackPressed() {
                        return HyperSdkReactModule.this.onBackPressed(hyperService);
                    }

                    @Override
                    public void resetActivity(@NonNull FragmentActivity activity) {
                        if (hyperService != null) {
                            hyperService.resetActivity(activity);
                        }
                    }
                });
                activity.startActivity(i);
            } catch (JSONException e) {
                SdkTracker.trackAndLogBootException(
                        NAME,
                        LogConstants.CATEGORY_LIFECYCLE,
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.SDK_TRACKER_LABEL,
                        "Exception in processWithActivity",
                        e
                );
            }
        }

    }

    @ReactMethod
    public void openPaymentPageWithKey(String data, String key){
        openPaymentPage(data, key != null ? key : HYPER_EVENT);
    }

    @ReactMethod
    public void openPaymentPage(String data) {
        openPaymentPage(data, HYPER_EVENT);
    }

    private void openPaymentPage(String data, String eventName) {
        synchronized (lock) {
            try {
                JSONObject sdkPayload = new JSONObject(data);
                FragmentActivity activity = (FragmentActivity) getCurrentActivity();

                if (activity == null) {
                    Log.w(NAME, "openPaymentPage: activity is null");
                    SdkTracker.trackBootLifecycle(
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.LEVEL_ERROR,
                            LogConstants.SDK_TRACKER_LABEL,
                            "initiate",
                            "activity is null");
                    return;
                }

                Intent i = new Intent(activity, ProcessActivity.class);
                boolean statusBarLight = sdkPayload.optJSONObject("payload").optBoolean("statusBarLight", false);
                i.putExtra("statusBarLight", statusBarLight);
                i.putExtra(ProcessActivity.EXTRA_HYPER_KEY, eventName);
                ProcessActivity.setActivityCallback(eventName, new ActivityCallback() {
                    @Override
                    public void onCreated(@NonNull FragmentActivity processActivity) {
                        HyperCheckoutLite.openPaymentPage(processActivity, sdkPayload, new HyperPaymentsCallbackAdapter() {
                            @Override
                            public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                                if (data.optString("event").equals("process_result")) {
                                    processActivity.finish();
                                    processActivity.overridePendingTransition(0, android.R.anim.fade_out);
                                    ProcessActivity.setActivityCallback(eventName, null);
                                }
                                sendEventToJS(eventName, data);
                            }
                        });
                    }

                    @Override
                    public boolean onBackPressed() {
                        return HyperCheckoutLite.onBackPressed();
                    }

                    @Override
                    public void resetActivity(@NonNull FragmentActivity activity) {
                        // Ignored
                    }
                });
                activity.startActivity(i);
            } catch (Exception e) {
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
            hyperServicesReference = new WeakReference<>(null);
        }
    }

    @ReactMethod
    public void terminateWithKey(String key) {
        if (key == null) {
            Log.w(NAME, "terminateWithKey skipped: key is null");
            return;
        }
        synchronized (lock) {
            HyperServices hyperService = hyperServicesMap.remove(key);
            if (hyperService != null) {
                hyperService.terminate();
            }
            keyedComponents.remove(key);
            processActivities.remove(key);
            ProcessActivity.setActivityCallback(key, null);
        }
    }

    @ReactMethod
    public void notifyAboutRegisterComponent(String tag) {
        registeredComponents.add(tag);
    }

    @ReactMethod
    public void notifyAboutRegisterComponentWithKey(String tag, String componentName, String key) {
        if (key == null || tag == null) {
            return;
        }
        if (componentName == null || componentName.isEmpty()) {
            componentName = tag;
        }
        Map<String, String> components = keyedComponents.get(key);
        if (components == null) {
            components = new ConcurrentHashMap<>();
            keyedComponents.put(key, components);
        }
        components.put(tag, componentName);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isNull() {
        return isNull(hyperServices);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isNullWithKey(String key) {
        if (key == null) {
            Log.w(NAME, "isNullWithKey: key is null");
            return true;
        }

        return isNull(hyperServicesMap.get(key));
    }

    private boolean isNull(HyperServices hyperServices) {
        return hyperServices == null;
    }

    @ReactMethod
    public void isInitialised(Promise promise) {
        isInitialised(hyperServices, promise);
    }

    @ReactMethod
    public void isInitialisedWithKey(String key, Promise promise) {
        if (key == null) {
            Log.w(NAME, "isInitialisedWithKey: key is null");
            promise.resolve(false);
            return;
        }

        HyperServices hyperService = hyperServicesMap.get(key);
        if (hyperService != null) {
            isInitialised(hyperService, promise);
        } else {
            Log.w(NAME, "isInitialisedWithKey: hyperServices is null");
            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_WARN,
                    LogConstants.SDK_TRACKER_LABEL,
                    "isInitialisedWithKey",
                    "hyperServices is null");
            promise.resolve(false);
        }
    }

    private void isInitialised(HyperServices hyperService, Promise promise) {
        boolean isInitialized = false;

        synchronized (lock) {
            if (hyperService != null) {
                try {
                    isInitialized = hyperService.isInitialised();
                } catch (Exception e) {
                    SdkTracker.trackAndLogBootException(
                            NAME,
                            LogConstants.CATEGORY_LIFECYCLE,
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.SDK_TRACKER_LABEL,
                            "Exception in isInitialised",
                            e
                    );
                }
            }
        }

        promise.resolve(isInitialized);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        synchronized (lock) {
            boolean delivered = false;
            if (hyperServices != null) {
                hyperServices.onActivityResult(requestCode, resultCode, data);
                delivered = true;
            }
            for (HyperServices hyperService : hyperServicesMap.values()) {
                hyperService.onActivityResult(requestCode, resultCode, data);
                delivered = true;
            }
            if (!delivered) {
                Log.w(NAME, "onActivityResult: hyperServices is null");
                SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_ERROR,
                        LogConstants.SDK_TRACKER_LABEL,
                        "onActivityResult",
                        "hyperServices is null");
            }
        }
    }

    @Override
    public void invalidate() {
        synchronized (lock) {
            for (HyperServices hyperService : hyperServicesMap.values()) {
                try {
                    hyperService.terminate();
                } catch (Exception e) {
                    SdkTracker.trackAndLogBootException(
                            NAME,
                            LogConstants.CATEGORY_LIFECYCLE,
                            LogConstants.SUBCATEGORY_HYPER_SDK,
                            LogConstants.SDK_TRACKER_LABEL,
                            "Exception while terminating hyperServices in invalidate",
                            e
                    );
                }
            }
            hyperServicesMap.clear();
            keyedComponents.clear();
            processActivities.clear();
            ProcessActivity.clearActivityCallbacks();
        }
        super.invalidate();
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

            if (hyperServices == null && hyperServicesMap.isEmpty()) {
                Log.w(NAME, "onRequestPermissionsResult: hyperServices is null");
                SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_ERROR,
                        LogConstants.SDK_TRACKER_LABEL,
                        "onRequestPermissionsResult",
                        "hyperServices is null");
                return;
            }

            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_INFO,
                    LogConstants.SDK_TRACKER_LABEL,
                    "onRequestPermissionsResult",
                    "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + Arrays.toString(permissions) + "], grantResults = [" + Arrays.toString(grantResults) + "]");

            if (hyperServices != null) {
                hyperServices.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            // The SDK routes results internally by requestCode, so every keyed instance can safely
            // be offered the result.
            for (HyperServices hyperService : hyperServicesMap.values()) {
                hyperService.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
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

            if (hyperServices == null && hyperServicesMap.isEmpty()) {
                Log.w(NAME, "onActivityResult: hyperServices is null");
                SdkTracker.trackBootLifecycle(
                        LogConstants.SUBCATEGORY_HYPER_SDK,
                        LogConstants.LEVEL_ERROR,
                        LogConstants.SDK_TRACKER_LABEL,
                        "onActivityResult",
                        "hyperServices is null");
                return;
            }

            SdkTracker.trackBootLifecycle(
                    LogConstants.SUBCATEGORY_HYPER_SDK,
                    LogConstants.LEVEL_INFO,
                    LogConstants.SDK_TRACKER_LABEL,
                    "onActivityResult",
                    "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]"
            );
            if (hyperServices != null) {
                hyperServices.onActivityResult(requestCode, resultCode, data);
            }
            // The SDK routes results internally by requestCode, so every keyed instance can safely
            // be offered the result.
            for (HyperServices hyperService : hyperServicesMap.values()) {
                hyperService.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
