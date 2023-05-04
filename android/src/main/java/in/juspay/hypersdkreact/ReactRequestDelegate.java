package in.juspay.hypersdkreact;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.ui.RequestPermissionDelegate;

final class ReactRequestDelegate implements RequestPermissionDelegate {

    @NonNull
    private final WeakReference<Activity> activity;

    public ReactRequestDelegate(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    @Override
    public void requestPermission(String[] permissions, int requestCode) {
        SdkTracker.trackBootLifecycle(
                LogConstants.SUBCATEGORY_HYPER_SDK,
                LogConstants.LEVEL_INFO,
                LogConstants.SDK_TRACKER_LABEL,
                "requestPermission",
                "requestPermission() called with: permissions = [" + Arrays.toString(permissions) + "], requestCode = [" + requestCode + "]");
        Activity activity = this.activity.get();
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permissions, requestCode);
        }
    }
}
