package org.TonController.TonConnect.Requests;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.TonController.Parsers.PayloadParser;
import org.TonController.TonConnect.TonConnectController;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.Utilities;
import org.ton.java.cell.Cell;

public class RequestSendTransaction extends Request {
    public final long validUntil;
    public final @Nullable String network;
    public final @Nullable String from;
    public final MessageRequest[] messages;

    public final long totalAmount;

    protected RequestSendTransaction (TonConnectController.ConnectedApplication application, TonConnectController.BridgeRequest request, long validUntil, @Nullable String network, @Nullable String from, MessageRequest[] messages) {
        super(application, request);
        this.validUntil = validUntil;
        this.network = network;
        this.from = from;
        this.messages = messages;

        long totalAmount = 0;
        for (MessageRequest message : messages) {
            totalAmount += message.amount;
        }
        this.totalAmount = totalAmount;
    }

    public static RequestSendTransaction valueOf (TonConnectController.ConnectedApplication application, TonConnectController.BridgeRequest request) throws Throwable {
        JSONObject params = new JSONObject(request.params[0]);

        long validUntil = params.optLong("valid_until", 0);
        String networkId = params.optString("network");
        if (TextUtils.isEmpty(networkId)) networkId = null;

        String from = params.has("from") ? Utilities.normalizeAddress(params.getString("from")) : null;

        JSONArray messages = params.getJSONArray("messages");

        MessageRequest[] messageRequests = new MessageRequest[messages.length()];
        for (int a = 0; a < messages.length(); a++) {
            messageRequests[a] = MessageRequest.valueOf(messages.getJSONObject(a));
        }

        return new RequestSendTransaction(application, request, validUntil, networkId, from, messageRequests);
    }

    public static class MessageRequest {
        public final String address;
        public final long amount;
        public final @Nullable byte[] payload;
        public final PayloadParser.Result parsedPayload;
        public final @Nullable byte[] stateInit;

        public MessageRequest (String address, long amount, @Nullable byte[] payload, @Nullable byte[] stateInit) {
            this.address = address;
            this.payload = payload;
            this.parsedPayload = PayloadParser.parse(payload);
            this.stateInit = stateInit;
            this.amount = amount;
        }

        public static MessageRequest valueOf (JSONObject message) throws Throwable {
            String address = Utilities.normalizeAddress(message.getString("address"));

            long amount = Utilities.parseLong(message.getString("amount"));

            byte[] payload = message.has("payload") ? Cell.deserializeBoc(
                Base64.decode(message.getString("payload"), Base64.DEFAULT)
            ).toBoc(false, true, false) : null;

            byte[] stateInit = message.has("stateInit") ? Cell.deserializeBoc(
                Base64.decode(message.getString("stateInit"), Base64.DEFAULT)
            ).toBoc(false, true, false) : null;

            return new MessageRequest(address, amount, payload, stateInit);
        }
    }
}
