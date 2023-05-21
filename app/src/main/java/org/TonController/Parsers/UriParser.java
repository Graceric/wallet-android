package org.TonController.Parsers;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

public class UriParser {
    public static @Nullable Result parse (String uriText) {
        try {
            Uri uri = Uri.parse(prepare(uriText));
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            boolean isTonDomain = Utilities.isValidTonDomain(host);
            boolean isTonScheme = TextUtils.equals(scheme, "ton");

            boolean isTonhubConnect  = TextUtils.equals(scheme, "https") && TextUtils.equals(host, "tonhub.com") && TextUtils.equals(path, "/ton-connect");
            boolean isTonkeeperConnect = TextUtils.equals(scheme, "https") && TextUtils.equals(host, "app.tonkeeper.com") && TextUtils.equals(path, "/ton-connect");
            boolean isUniversalConnect = TextUtils.equals(scheme, "tc");
            boolean isTonConnect = isTonkeeperConnect || isTonhubConnect || isUniversalConnect;

            if (isTonDomain) {
                return new ResultTonDomain(host);
            } else if (isTonScheme) {
                if (TextUtils.isEmpty(path)) return null;
                if (!TextUtils.equals(host, "transfer")) return null;
                String address = path.replace("/", "");
                if (!Utilities.isValidWalletAddress(address)) return null;
                String text = uri.getQueryParameter("text");
                String amount = uri.getQueryParameter("amount");

                return new ResultTonLink(address, Utilities.parseLong(amount), text);
            } else if (isTonConnect) {
                String v = uri.getQueryParameter("v");
                String idHex = uri.getQueryParameter("id");
                String rStr = uri.getQueryParameter("r");
                String ret = uri.getQueryParameter("ret");
                int version = Utilities.parseInt(v);
                JSONObject request = new JSONObject(rStr);

                final String bridgeUrl;
                if (isTonhubConnect) {
                    bridgeUrl = "https://connect.tonhubapi.com/tonconnect";
                } else {
                    bridgeUrl = "https://bridge.tonapi.io/bridge";
                }

                return new ResultTonConnectLink(bridgeUrl, version, idHex, request, ret);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        return null;
    }

    private static String prepare (String uriText) {
        if (uriText.startsWith("ton:transfer")) {
            uriText = uriText.replace("ton:transfer", "ton://transfer");
        }

        return uriText;
    }


    public static abstract class Result {}

    public static class ResultTonDomain extends Result {
        public final String domain;

        private ResultTonDomain (String domain) {
            this.domain = domain;
        }
    }

    public static class ResultTonLink extends Result {
        public final @NonNull String address;
        public final long amount;
        public final @Nullable String comment;

        private ResultTonLink (@NonNull String address, long amount, @Nullable String comment) {
            this.address = address;
            this.amount = amount;
            this.comment = comment;
        }

        public static ResultTonLink valueOf (@NonNull String address) {
            return new ResultTonLink(address, 0, null);
        }
    }

    public static class ResultTonConnectLink extends Result {
        public final String bridgeUrl;
        public final int version;
        public final String id;
        public final @NonNull JSONObject request;
        public final @Nullable String ret;

        private ResultTonConnectLink (String bridgeUrl, int version, String id, @NonNull JSONObject request, @Nullable String ret) {
            this.bridgeUrl = bridgeUrl;
            this.version = version;
            this.id = id;
            this.request = request;
            this.ret = ret;
        }
    }
}
