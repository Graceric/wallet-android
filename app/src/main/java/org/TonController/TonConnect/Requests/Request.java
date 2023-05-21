package org.TonController.TonConnect.Requests;

import org.TonController.TonConnect.TonConnectController;

public abstract class Request {
    public final TonConnectController.ConnectedApplication application;
    public final TonConnectController.BridgeEvent event;
    public final String[] params;
    public final String method;
    public final String id;

    protected Request (TonConnectController.ConnectedApplication application, TonConnectController.BridgeRequest request) {
        this.application = application;
        this.event = request.event;
        this.params = request.params;
        this.method = request.method;
        this.id = request.id;
    }
}
