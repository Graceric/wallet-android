package org.TonController.TonConnect.Features;

import org.TonController.TonConnect.Requests.Request;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonConnect.TonConnectController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

public class FeatureSendTransaction extends Feature {
    public final int maxTransactionsCount;

    public FeatureSendTransaction (int maxTransactionsCount) {
        this.maxTransactionsCount = maxTransactionsCount;
    }

    @Override
    public void fillFeaturesList (JSONArray features) {
        try {
            features.put("SendTransaction");
            features.put(new JSONObject()
                .put("name", "SendTransaction")
                .put("maxMessages", maxTransactionsCount));
        } catch (JSONException e) {
            FileLog.e(e);
        }
    }

    @Override
    public String getMethodName () {
        return "sendTransaction";
    }

    @Override
    public Request parseRequest (TonConnectController.ConnectedApplication application, TonConnectController.BridgeRequest request) throws Throwable {
        return RequestSendTransaction.valueOf(application, request);
    }
}
