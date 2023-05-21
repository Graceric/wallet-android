package org.TonController.TonConnect.Connection;

import android.os.AsyncTask;
import android.util.Base64;

import com.iwebpp.crypto.TweetNaclFast;

import org.json.JSONObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.Utils.Callbacks;
import org.ton.java.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TonConnectSender {
    public static void send (String bridgeUrl, byte[] secretKey, String fromClientId, String toClientId, JSONObject message, Callbacks.JsonCallback callback) {
        TweetNaclFast.Box box = new TweetNaclFast.Box(Utils.hexToBytes(toClientId), secretKey);

        byte[] encodedMessage = message.toString().getBytes();
        byte[] nonce = new byte[24];
        Utilities.random.nextBytes(nonce);

        byte[] encryptedMessage = box.box(encodedMessage, nonce);
        byte[] fullPackage = new byte[encryptedMessage.length + 24];
        System.arraycopy(nonce, 0, fullPackage, 0, 24);
        System.arraycopy(encryptedMessage, 0, fullPackage, 24, encryptedMessage.length);
        String encodedFullPackage = Base64.encodeToString(fullPackage, Base64.NO_WRAP);

        String url = bridgeUrl + "/message?client_id=" + fromClientId + "&to=" + toClientId + "&ttl=300";
        TonConnectBridgeSenderTask.send(url, encodedFullPackage, callback);
    }

    private static class TonConnectBridgeSenderTask extends AsyncTask<Void, Void, JSONObject> {

        private final String currentUrl;
        private final String currentBody;
        private final Callbacks.JsonCallback onFinishCallback;

        public static void send (String url, String body, Callbacks.JsonCallback callback) {
            new TonConnectBridgeSenderTask(url, body, callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
        }

        public TonConnectBridgeSenderTask (String url, String body, Callbacks.JsonCallback callback) {
            super();
            currentUrl = url;
            currentBody = body;
            onFinishCallback = callback;
        }

        protected JSONObject doInBackground (Void... voids) {
            ByteArrayOutputStream outbuf = null;
            InputStream httpConnectionStream = null;
            try {
                URL downloadUrl = new URL(currentUrl);
                HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                httpConnection.setRequestMethod("POST");
                httpConnection.setConnectTimeout(1000);
                httpConnection.setReadTimeout(2000);
                httpConnection.setDoOutput(true);
                OutputStream outStream = httpConnection.getOutputStream();
                OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
                outStreamWriter.write(currentBody);
                outStreamWriter.flush();
                outStreamWriter.close();
                outStream.close();
                httpConnection.connect();
                httpConnectionStream = httpConnection.getInputStream();

                outbuf = new ByteArrayOutputStream();

                byte[] data = new byte[1024 * 32];
                while (true) {
                    int read = httpConnectionStream.read(data);
                    if (read > 0) {
                        outbuf.write(data, 0, read);
                    } else if (read == -1) {
                        break;
                    } else {
                        break;
                    }
                }

                String result = outbuf.toString();
                return new JSONObject(result);
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                try {
                    if (httpConnectionStream != null) {
                        httpConnectionStream.close();
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                try {
                    if (outbuf != null) {
                        outbuf.close();
                    }
                } catch (Exception ignore) {

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute (final JSONObject result) {
            if (onFinishCallback != null) {
                onFinishCallback.run(result);
            }
        }
    }
}
