package in.juspay.hypersdkreact;

import static in.juspay.hypersdkreact.LogConstants.SDK_TRACKER_LABEL;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import in.juspay.hyper.constants.LogLevel;
import in.juspay.hyper.constants.LogSubCategory;
import in.juspay.hypersdk.core.SdkTracker;


public class ProcessActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        try {
            JSONObject payload = new JSONObject(getIntent().getStringExtra(HyperSdkReactModule.PROCESS_PAYLOAD_ARG));
            HyperSdkReactModule.processWithActivity(this, payload, new HyperSdkReactModule.ProcessCallback() {
                @Override
                public void onResult() {
                    finish();
                }
            });
        } catch (JSONException e) {
            SdkTracker.trackAndLogBootException(
                    LogSubCategory.LifeCycle.HYPER_SDK,
                    LogLevel.ERROR,
                    SDK_TRACKER_LABEL,
                    "process",
                    "error while parsing string to JSON",
                    e);
        }
    }

    @Override
    public void onBackPressed() {
        boolean backPressHandled = HyperSdkReactModule.onBackPressed();
        if (!backPressHandled) {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        HyperSdkReactModule.resetActivity(this);
        super.onDestroy();
    }
}
