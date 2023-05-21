package org.TonController.TonConnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.iwebpp.crypto.TweetNaclFast;

import org.TonController.TonConnect.Connection.TonConnectGateway;
import org.TonController.TonConnect.Connection.TonConnectListenerManager;
import org.TonController.TonConnect.Connection.TonConnectSender;
import org.TonController.TonConnect.Features.Feature;
import org.TonController.TonConnect.Features.FeatureSendTransaction;
import org.TonController.TonConnect.Requests.Request;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;
import org.ton.java.utils.Utils;
import org.Utils.Callbacks;
import org.TonController.Parsers.UriParser;
import org.Utils.network.JsonFetch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TonConnectController {
    private final TonController controller;
    private final byte[] secretKey;
    private final String myClientId;

    private final TonConnectListenerManager listenerManager;
    private final ConnectedApplicationStore store;
    private final HashMap<String, InitialRequest> initialRequestsMap = new HashMap<>();
    private final HashMap<String, ConnectedApplication> initialApplications = new HashMap<>();

    public final Feature[] features;
    public final HashMap<String, Feature> featureHashMap;

    private TonConnectRequestWithAppCallback requestCallback;

    public TonConnectController (TonController controller, byte[] secretKey, byte[] publicKey) {
        this.controller = controller;
        this.featureHashMap = new HashMap<>();
        this.features = new Feature[] {
            new FeatureSendTransaction(4)
        };

        for (Feature feature: features) {
            featureHashMap.put(feature.getMethodName(), feature);
        }

        this.store = new ConnectedApplicationStore();
        this.secretKey = secretKey;
        this.myClientId = Utils.bytesToHex(publicKey).toLowerCase();
        this.listenerManager = new TonConnectListenerManager(this::onNewBridgeEncryptedMessage);

        for (String clientId: store.connectedClientsId) {
            ConnectedApplication app = store.get(clientId);
            if (app == null) continue;
            listenerManager.startConnection(app, myClientId);
        }
    }

    public void initNewConnect (UriParser.ResultTonConnectLink request, ApplicationCallback callback, Callbacks.ErrorCallback errorCallback) {
        if (request.version != 2) return ;          // todo: send 1 error code
        try {
            InitialRequest initialRequest = InitialRequest.valueOf(request);
            initialRequest.loadManifest(() -> {
                ParsedApplicationManifest manifest = initialRequest.getManifest();
                if (manifest == null) return;       // todo: send 2 or 3 error code

                ConnectedApplication application = new ConnectedApplication(request.bridgeUrl, initialRequest.clientId, initialRequest.manifestUrl, manifest, 0);
                initialApplications.put(initialRequest.clientId, application);
                AndroidUtilities.runOnUIThread(() -> callback.run(application));
            });

            initialRequestsMap.put(initialRequest.clientId, initialRequest);
        } catch (Throwable e) {
            // return;                                 // todo: send 0, 1 or 2 error code;
        }
    }

    public InitialResponse makeNewInitialResponse (String clientId) {
        InitialRequest request = initialRequestsMap.get(clientId);
        if (request == null) throw new RuntimeException();

        return new InitialResponse(request, controller, features);
    }

    public void finishInitNewConnect (InitialResponse response, Runnable onFinishCallback, Callbacks.ErrorCallback onErrorCallback) {
        String clientId = response.request.clientId;
        initialRequestsMap.remove(clientId);
        ConnectedApplication app = initialApplications.remove(clientId);
        if (app == null) return;

        store.save(app);
        AndroidUtilities.runOnUIThread(() -> listenerManager.startConnection(app, myClientId));
        try {
            sendEventToApplication(app, "connect", response.getEventPayload(), r -> onFinishCallback.run());
        } catch (JSONException e) {
            FileLog.e(e);
        }
    }

    public void setRequestCallback (TonConnectRequestWithAppCallback requestCallback) {
        this.requestCallback = requestCallback;
    }




    /* Send to dApp functions */

    public void disconnect (ConnectedApplication application, Callbacks.JsonCallback callback) {
        sendEventToApplication(application, "disconnect", new JSONObject(), callback);
        disconnectImpl(application);
    }

    private void disconnectImpl (ConnectedApplication application) {
        store.delete(application.clientId);
        listenerManager.stopConnection(application, myClientId);
    }

    public void sendEventToApplication (ConnectedApplication application, String eventName, JSONObject payload, Callbacks.JsonCallback callback) {
        try {
            JSONObject event = new JSONObject()
                .put("event", eventName)
                .put("id", System.currentTimeMillis())
                .put("payload", payload);
            TonConnectSender.send(application.bridgeUrl, secretKey, myClientId, application.clientId, event, callback);
        } catch (JSONException e) {
            FileLog.e(e);
        }
    }

    public void sendResponseToApplication (Request request, Object response, Callbacks.JsonCallback callback) {
        sendResponseToApplication(request.application, request.id, response, callback);
    }

    public void sendResponseToApplication (ConnectedApplication application, String requestId, Object response, Callbacks.JsonCallback callback) {
        try {
            JSONObject res = new JSONObject().put("id", requestId).put("result", response);
            TonConnectSender.send(application.bridgeUrl, secretKey, myClientId, application.clientId, res, callback);
        } catch (JSONException e) {
            FileLog.e(e);
        }
    }

    public void sendErrorToApplication (Request request, int code, String message, Callbacks.JsonCallback callback) {
        sendErrorToApplication(request.application.bridgeUrl, request.application.clientId, request.id, code, message, callback);
    }

    public void sendErrorToApplication (String bridgeUrl, String toClientId, String requestId, int code, String message, Callbacks.JsonCallback callback) {
        try {
            JSONObject res = new JSONObject()
                    .put("id", requestId)
                    .put("error", new JSONObject()
                            .put("code", code)
                            .put("message", message)
                    );
            TonConnectSender.send(bridgeUrl, secretKey, myClientId, toClientId, res, callback);
        } catch (JSONException e) {
            FileLog.e(e);
        }
    }



    /*  */

    private void onNewBridgeEncryptedMessage (TonConnectGateway.BridgeEncryptedMessage message) {
        try {
            TweetNaclFast.Box box = new TweetNaclFast.Box(Utils.hexToBytes(message.fromClientId), secretKey);
            byte[] decrypted = box.open(message.encryptedMessage, message.nonce);
            JSONObject decryptedMessage = new JSONObject(new String(decrypted));
            onNewBridgeMessage(new BridgeEvent(message.bridgeUrl, message.eventId, message.fromClientId, myClientId, decryptedMessage));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void onNewBridgeMessage (BridgeEvent event) {
        Log.i("TONCONNECT", event.jsonObject.toString());
        BridgeRequest request = null;
        try {
            ConnectedApplication application = store.get(event.fromClientId);
            request = BridgeRequest.valueOf(event);

            if (application == null) {
                sendErrorToApplication(event.bridgeUrl, event.fromClientId, request.id, 100, "Unknown app", null);
                return;
            }
            if (application.lastEventId >= event.eventId) return;   // ignore
            application.lastEventId = event.eventId;
            store.save(application);

            if (request.method.equals("disconnect")) {
                sendResponseToApplication(application, request.id, new JSONObject(), null);
                disconnectImpl(application);
                return;
            }

            Feature feature = featureHashMap.get(request.method);
            if (feature == null) {
                sendErrorToApplication(event.bridgeUrl, event.fromClientId, request.id, 400, "Method not supported", null);
                return;
            }

            Request parsedRequest = feature.parseRequest(application, request);
            if (filterRequest(parsedRequest)) {
                AndroidUtilities.runOnUIThread(() -> requestCallback.run(parsedRequest));
            }
        } catch (Throwable e) {
            FileLog.e(e);
            if (request != null) {
                sendErrorToApplication(event.bridgeUrl, event.fromClientId, request.id, 0, "Unknown error", null);
            }
        }
    }

    private boolean filterRequest (Request request) {
        if (request instanceof RequestSendTransaction) {
            RequestSendTransaction sendTransaction = (RequestSendTransaction) request;
            /*if (TextUtils.isEmpty(sendTransaction.network)) {         ???
                sendErrorToApplication(request.event.bridgeUrl, request.event.fromClientId, request.id, 1, "Network id missing", null);
                return false;
            }*/
            if (!TextUtils.isEmpty(sendTransaction.network) && !TextUtils.equals(sendTransaction.network, controller.getNetworkId())) {
                sendErrorToApplication(request.event.bridgeUrl, request.event.fromClientId, request.id, 1, "Wrong network id", null);
                return false;
            }
        }

        return true;
    }



    public static class BridgeRequest {
        public final BridgeEvent event;
        public final String id;
        public final String method;
        public final String[] params;

        private BridgeRequest (BridgeEvent event, String id, String method, String[] params) {
            this.event = event;
            this.id = id;
            this.method = method;
            this.params = params;
        }

        private static BridgeRequest valueOf (BridgeEvent event) throws Throwable {
            JSONArray params = event.jsonObject.getJSONArray("params");
            String[] paramsArr = new String[params.length()];
            for (int a = 0; a < params.length(); a++) {
                paramsArr[a] = params.getString(a);
            }

            return new BridgeRequest(event,
                    event.jsonObject.getString("id"),
                    event.jsonObject.getString("method"),
                    paramsArr
            );
        }
    }

    public static class BridgeEvent {
        public final String bridgeUrl;
        public final long eventId;
        public final String fromClientId;
        public final String toClientId;
        public final JSONObject jsonObject;

        public BridgeEvent(String bridgeUrl, long eventId, String fromClientId, String toClientId, JSONObject jsonObject) {
            this.bridgeUrl = bridgeUrl;
            this.eventId = eventId;
            this.fromClientId = fromClientId;
            this.toClientId = toClientId;
            this.jsonObject = jsonObject;
        }
    }



    public ArrayList<String> getConnectedClientsId () {
        return new ArrayList<>(store.connectedClientsId);
    }

    public ConnectedApplication getConnectedApplication (String clientId) {
        return store.get(clientId);
    }

    public void cleanup () {
        Set<String> clients = new HashSet<>(store.connectedClientsId);
        for (String clientId: clients) {
            ConnectedApplication app = store.get(clientId);
            if (app == null) continue;
            disconnectImpl(app);
        }
        initialRequestsMap.clear();
        initialApplications.clear();
        store.clear();
    }



    /* * */

    public static class InitialResponse {
        private final Feature[] features;
        public final InitialRequest request;
        public final String walletAddress;
        public final byte[] walletPublicKey;
        public final byte[] walletInitState;
        public final String networkId;
        public final long timestamp;
        public final @Nullable byte[] walletProofHash;
        public final byte[] appDomain;
        private byte[] walletProofSignature;

        private InitialResponse (InitialRequest request, TonController tonController, Feature[] features) {
            this.request = request;
            this.features = features;
            this.walletAddress = tonController.getCurrentWalletAddress();
            this.walletPublicKey = tonController.getPublicKey();
            this.walletInitState = tonController.getWalletInitAccountStateRaw();
            this.networkId = tonController.getNetworkId();
            this.timestamp = System.currentTimeMillis() / 1000;
            this.appDomain = request.manifest.getHost().getBytes(StandardCharsets.UTF_8);
            this.walletProofHash = makeProof(request.proofPayload);
        }

        private @Nullable byte[] makeProof (String payload) {
            if (payload == null) return null;
            try {
                Address address = new Address(walletAddress);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8); // payload.getBytes(StandardCharsets.UTF_8);

                BitString timestamp = new BitString(64);
                timestamp.writeUint(this.timestamp, 64);

                BitString domainByteLength = new BitString(32);
                domainByteLength.writeUint(appDomain.length, 32);

                BitString message = new BitString((18 + 4 + 32 + 4 + 8 + appDomain.length + payloadBytes.length) * 8);
                message.writeBytes("ton-proof-item-v2/".getBytes(StandardCharsets.UTF_8));
                message.writeUint(address.wc, 32);
                message.writeBytes(address.hashPart);
                message.writeBytes(Utilities.reverse(domainByteLength.toByteArray()));
                message.writeBytes(appDomain);
                message.writeBytes(Utilities.reverse(timestamp.toByteArray()));
                message.writeBytes(payloadBytes);
                byte[] messageHash = Utils.hexToBytes(Utils.sha256(message.toByteArray()));

                BitString messageFull = new BitString((2 + 11 + 32) * 8);
                messageFull.writeUint(0xFFFF, 16);
                messageFull.writeBytes("ton-connect".getBytes(StandardCharsets.UTF_8));
                messageFull.writeBytes(messageHash);
                return Utils.hexToBytes(Utils.sha256(messageFull.toByteArray()));
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }

        public void setWalletProofSignature (byte[] walletProofSignature) {
            this.walletProofSignature = walletProofSignature;
        }

        public JSONObject getEventPayload() throws JSONException {
            Feature[] featuresList = this.features;
            Address address = new Address(walletAddress);

            JSONArray features = new JSONArray();
            for (Feature feature: featuresList) {
                feature.fillFeaturesList(features);
            }

            JSONObject deviceInfo = new JSONObject()
                .put("platform", "android")
                .put("appName", "Tonkeeper")
                .put("appVersion", "3.0.305")
                .put("maxProtocolVersion", 2)
                .put("features", features);

            JSONArray items = new JSONArray();

            if (request.needReturnAddress) {
                JSONObject addressItem = new JSONObject()
                    .put("name", "ton_addr")
                    .put("network", networkId)
                    .put("address", address.toString(false))
                    .put("publicKey", Utils.bytesToHex(walletPublicKey))
                    .put("walletStateInit", Base64.encodeToString(walletInitState, Base64.NO_WRAP));
                items.put(addressItem);
            }

            if (request.proofPayload != null) {
                if (walletProofSignature != null) {
                    JSONObject proofItem = new JSONObject()
                        .put("name", "ton_proof")
                        .put("proof", new JSONObject()
                            .put("timestamp", timestamp)
                            .put("signature", Base64.encodeToString(walletProofSignature, Base64.NO_WRAP))
                            .put("payload", request.proofPayload)
                            .put("domain", new JSONObject()
                                .put("lengthBytes", appDomain.length)
                                .put("value", new String(appDomain))
                            ));
                    items.put(proofItem);
                }
            }

            return new JSONObject()
                .put("items", items)
                .put("device", deviceInfo);
        }
    }

    public static class InitialRequest {
        public final String clientId;
        public final String manifestUrl;
        public final boolean needReturnAddress;
        public final @Nullable String proofPayload;
        private ParsedApplicationManifest manifest = null;
        private boolean manifestLoading;

        private InitialRequest(String clientId, String manifestUrl, boolean needReturnAddress, @Nullable String proofPayload) {
            this.clientId = clientId;
            this.manifestUrl = manifestUrl;
            this.needReturnAddress = needReturnAddress;
            this.proofPayload = proofPayload;
        }

        public @Nullable ParsedApplicationManifest getManifest () {
            return manifest;
        }

        public void loadManifest (Runnable onFinishCallback) {
            if (manifestLoading) return;
            manifestLoading = true;
            JsonFetch.fetch(manifestUrl, result -> {
                manifestLoading = false;
                if (result == null) {
                    onFinishCallback.run();
                    return;
                }

                manifest = ParsedApplicationManifest.valueOf(result);
                onFinishCallback.run();
            });
        }

        public static InitialRequest valueOf (UriParser.ResultTonConnectLink request) throws Throwable {
            String clientId = request.id;
            String manifestUrl = request.request.getString("manifestUrl");
            String proofPayload = null;
            boolean needReturnAddress = false;

            JSONArray itemsArray  = request.request.getJSONArray("items");
            for (int a = 0; a < itemsArray.length(); a++) {
                JSONObject item = itemsArray.getJSONObject(a);
                String name = item.getString("name");
                if (TextUtils.equals(name, "ton_addr")) {
                    needReturnAddress = true;
                } else if (TextUtils.equals(name, "ton_proof")) {
                    proofPayload = item.getString("payload");
                }
            }

            return new InitialRequest(clientId, manifestUrl, needReturnAddress, proofPayload);
        }
    }

    public interface TonConnectRequestWithAppCallback {
        void run (Request request);
    }

    public interface ApplicationCallback {
        void run (ConnectedApplication application);
    }

    public static class ParsedApplicationManifest {
        public final String url;
        public final String name;
        public final String iconUrl;
        public final @Nullable String termsOfUseUrl;
        public final @Nullable String privacyPolicyUrl;

        private ParsedApplicationManifest (String url, String name, String iconUrl, @Nullable String termsOfUseUrl, @Nullable String privacyPolicyUrl) {
            this.url = url;
            this.name = name;
            this.iconUrl = iconUrl;
            this.termsOfUseUrl = termsOfUseUrl;
            this.privacyPolicyUrl = privacyPolicyUrl;
        }

        public String getHost () {
            try {
                Uri uri = Uri.parse(url);
                return uri.getHost();
            } catch (Exception e) {
                return name;
            }
        }

        public static @Nullable ParsedApplicationManifest valueOf (JSONObject manifest) {
            try {
                return new ParsedApplicationManifest(
                    manifest.getString("url"),
                    manifest.getString("name"),
                    manifest.getString("iconUrl"),
                    manifest.optString("termsOfUseUrl"),
                    manifest.optString("privacyPolicyUrl")
                );
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }
    }

    public static class ConnectedApplication {
        public final String bridgeUrl;
        public final String clientId;
        public final String manifestUrl;
        public final ParsedApplicationManifest manifest;
        public long lastEventId;

        private ConnectedApplication (String bridgeUrl, String clientId, String manifestUrl, ParsedApplicationManifest manifest, long lastEventId) {
            this.bridgeUrl = bridgeUrl;
            this.clientId = clientId;
            this.manifestUrl = manifestUrl;
            this.manifest = manifest;
            this.lastEventId = lastEventId;
        }
    }

    private static class ConnectedApplicationStore {
        private final SharedPreferences preferences;
        private final Set<String> connectedClientsId;
        private final HashMap<String, ConnectedApplication> connectedApps = new HashMap<>();

        private ConnectedApplicationStore () {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("ton_connected_apps", Context.MODE_PRIVATE);
            connectedClientsId = preferences.getStringSet("connected_apps_id", new HashSet<>());
            for (String clientId: connectedClientsId) {
                connectedApps.put(clientId, loadConnectedApp(clientId));
            }
        }

        public boolean has (String clientId) {
            return connectedApps.containsKey(clientId);
        }

        public @Nullable ConnectedApplication get (String clientId) {
            return connectedApps.get(clientId);
        }

        public void save (ConnectedApplication app) {
            connectedClientsId.add(app.clientId);
            connectedApps.put(app.clientId, app);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("connected_apps_id", connectedClientsId);
            saveConnectedApp(editor, app);
            editor.apply();
        }

        public void delete (String clientId) {
            String key = "connected_app_" + clientId + "_";
            String keyM = key + "manifest_";

            connectedClientsId.remove(clientId);
            connectedApps.remove(clientId);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("connected_apps_id", connectedClientsId);

            editor.remove(key + "bridgeUrl");
            editor.remove(key + "clientId");
            editor.remove(key + "manifestUrl");
            editor.remove(key + "lastEventId");
            editor.remove(keyM + "url");
            editor.remove(keyM + "name");
            editor.remove(keyM + "iconUrl");
            editor.remove(keyM + "termsOfUseUrl");
            editor.remove(keyM + "privacyPolicyUrl");
            editor.apply();
        }

        public void clear () {
            preferences.edit().clear().apply();
            connectedClientsId.clear();
            connectedApps.clear();
        }

        private ConnectedApplication loadConnectedApp (String clientId) {
            String key = "connected_app_" + clientId + "_";
            String keyM = key + "manifest_";
            return new ConnectedApplication(
                preferences.getString(key + "bridgeUrl", null),
                preferences.getString(key + "clientId", null),
                preferences.getString(key + "manifestUrl", null),
                new ParsedApplicationManifest (
                        preferences.getString(keyM + "url", null),
                        preferences.getString(keyM + "name", null),
                        preferences.getString(keyM + "iconUrl", null),
                        preferences.getString(keyM + "termsOfUseUrl", null),
                        preferences.getString(keyM + "privacyPolicyUrl", null)
                ),
                preferences.getLong(key + "lastEventId", 0)
            );
        }

        private void saveConnectedApp (SharedPreferences.Editor editor, ConnectedApplication app) {
            String key = "connected_app_" + app.clientId + "_";
            String keyM = key + "manifest_";
            editor.putString(key + "bridgeUrl", app.bridgeUrl);
            editor.putString(key + "clientId", app.clientId);
            editor.putString(key + "manifestUrl", app.manifestUrl);
            editor.putLong(key + "lastEventId", app.lastEventId);
            editor.putString(keyM + "url", app.manifest.url);
            editor.putString(keyM + "name", app.manifest.name);
            editor.putString(keyM + "iconUrl", app.manifest.iconUrl);
            editor.putString(keyM + "termsOfUseUrl", app.manifest.termsOfUseUrl);
            editor.putString(keyM + "privacyPolicyUrl", app.manifest.privacyPolicyUrl);
        }
    }
}
