package org.TonController.Data;

import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Parsers.PayloadParser;
import org.UI.Fragments.Main.WalletActivityListAdapter;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;

import java.math.BigInteger;

import drinkless.org.ton.TonApi;

public class WalletTransactionMessage {
    public WalletTransaction walletTransaction;
    public TonApi.RawTransaction transaction;
    public final TonApi.RawMessage msg;
    public final boolean isOut;
    public final @Nullable PayloadParser.Result parsedPayload;
    public final @Nullable byte[] stateInit;
    private @Nullable String toDomain;

    private @Nullable TransactionMessageRepresentation representation;
    private int representationMode = -1;

    public WalletTransactionMessage (WalletTransaction walletTransaction, TonApi.RawMessage msg, boolean isOut) {
        this.walletTransaction = walletTransaction;
        if (walletTransaction != null) {
            this.transaction = walletTransaction.rawTransaction;
        }
        this.msg = msg;
        this.isOut = isOut;

        if (msg.msgData instanceof TonApi.MsgDataRaw) {
            parsedPayload = PayloadParser.parse(((TonApi.MsgDataRaw) msg.msgData).body);
            stateInit = ((TonApi.MsgDataRaw) msg.msgData).initState;
        } else {
            parsedPayload = null;
            stateInit = null;
        }
    }

    public void setTransaction (WalletTransaction transaction) {
        this.walletTransaction = transaction;
        this.transaction = transaction.rawTransaction;
    }

    public @Nullable String getToDomain () {
        return toDomain;
    }

    public void setOutInfo (String toDomain) {
        this.toDomain = toDomain;
    }



    public TransactionMessageRepresentation getRepresentation (int mode, @Nullable RootJettonContract contract) {
        if (representation != null && mode == representationMode) return representation;

        representationMode = mode;

        final boolean isPending = walletTransaction.isPending();
        final long currentStorageFee = !isPending ? transaction.storageFee: 0;
        final long currentTransactionFee = !isPending ? transaction.otherFee: 0;

        final String dateTimeFull = LocaleController.getInstance().formatterTransactionTime.format(walletTransaction.rawTransaction.utime * 1000);
        final String dateTimeShort = LocaleController.getInstance().formatterDay.format(walletTransaction.rawTransaction.utime * 1000);

        if (mode == WalletActivityListAdapter.TRANSACTIONS_MODE_DEFAULT) {
            final long value = isOut ? -msg.value : msg.value;
            final String transactionFeeText;
            final String transactionFeeTextFull;
            if (currentStorageFee != 0 || currentTransactionFee != 0) {
                transactionFeeTextFull = LocaleController.formatString("WalletTransactionFee", R.string.WalletTransactionFee, Utilities.formatCurrency(currentTransactionFee + currentStorageFee));
                transactionFeeText = LocaleController.formatString("WalletBlockchainFees", R.string.WalletBlockchainFees, Utilities.formatCurrency(- currentStorageFee - currentTransactionFee));
            } else {
                transactionFeeTextFull = null;
                transactionFeeText = null;
            }

            final String transactionAddress;
            if (value > 0) {
                transactionAddress = transaction.inMsg.source.accountAddress;
            } else {
                if (isPending) {
                    transactionAddress = msg.destination.accountAddress;
                } else if (transaction.outMsgs != null && transaction.outMsgs.length > 0) {
                    transactionAddress = msg.destination.accountAddress;
                } else {
                    transactionAddress = null;
                }
            }

            representation = new TransactionMessageRepresentation(transactionAddress, TonAmount.valueOf(value), parsedPayload, transactionFeeText, transactionFeeTextFull, dateTimeFull, dateTimeShort);
        } else if (mode == WalletActivityListAdapter.TRANSACTIONS_MODE_JETTON) {
            final PayloadParser.ResultJettonInternalTransfer transfer;
            if (parsedPayload instanceof PayloadParser.ResultJettonInternalTransfer) {
                transfer = (PayloadParser.ResultJettonInternalTransfer) parsedPayload;
            } else {
                transfer = null;
            }

            BigInteger value = null;
            String transactionAddress = null;
            if (walletTransaction.isInternalTokenReceive()) {
                if (transfer != null) {
                    transactionAddress = transfer.fromAddress;
                    value = transfer.amount;
                }
            } else if (walletTransaction.isInternalTokenSend()) {
                if (walletTransaction.isPending()) {
                    if (parsedPayload instanceof PayloadParser.ResultJettonTransfer) {
                        transactionAddress = ((PayloadParser.ResultJettonTransfer) parsedPayload).toOwnerAddress;
                        value = ((PayloadParser.ResultJettonTransfer) parsedPayload).jettonAmount.negate();
                    }
                } else if (transfer != null) {
                    if (walletTransaction.inMsg != null && walletTransaction.inMsg.parsedPayload instanceof PayloadParser.ResultJettonTransfer) {
                        transactionAddress = ((PayloadParser.ResultJettonTransfer) walletTransaction.inMsg.parsedPayload).toOwnerAddress;
                    }
                    value = transfer.amount.negate();
                }
            }

            StringBuilder feeTextBuilder = new StringBuilder();
            long change = walletTransaction.balanceChange - currentStorageFee - currentTransactionFee;
            if (change > 0) feeTextBuilder.append("+");
            feeTextBuilder.append(LocaleController.formatString("WalletBalnceChanges", R.string.WalletBalnceChanges, Utilities.formatCurrency(change)));

            TonAmount b = new TonAmount(contract != null ? contract.content.decimals: 9);
            b.setBalance(value);
            representation = new TransactionMessageRepresentation(transactionAddress, b, transfer != null ? transfer.parsedForwardPayload: null, feeTextBuilder.toString(), feeTextBuilder.toString(), dateTimeFull, dateTimeShort);
        } else {
            throw new RuntimeException("Unknown mode");
        }


        return representation;
    }




    public void save (SharedPreferences.Editor editor, String key) {
        editor.putBoolean(key + "contains", true);
        editor.putBoolean(key + "flags.isOut", isOut);
        editor.putString(key + "source", msg.source.accountAddress);
        editor.putString(key + "destination", msg.destination.accountAddress);
        editor.putLong(key + "value", msg.value);
        editor.putLong(key + "fwdFee", msg.fwdFee);
        editor.putLong(key + "ihrFee", msg.ihrFee);
        editor.putLong(key + "createdLt", msg.createdLt);
        editor.putString(key + "bodyHash", Base64.encodeToString(Utilities.nonNull(msg.bodyHash), Base64.NO_WRAP));

        byte[] body = null;
        byte[] initState = null;
        if (msg.msgData instanceof TonApi.MsgDataRaw) {
            TonApi.MsgDataRaw msgData = (TonApi.MsgDataRaw) msg.msgData;
            body = msgData.body;
            initState = msgData.initState;
        }

        editor.putString(key + "msgData.body", Base64.encodeToString(Utilities.nonNull(body), Base64.DEFAULT));
        editor.putString(key + "msgData.initState", Base64.encodeToString(Utilities.nonNull(initState), Base64.DEFAULT));

        if (toDomain != null) {
            editor.putString(key + "flags.toDomain", toDomain);
        }
    }

    public static WalletTransactionMessage load (WalletTransaction transaction, SharedPreferences preferences, String key) {
        try {
            if (!preferences.contains(key + "contains")) return null;

            TonApi.MsgData msgData = new TonApi.MsgDataRaw(
                Base64.decode(preferences.getString(key + "msgData.body", null), Base64.DEFAULT),
                Base64.decode(preferences.getString(key + "msgData.initState", null), Base64.DEFAULT)
            );

            TonApi.RawMessage msg = new TonApi.RawMessage(
                new TonApi.AccountAddress(preferences.getString(key + "source", null)),
                new TonApi.AccountAddress(preferences.getString(key + "destination", null)),
                preferences.getLong(key + "value", 0),
                preferences.getLong(key + "fwdFee", 0),
                preferences.getLong(key + "ihrFee", 0),
                preferences.getLong(key + "createdLt", 0),
                Base64.decode(preferences.getString(key + "bodyHash", null), Base64.DEFAULT),
                msgData
            );

            WalletTransactionMessage message = new WalletTransactionMessage(transaction, msg, preferences.getBoolean(key + "flags.isOut", false));
            message.toDomain = preferences.getString(key + "flags.toDomain", null);
            return message;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }
}
