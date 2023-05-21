package org.TonController.Data.Accounts;

import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import drinkless.org.ton.TonApi;

public class ExtendedAccountState extends Data {
    public final String address;
    private @Nullable TonApi.RawFullAccountState state;
    public final TransactionList transactions;
    public final TransactionList pendingTransactions;
    public final HashMap<String, String> jettonWallets;

    public ExtendedAccountState (SharedPreferences preferences, String key, String address) {
        super(preferences, key + "_" + address + "_");
        this.address = address;
        this.state = load();
        transactions = new TransactionList(preferences, this.key + "transactions_", 25);
        pendingTransactions = new TransactionList(preferences, this.key + "pending_transactions_", -1);
        jettonWallets = loadJettonWallets();
    }

    public boolean needRequestNewTransactions (TonApi.RawFullAccountState newState) {
        return newState != null && (isInvalidated() || state == null || (state.lastTransactionId.lt != newState.lastTransactionId.lt));
    }

    public void setState (@Nullable TonApi.RawFullAccountState newState) {
        if (newState == null) return;
        state = newState;
    }

    public @Nullable TonApi.RawFullAccountState state () {
        return state;
    }

    public boolean isInvalidated () {
        return state == null || state.syncUtime == 0;
    }

    @Override
    public void save (SharedPreferences.Editor editor) {
        if (state == null) return;

        editor.putBoolean(key + "contains", true);
        editor.putLong(key + "balance", state.balance);
        editor.putLong(key + "transaction.lt", state.lastTransactionId.lt);
        editor.putString(key + "transaction.hash", Base64.encodeToString(Utilities.nonNull(state.lastTransactionId.hash), Base64.NO_WRAP));
        editor.putInt(key + "tonblock.workchain", state.blockId.workchain);
        editor.putLong(key + "tonblock.shard", state.blockId.shard);
        editor.putInt(key + "tonblock.seqno", state.blockId.seqno);
        editor.putString(key + "tonblock.rootHash", Base64.encodeToString(Utilities.nonNull(state.blockId.rootHash), Base64.NO_WRAP));
        editor.putString(key + "tonblock.fileHash", Base64.encodeToString(Utilities.nonNull(state.blockId.fileHash), Base64.NO_WRAP));
        editor.putString(key + "frozenHash", Base64.encodeToString(Utilities.nonNull(state.frozenHash), Base64.NO_WRAP));
        editor.putString(key + "code", Base64.encodeToString(Utilities.nonNull(state.code), Base64.NO_WRAP));
        editor.putString(key + "data", Base64.encodeToString(Utilities.nonNull(state.data), Base64.NO_WRAP));

        transactions.save(editor);

        Set<String> jettonRootAddresses = jettonWallets.keySet();
        editor.putStringSet(key + "jetton_root_list", jettonRootAddresses);
        for (Map.Entry<String, String> entry: jettonWallets.entrySet())  {
            editor.putString(key + "jetton_" + entry.getKey(), entry.getValue());
        }
    }

    private @Nullable TonApi.RawFullAccountState load () {
        if (!preferences.getBoolean(key + "contains", false)) return null;
        try {
            TonApi.RawFullAccountState accountState = new TonApi.RawFullAccountState();
            accountState.balance = preferences.getLong(key + "balance", 0);
            accountState.lastTransactionId = new TonApi.InternalTransactionId();
            accountState.lastTransactionId.lt = preferences.getLong(key + "transaction.lt", 0);
            accountState.lastTransactionId.hash = Base64.decode(preferences.getString(key + "transaction.hash", null), Base64.DEFAULT);
            accountState.blockId = new TonApi.TonBlockIdExt();
            accountState.blockId.workchain = preferences.getInt(key + "tonblock.workchain", 0);
            accountState.blockId.shard = preferences.getLong(key + "tonblock.shard", 0);
            accountState.blockId.seqno = preferences.getInt(key + "tonblock.seqno", 0);
            accountState.blockId.rootHash = Base64.decode(preferences.getString(key + "tonblock.rootHash", null), Base64.DEFAULT);
            accountState.blockId.fileHash = Base64.decode(preferences.getString(key + "tonblock.fileHash", null), Base64.DEFAULT);
            accountState.frozenHash = Base64.decode(preferences.getString(key + "frozenHash", null), Base64.DEFAULT);
            accountState.code = Base64.decode(preferences.getString(key + "code", null), Base64.DEFAULT);
            accountState.data = Base64.decode(preferences.getString(key + "data", null), Base64.DEFAULT);
            return accountState;
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return null;
    }

    private HashMap<String, String> loadJettonWallets () {
        HashMap<String, String> wallets = new HashMap<>();
        if (!preferences.getBoolean(key + "contains", false)) return wallets;
        try {
            Set<String> jettonRootAddresses = preferences.getStringSet(key + "jetton_root_list", null);
            if (jettonRootAddresses == null) return wallets;

            for (String rootWalletAddress: jettonRootAddresses)  {
                String address = preferences.getString(key + "jetton_" + rootWalletAddress, null);
                if (address != null) {
                    wallets.put(rootWalletAddress, address);
                }
            }

            return wallets;
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return wallets;
    }
}
