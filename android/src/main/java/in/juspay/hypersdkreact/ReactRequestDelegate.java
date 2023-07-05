/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import in.juspay.hypersdk.core.SdkTracker;
import in.juspay.hypersdk.ui.RequestPermissionDelegate;

final class ReactRequestDelegate implements RequestPermissionDelegate {

    @NonNull
    private final WeakReference<Activity> activity;
    private static final Set<Integer> permissionRequestCodes = new HashSet<>();

    public ReactRequestDelegate(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    @Override
    public void requestPermission(String[] permissions, int requestCode) {
        SdkTracker.trackBootLifecycle(
                Constants.SUBCATEGORY_HYPER_SDK,
                Constants.LEVEL_INFO,
                Constants.SDK_TRACKER_LABEL,
                "requestPermission",
                "requestPermission() called with: permissions = [" + Arrays.toString(permissions) + "], requestCode = [" + requestCode + "]");
        Activity activity = this.activity.get();
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionRequestCodes.add(requestCode);
            activity.requestPermissions(permissions, requestCode);
        }
    }

    static Set<Integer> getPermissionRequestCodes() {
        return permissionRequestCodes;
    }
}
