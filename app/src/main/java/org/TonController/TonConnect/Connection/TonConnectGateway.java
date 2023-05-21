package org.TonController.TonConnect.Connection;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class TonConnectGateway {
    private final DispatchQueue queue;

    public final String bridgeUrl;
    public final String myClientId;
    private final Callback messageCallback;
    private final AtomicBoolean isFinished;

    private HttpURLConnection httpConnection;
    private BufferedReader bufferedReader;
    private long lastEventId;

    public TonConnectGateway (String bridgeUrl, String myClientId, Callback messageCallback) {
        this.queue = new DispatchQueue("bridge-listener-from-to-" + myClientId);
        this.bridgeUrl = bridgeUrl;
        this.myClientId = myClientId;
        this.messageCallback = messageCallback;
        this.isFinished = new AtomicBoolean(true);
    }

    public void start () {
        if (isFinished.getAndSet(false)) {
            queue.postRunnable(this::connect);
        }
    }

    public void stop () {
        isFinished.set(true);
        Utilities.globalQueue.postRunnable(() -> {
            try {
                /*if (bufferedReader != null) {
                    bufferedReader.close();
                }*/
                if (httpConnection != null) {
                    httpConnection.disconnect();
                    httpConnection = null;
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    public boolean isStarted () {
        return !isFinished.get();
    }

    private String getUrl () {
        StringBuilder b = new StringBuilder(this.bridgeUrl);
        b.append("/events?client_id=");
        b.append(this.myClientId);
        if (lastEventId > 0) {
            b.append("&last_event_id=");
            b.append(lastEventId);
        }

        return b.toString();
    }

    private void connect () {
        Log.i("TON_CONNECT_LISTENER", "Start " + this.bridgeUrl);

        if (isFinished.get()) return;

        httpConnection = null;
        InputStream httpConnectionStream = null;

        try {
            URL url = new URL(getUrl());

            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
            httpConnection.setConnectTimeout(1000);
            httpConnection.setRequestProperty("Connection", "Keep-Alive");
            httpConnection.connect();
            httpConnectionStream = httpConnection.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(httpConnectionStream));
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() == 0) {
                    if (data.length() > 0) {
                        onNewDataRawPackage(data.toString());
                    }
                    data = new StringBuilder();
                } else {
                    if (data.length() > 0) {
                        data.append("\n");
                    }
                    data.append(line);
                }
            }

            httpConnectionStream.close();
            bufferedReader.close();
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                if (httpConnectionStream != null) {
                    httpConnectionStream.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }

        if (!isFinished.get()) {
            queue.postRunnable(this::connect, 1000);
        }
    }

    private void onNewDataRawPackage (String rawPackage) {
        try {
            if (TextUtils.equals(rawPackage, "body: heartbeat")) return;
            Log.i("TONCONNECT", "NEW PACKAGE " + rawPackage);

            int sepIndex = rawPackage.indexOf("\n");
            if (sepIndex < 0) return;
            String idStr = rawPackage.substring(0, sepIndex);
            String dataStr = rawPackage.substring(sepIndex + 1);
            if (!idStr.startsWith("id: ")) return;
            if (!dataStr.startsWith("data: ")) return;

            long id = Utilities.parseLong(idStr.substring(4));
            JSONObject dataJson = new JSONObject(dataStr.substring(6));

            String fromClientId = dataJson.getString("from");
            String encryptedMessageStr = dataJson.getString("message");

            byte[] encryptedMessageWithNonce = Base64.decode(encryptedMessageStr, Base64.DEFAULT);
            byte[] encryptedMessage = Arrays.copyOfRange(encryptedMessageWithNonce, 24, encryptedMessageWithNonce.length);
            byte[] nonce = Arrays.copyOfRange(encryptedMessageWithNonce, 0, 24);

            BridgeEncryptedMessage message = new BridgeEncryptedMessage(bridgeUrl, id, fromClientId, myClientId, nonce, encryptedMessage);
            lastEventId = id;

            AndroidUtilities.runOnUIThread(() -> messageCallback.run(message));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }


    /**/

    public interface Callback {
        void run (BridgeEncryptedMessage message);
    }

    public static class BridgeEncryptedMessage {
        public final String bridgeUrl;
        public final long eventId;
        public final String fromClientId;
        public final String toClientId;
        public final byte[] nonce;
        public final byte[] encryptedMessage;

        private BridgeEncryptedMessage (String bridgeUrl, long eventId, String fromClientId, String toClientId, byte[] nonce, byte[] encryptedMessage) {
            this.bridgeUrl = bridgeUrl;
            this.eventId = eventId;
            this.fromClientId = fromClientId;
            this.toClientId = toClientId;
            this.nonce = nonce;
            this.encryptedMessage = encryptedMessage;
        }
    }
}
