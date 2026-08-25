/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessActivity extends AppCompatActivity {

    static final String EXTRA_HYPER_KEY = "hyperKey";
    private static final String DEFAULT_CALLBACK_KEY = "HyperEvent";

    /**
     * Callbacks keyed by the HyperServices instance key (HyperEvent for the single-instance API),
     * so concurrent flows from different instances do not clobber each other.
     */
    private static final Map<String, ActivityCallback> activityCallbacks = new ConcurrentHashMap<>();

    static void setActivityCallback(@Nullable ActivityCallback activityCallback) {
        setActivityCallback(DEFAULT_CALLBACK_KEY, activityCallback);
    }

    static void clearActivityCallbacks() {
        activityCallbacks.clear();
    }

    static void setActivityCallback(@Nullable String key, @Nullable ActivityCallback activityCallback) {
        if (key == null) {
            key = DEFAULT_CALLBACK_KEY;
        }
        if (activityCallback == null) {
            activityCallbacks.remove(key);
        } else {
            activityCallbacks.put(key, activityCallback);
        }
    }

    @Nullable
    private ActivityCallback getActivityCallback() {
        String key = getIntent().getStringExtra(EXTRA_HYPER_KEY);
        return activityCallbacks.get(key != null ? key : DEFAULT_CALLBACK_KEY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        boolean statusBarLight = getIntent().getBooleanExtra("statusBarLight", false);
        if (statusBarLight) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        ActivityCallback activityCallback = getActivityCallback();
        if (activityCallback != null) {
            activityCallback.onCreated(this);
        }
    }


    @Override
    public void onBackPressed() {
        ActivityCallback activityCallback = getActivityCallback();
        if (activityCallback != null && !activityCallback.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        ActivityCallback activityCallback = getActivityCallback();
        if (activityCallback != null) {
            activityCallback.resetActivity(this);
        }
        super.onDestroy();
    }
}
