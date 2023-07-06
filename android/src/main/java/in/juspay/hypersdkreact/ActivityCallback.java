/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import androidx.fragment.app.FragmentActivity;

public interface ActivityCallback {
    void onCreated(FragmentActivity fragmentActivity);

    boolean onBackPressed();

    void resetActivity(FragmentActivity activity);
}
