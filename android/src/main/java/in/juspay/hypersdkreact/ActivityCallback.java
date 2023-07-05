package in.juspay.hypersdkreact;

import androidx.fragment.app.FragmentActivity;

public interface ActivityCallback {
    void onCreated(FragmentActivity fragmentActivity);

    boolean onBackPressed();

    void resetActivity(FragmentActivity activity);
}
