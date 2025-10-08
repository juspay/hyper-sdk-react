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

import org.json.JSONException;
import org.json.JSONObject;

public class ProcessActivity extends AppCompatActivity {

    @Nullable
    private static ActivityCallback activityCallback;

    static void setActivityCallback(@Nullable ActivityCallback activityCallback) {
        ProcessActivity.activityCallback = activityCallback;
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
        if (activityCallback != null) {
            activityCallback.onCreated(this);
        }
    }


    @Override
    public void onBackPressed() {
        if (activityCallback != null && !activityCallback.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (activityCallback != null) {
            activityCallback.resetActivity(this);
        }
        super.onDestroy();
    }
}
