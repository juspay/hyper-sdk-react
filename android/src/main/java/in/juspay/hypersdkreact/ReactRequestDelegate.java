package in.juspay.hypersdkreact;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import android.util.Log;

import in.juspay.hypersdk.core.PaymentConstants;
import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.ui.RequestPermissionDelegate;

public class ReactRequestDelegate implements RequestPermissionDelegate {

    @NonNull private WeakReference<Activity> activity1 = new WeakReference<>(null);

    public ReactRequestDelegate(Activity activity) {
        this.activity1 = new WeakReference<>(activity);
    }

    @Override
    public void requestPermission(String[] permissions, int requestCode) {
        SdkTracker.trackBootLifecycle(
            PaymentConstants.SubCategory.LifeCycle.HYPER_SDK,
            PaymentConstants.LogLevel.INFO,
            HyperSdkReactModule.SDK_TRACKER_LABEL,
            "requestPermission",
            "requestPermission() called with: permissions = [" + Arrays.toString(permissions) + "], requestCode = [" + requestCode + "]");
        Activity activity = activity1.get();
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions, requestCode);
        }
    }
}
