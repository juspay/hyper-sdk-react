package in.juspay.hypersdkreact;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.react.bridge.ReactApplicationContext;

import java.util.HashSet;
import java.util.Set;

import in.juspay.hypersdk.ui.ActivityLaunchDelegate;

final class ReactLaunchDelegate implements ActivityLaunchDelegate {

    private final ReactApplicationContext context;
    private static final Set<Integer> intentRequestCodes = new HashSet<>();

    public ReactLaunchDelegate(ReactApplicationContext context) {
        this.context = context;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle bundle) {
        intentRequestCodes.add(requestCode);
        context.startActivityForResult(intent, requestCode, bundle);
    }

    static Set<Integer> getIntentRequestCodes() {
        return intentRequestCodes;
    }
}
