package org.Utils;

import android.graphics.Bitmap;
import android.text.TextUtils;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.TonController.Data.WalletTransaction;
import org.TonController.Parsers.UriParser;
import org.TonController.TonController;
import org.json.JSONObject;

import java.util.ArrayList;

import drinkless.org.ton.TonApi;

public class Callbacks {
    public interface SendInfoGetters {
        TonAmount getCurrentWalletBalance ();

        TonAmount getInputFieldSendAmountValue ();

        String getInputFieldRecipientAddressString ();

        String getInputFieldRecipientDomainString ();

        String getInputFieldCommentString ();

        default String getRecipientAddressOrDomain () {
            String domain = getInputFieldRecipientDomainString();
            return TextUtils.isEmpty(domain) ? getInputFieldRecipientAddressString(): domain;
        }
    }

    public interface SendAmountInputCallback {
        void run (long balance, boolean sendAllFlag);
    }

    public interface StringCallback {
        void run(String result);
    }

    public interface BitmapCallback {
        void run(Bitmap result);
    }

    public interface TonLibCallback {
        void run(Object result);
    }

    public interface ErrorCallback {
        void run(String text, TonApi.Error error);
    }

    public interface WordsCallback {
        void run(String[] words);
    }

    public interface AccountStateCallback {
        void run(TonApi.RawFullAccountState state);
    }

    public interface AccountStateCallbackExtended {
        void run(String address, TonApi.RawFullAccountState state, boolean needUpdateTransactions);
    }

    public interface BooleanCallback {
        void run(boolean param);
    }

    public interface BytesCallback {
        void run(byte[] param);
    }

    public interface LongCallback {
        void run(long value);
    }

    public interface LongValuedCallback {
        void run(Long value);
    }

    public interface DnsResolvedResultCallback {
        void run(TonApi.DnsResolved dnsResolved);
    }

    public interface SmcRunResultCallback {
        void run(TonApi.SmcRunResult smcResult);
    }

    public interface JsonCallback {
        void run(JSONObject result);
    }

    public interface AuthCallback {
        void run(TonController.UserAuthInfo info);
    }

    public interface TonLinkDelegate {
        void run (UriParser.ResultTonLink result);
    }

    public interface RawTransactionsCallback {
        void run (TonApi.RawTransactions transactions);
    }

    public interface RawAndParsedTransactionsCallback {
        void run (TonApi.RawTransactions transactions, ArrayList<WalletTransaction> transactionsParsed);
    }

    public interface RootJettonContractCallback {
        void run (RootJettonContract contract);
    }

    public interface QueryInfoCallback {
        void run (TonApi.QueryInfo result);
    }

    public interface JettonBalanceUpdateCallback {
        void run (String jettonWalletAddress, long balance);
    }
}
