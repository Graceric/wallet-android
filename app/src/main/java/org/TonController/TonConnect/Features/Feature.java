package org.TonController.TonConnect.Features;

import org.TonController.TonConnect.Requests.Request;
import org.TonController.TonConnect.TonConnectController;
import org.json.JSONArray;

public abstract class Feature {

    public abstract void fillFeaturesList (JSONArray features);

    public abstract String getMethodName ();

    public abstract Request parseRequest (TonConnectController.ConnectedApplication application, TonConnectController.BridgeRequest request) throws Throwable;

}
