package org.Utils.network;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.Utils.Callbacks;

public class JsonFetch {
    public static void fetch (String url, @Nullable Callbacks.JsonCallback callback) {
        WalletConfigLoader.loadConfig(Utilities.fixUrl(url), s -> {
            if (callback == null) return;
            if (s == null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
                return;
            }
            try {
                JSONObject j = new JSONObject(s);
                AndroidUtilities.runOnUIThread(() -> callback.run(j));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            }
        });
    }
}
