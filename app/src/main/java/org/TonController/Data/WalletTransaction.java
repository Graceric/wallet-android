/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.TonController.Data;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.TonController.Parsers.PayloadParser;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.Arrays;

import drinkless.org.ton.TonApi;

public class WalletTransaction {

    public TonApi.RawTransaction rawTransaction;
    public final WalletTransactionMessage inMsg;
    public final WalletTransactionMessage[] outMsgs;
    public final long balanceChange;

    public WalletTransaction (TonApi.RawTransaction transaction) {
        this.rawTransaction = transaction;
        this.inMsg = transaction.inMsg != null ? new WalletTransactionMessage(this, transaction.inMsg, false) : null;
        if (transaction.outMsgs == null || transaction.outMsgs.length == 0) {
            this.outMsgs = new WalletTransactionMessage[0];
        } else {
            this.outMsgs = new WalletTransactionMessage[transaction.outMsgs.length];
            for (int a = 0; a < transaction.outMsgs.length; a++) {
                this.outMsgs[a] = new WalletTransactionMessage(this, transaction.outMsgs[a], true);
            }
        }
        this.balanceChange = getBalanceChange();
    }

    private WalletTransaction (TonApi.RawTransaction transaction, WalletTransactionMessage inMsg, WalletTransactionMessage[] outMsgs) {
        this.rawTransaction = transaction;
        this.inMsg = inMsg;
        this.inMsg.setTransaction(this);
        this.outMsgs = outMsgs;
        for (int a = 0; a < outMsgs.length; a++) {
            this.outMsgs[a].setTransaction(this);
        }
        this.balanceChange = getBalanceChange();
    }

    public boolean isPending () {
        return rawTransaction.transactionId.lt == 0;
    }

    public boolean isEmpty () {
        return Utilities.isEmptyTransaction(rawTransaction);
    }

    public boolean isInternalTokenReceive () {
        return inMsg != null && inMsg.parsedPayload instanceof PayloadParser.ResultJettonInternalTransfer;
    }

    public boolean isInternalTokenSend () {
        if (isPending() && inMsg != null) {
            if (outMsgs.length > 0 && outMsgs[0].parsedPayload instanceof PayloadParser.ResultJettonTransfer) {
                return true;
            }
        }

        for (int a = 0; a < outMsgs.length; a++) {
            if (outMsgs[a].parsedPayload instanceof PayloadParser.ResultJettonInternalTransfer) {
                return true;
            }
        }
        return false;
    }


    private long getBalanceChange () {
        long result = 0;
        if (inMsg != null) {
            result += inMsg.msg.value;
        }
        for (int a = 0; a < outMsgs.length; a++) {
            result -= outMsgs[a].msg.value;
        }
        return result;
    }

    public void save (SharedPreferences.Editor editor, String key) {
        TonApi.RawTransaction transaction = rawTransaction;

        if (inMsg != null) {
            editor.putBoolean(key + "contains", true);
            inMsg.save(editor, key + "inMsg.");
        }

        int outMsgCount = outMsgs.length;
        editor.putInt(key + "outMsgCount", outMsgCount);
        for (int b = 0; b < outMsgCount; b++) {
            outMsgs[b].save(editor, key + "outMsg" + b + ".");
        }

        editor.putLong(key + "utime", transaction.utime);
        editor.putString(key + "data", Base64.encodeToString(Utilities.nonNull(transaction.data), Base64.DEFAULT));
        editor.putLong(key + "lt", transaction.transactionId.lt);
        editor.putString(key + "hash", Base64.encodeToString(Utilities.nonNull(transaction.transactionId.hash), Base64.DEFAULT));
        editor.putLong(key + "fee", transaction.fee);
        editor.putLong(key + "storageFee", transaction.storageFee);
        editor.putLong(key + "otherFee", transaction.otherFee);
    }

    public static WalletTransaction load (SharedPreferences preferences, String key) {
        try {
            if (!preferences.getBoolean(key + "contains", false)) {
                return null;
            }

            WalletTransactionMessage inMsg = WalletTransactionMessage.load(null, preferences, key + "inMsg.");
            WalletTransactionMessage[] outMsg;
            TonApi.RawMessage[] outMsgs;
            if (preferences.contains(key + "outMsgCount")) {
                outMsg = new WalletTransactionMessage[preferences.getInt(key + "outMsgCount", 0)];
                outMsgs = new TonApi.RawMessage[outMsg.length];
                for (int b = 0; b < outMsg.length; b++) {
                    outMsg[b] = WalletTransactionMessage.load(null, preferences, key + "outMsg" + b + ".");
                    outMsgs[b] = outMsg[b].msg;
                }
            } else {
                outMsg = new WalletTransactionMessage[0];
                outMsgs = new TonApi.RawMessage[0];
            }
            TonApi.RawTransaction transaction = new TonApi.RawTransaction(
                null,
                preferences.getLong(key + "utime", 0),
                Base64.decode(preferences.getString(key + "data", null), Base64.DEFAULT),
                new TonApi.InternalTransactionId(preferences.getLong(key + "lt", 0), Base64.decode(preferences.getString(key + "hash", null), Base64.DEFAULT)),
                preferences.getLong(key + "fee", 0),
                preferences.getLong(key + "storageFee", 0),
                preferences.getLong(key + "otherFee", 0),
                inMsg.msg,
                outMsgs
            );

            return new WalletTransaction(transaction, inMsg, outMsg);
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<WalletTransactionMessage> from (ArrayList<WalletTransaction> transactions) {
        ArrayList<WalletTransactionMessage> messages = new ArrayList<>();
        for (int a = 0; a < transactions.size(); a++) {
            WalletTransaction walletTransaction = transactions.get(a);
            if (walletTransaction.outMsgs.length > 0) {
                messages.addAll(Arrays.asList(walletTransaction.outMsgs));
            }
            if (walletTransaction.inMsg != null) {
                if (walletTransaction.outMsgs.length == 0 || !TextUtils.isEmpty(walletTransaction.inMsg.msg.source.accountAddress)) {
                    messages.add(walletTransaction.inMsg);
                }
            }
        }
        return messages;
    }
}
