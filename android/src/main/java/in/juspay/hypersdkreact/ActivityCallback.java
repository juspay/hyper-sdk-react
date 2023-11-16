/*
 * Copyright (c) Juspay Technologies.
 *
 * This source code is licensed under the AGPL 3.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package in.juspay.hypersdkreact;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface ActivityCallback {
    void onCreated(@NonNull FragmentActivity fragmentActivity);

    boolean onBackPressed();

    void resetActivity(@NonNull FragmentActivity activity);
}
