package in.juspay.hypersdkreact;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.services.HyperServices;

public class HyperSdkReactModule extends ReactContextBaseJavaModule {

  private static final String HYPER_EVENT = "HyperEvent";
  private static List<WeakReference<HyperServices>> hyperServiceReferences = new ArrayList<>();
  private ReactContext reactContext;
  private HyperServices hyperServices;
  private final ActivityEventListener activityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (hyperServices != null) {
        hyperServices.onActivityResult(requestCode, resultCode, data);
      }
    }
  };

  HyperSdkReactModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addActivityEventListener(activityEventListener);
  }

  public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    for (WeakReference<HyperServices> hyperServicesWeakReference : hyperServiceReferences) {
      HyperServices hyperServices = hyperServicesWeakReference.get();
      if (hyperServices != null) {
        hyperServices.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }
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
    Map<String, Object> constants = new HashMap<>();
    constants.put(HYPER_EVENT, HYPER_EVENT);
    return constants;
  }

  @ReactMethod
  public void preFetch(String data) {
    try {
      JSONObject payload = new JSONObject(data);
      HyperServices.preFetch(reactContext, payload);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void createHyperServices() {
    if (getCurrentActivity() != null) {
      FragmentActivity activity = (FragmentActivity) getCurrentActivity();
      hyperServices = new HyperServices(activity);
      hyperServiceReferences.add(new WeakReference<>(hyperServices));
    }
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public boolean onBackPressed() {
    return hyperServices.onBackPressed();
  }

  @ReactMethod
  public void initiate(String data) {
    try {
      JSONObject payload = new JSONObject(data);
      hyperServices.initiate(payload, new HyperPaymentsCallbackAdapter() {
        @Override
        public void onEvent(JSONObject data, JuspayResponseHandler handler) {
          reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(HYPER_EVENT, data.toString());
        }
      });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void process(String data) {
    try {
      JSONObject payload = new JSONObject(data);
      hyperServices.process(payload);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void terminate() {
    hyperServices.terminate();
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public boolean isNull() {
    return hyperServices == null;
  }

  @ReactMethod
  public void isInitialised(Promise promise) {
    promise.resolve(hyperServices.isInitialised());
  }
}
