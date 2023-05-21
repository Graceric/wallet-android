package org.TonController.TonConnect.Connection;

import android.util.Log;
import android.util.Pair;

import org.TonController.TonConnect.TonConnectController;

import java.util.HashMap;
import java.util.HashSet;

public class TonConnectListenerManager {
    private final HashMap<String, Pair<TonConnectGateway, HashMap<String, HashSet<String>>>> connections;
    private final TonConnectGateway.Callback newMessageCallback;

    public TonConnectListenerManager (TonConnectGateway.Callback newMessageCallback) {
        this.newMessageCallback = newMessageCallback;
        this.connections = new HashMap<>();
    }


    public void startConnection (TonConnectController.ConnectedApplication application, String myClientId) {
        Pair<TonConnectGateway, HashMap<String, HashSet<String>>> bridgeConnections = connections.get(application.bridgeUrl);
        if (bridgeConnections == null) {
            bridgeConnections = new Pair<>(new TonConnectGateway(application.bridgeUrl, myClientId, newMessageCallback), new HashMap<>());
            connections.put(application.bridgeUrl, bridgeConnections);
        }

        TonConnectGateway listener = bridgeConnections.first;
        if (!listener.isStarted()) {
            listener.start();
        }

        HashMap<String, HashSet<String>> connections = bridgeConnections.second;
        HashSet<String> connectedApps = connections.get(myClientId);
        if (connectedApps == null) {
            connectedApps = new HashSet<>();
            connections.put(myClientId, connectedApps);
        }

        connectedApps.add(application.clientId);

        Log.i("TONCONNECT", "START CONNECTION");
    }

    public void stopConnection (TonConnectController.ConnectedApplication application, String myClientId) {
        Pair<TonConnectGateway, HashMap<String, HashSet<String>>> bridgeConnections = connections.get(application.bridgeUrl);
        if (bridgeConnections == null) return;

        TonConnectGateway listener = bridgeConnections.first;
        HashMap<String, HashSet<String>> connections = bridgeConnections.second;
        HashSet<String> connectedApps = connections.get(myClientId);
        if (connectedApps == null) return;

        connectedApps.remove(application.clientId);
        if (connectedApps.isEmpty()) {
            connections.remove(myClientId);
        }

        if (connections.isEmpty()) {
            if (listener.isStarted()) {
                listener.stop();
            }
        }

        Log.i("TONCONNECT", "STOP CONNECTION");
    }
}
