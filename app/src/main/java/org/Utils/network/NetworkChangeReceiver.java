package org.Utils.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.Utils.Callbacks;

public class NetworkChangeReceiver extends BroadcastReceiver {

    Callbacks.BooleanCallback onConnectionChangeDelegate;

    @Override
    public void onReceive(final Context context, final Intent intent) {

        int status = NetworkUtil.getConnectivityStatusString(context);
        Log.e("NETWORK", "Sulod sa network reciever");
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onConnectionChangeDelegate != null) {
                        onConnectionChangeDelegate.run(false);
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onConnectionChangeDelegate != null) {
                        onConnectionChangeDelegate.run(true);
                    }
                });
            }
        }
    }

    public void setOnConnectionChangeDelegate(Callbacks.BooleanCallback onConnectionChangeDelegate) {
        this.onConnectionChangeDelegate = onConnectionChangeDelegate;
    }
}