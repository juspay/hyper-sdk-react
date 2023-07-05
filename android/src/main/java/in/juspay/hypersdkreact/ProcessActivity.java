package in.juspay.hypersdkreact;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

public class ProcessActivity extends AppCompatActivity {

    @Nullable
    private ActivityCallback activityCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityCallback = getIntent().getSerializableExtra(Constants.ACTIVITY_CALLBACK, ActivityCallback.class);
        } else {
            activityCallback = (ActivityCallback) getIntent().getSerializableExtra(Constants.ACTIVITY_CALLBACK);
        }

        activityCallback.onCreated(this);
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
