package org.TonController.Data.Accounts;

import android.content.SharedPreferences;

import org.TonController.Data.WalletTransaction;

import java.util.ArrayList;

public class TransactionList extends Data {
    private ArrayList<WalletTransaction> list;
    private final int limit;

    public TransactionList (SharedPreferences preferences, String key, int limit) {
        super(preferences, key);
        this.limit = limit;
        this.list = load();
    }

    public void setTransactions (ArrayList<WalletTransaction> list) {
        this.list = list;
    }

    public ArrayList<WalletTransaction> transactions () {
        return list;
    }

    @Override
    public void save (SharedPreferences.Editor editor) {
        int transactionsCount = list.size();
        if (limit > 0) transactionsCount = Math.min(limit, transactionsCount);

        editor.putInt(key + "sCount", transactionsCount);
        for (int a = 0; a < transactionsCount; a++) {
            list.get(a).save(editor, key + "_" + a + "_");
        }
    }

    private ArrayList<WalletTransaction> load () {
        int transactionsCount = preferences.getInt(key + "sCount", 0);
        ArrayList<WalletTransaction> transactions = new ArrayList<>(transactionsCount);
        for (int a = 0; a < transactionsCount; a++) {
            WalletTransaction transaction = WalletTransaction.load(preferences, key + "_" + a + "_");
            if (transaction == null) continue;
            transactions.add(transaction);
        }
        return transactions;
    }
}
