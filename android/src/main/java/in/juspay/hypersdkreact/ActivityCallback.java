package in.juspay.hypersdkreact;

import androidx.fragment.app.FragmentActivity;

import java.io.Serializable;

public interface ActivityCallback extends Serializable {
    void onCreated(FragmentActivity fragmentActivity);

    boolean onBackPressed();

    void resetActivity(FragmentActivity activity);
}
