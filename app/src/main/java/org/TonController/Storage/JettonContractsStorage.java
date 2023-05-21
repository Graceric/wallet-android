package org.TonController.Storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.JettonContract;
import org.TonController.Data.Jettons.RootJettonContract;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class JettonContractsStorage {
    private final SharedPreferences preferences;
    private final HashMap<String, RootJettonContract> rootContracts = new HashMap<>();
    private final HashMap<String, JettonContract> walletContracts = new HashMap<>();

    public JettonContractsStorage () {
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("jettons_cache", Context.MODE_PRIVATE);
        load();
    }

    public ArrayList<String> getTokens () {
        return new ArrayList<>(rootContracts.keySet());
    }

    public ArrayList<RootJettonContract> getTokensList () {
        return new ArrayList<>(rootContracts.values());
    }

    public @Nullable RootJettonContract get (String address) {
        return rootContracts.get(address);
    }

    public @Nullable JettonContract getWallet (String address) {
        return walletContracts.get(address);
    }

    public void clear () {
        preferences.edit().clear().apply();
        rootContracts.clear();
        walletContracts.clear();
    }

    public void save (RootJettonContract contract) {
        rootContracts.put(contract.address, contract);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("jettons_addresses", rootContracts.keySet());
        contract.save(editor, "key_" + contract.address + "_");
        editor.apply();
    }

    public void remove (String address) {
        rootContracts.remove(address);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("jettons_addresses", rootContracts.keySet());
        editor.apply();
    }

    public void save (JettonContract contract) {
        BigInteger balance = contract.balance.getBalance();

        walletContracts.put(contract.address, contract);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("_jetton_wallet_addresses", walletContracts.keySet());
        editor.putString("wallet_" + contract.address + "_root", contract.root.address);
        if (balance != null) {
            editor.putString("wallet_" + contract.address + "_balance", balance.toString());
        }
        editor.apply();
    }

    private void load () {
        Set<String> rootJettonAddresses = preferences.getStringSet("jettons_addresses", null);
        if (rootJettonAddresses == null) return;
        for (String address : rootJettonAddresses) {
            try {
                rootContracts.put(address, RootJettonContract.load(preferences, "key_" + address + "_"));
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }

        Set<String> walletJettonAddresses = preferences.getStringSet("_jetton_wallet_addresses", null);
        if (walletJettonAddresses == null) return;
        for (String address : walletJettonAddresses) {
            try {
                String rootAddress = preferences.getString("wallet_" + address + "_root", null);
                RootJettonContract root = rootContracts.get(rootAddress);
                if (root == null) continue;

                JettonContract contract = new JettonContract(address, root);
                contract.balance.setBalance(preferences.getString("wallet_" + address + "_balance", null));
                contract.balance.invalidate();

                walletContracts.put(address, contract);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }
}
