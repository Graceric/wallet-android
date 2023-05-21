package org.TonController;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.TonController.Data.Accounts.ExtendedAccountState;
import org.TonController.Data.Jettons.JettonContract;
import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.WalletTransaction;
import org.TonController.Data.Wallets;
import org.TonController.Storage.JettonContractsStorage;
import org.TonController.Storage.RecentRecipientsStorage;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import drinkless.org.ton.TonApi;

public class AccountsStateManager {
    private final RecentRecipientsStorage recentRecipientsStorage;
    private final JettonContractsStorage jettonContractsStorage;

    private static final String ACCOUNT_STATE_KEY = "state";
    private static final String JETTON_WALLET_KEY = "jetton";

    private final SharedPreferences preferences;
    private final TonController controller;

    private final AccountStateReceiver accountStateReceiver;
    private final TonController.GuaranteedReceiver transactionsReceiver;

    private final HashMap<String, ExtendedAccountState> accountsCache = new HashMap<>();
    private final HashMap<String, ArrayList<Runnable>> pendingTransactionsCompleteCallbacks = new HashMap<>();
    private final HashMap<String, HashSet<Callbacks.JettonBalanceUpdateCallback>> jettonBalanceUpdatesCallbacks = new HashMap<>();
    private final HashMap<String, HashSet<Callbacks.AccountStateCallbackExtended>> shortPollUpdateListeners = new HashMap<>();

    protected AccountsStateManager (TonController controller) {
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("wallets_cache_V3", Context.MODE_PRIVATE);
        this.controller = controller;

        this.accountStateReceiver = new AccountStateReceiver(this::onFetchRawFullAccountState);
        this.transactionsReceiver = new TonController.GuaranteedReceiver(TonApi.RawTransactions.CONSTRUCTOR);

        this.recentRecipientsStorage = new RecentRecipientsStorage();
        this.jettonContractsStorage = new JettonContractsStorage();

        loadCache();

        if (controller.getCurrentWalletAddress() != null) {
            updateSupportedWalletTypes(controller.getPublicKey(), controller.getCurrentSubWalletId());
        }
    }

    public void cleanup () {
        Set<String> keys = new HashSet<>(shortPollUpdateListeners.keySet());
        for (String address: keys) {
            Set<Callbacks.AccountStateCallbackExtended> runnableSet = shortPollUpdateListeners.get(address);
            if (runnableSet == null) continue;
            runnableSet = new HashSet<>(runnableSet);
            for (Callbacks.AccountStateCallbackExtended callback: runnableSet) {
                unsubscribeFromRawAccountStateUpdates(address, callback);
            }
        }

        accountsCache.clear();
        supportedWalletsMapByAddress.clear();
        pendingTransactionsCompleteCallbacks.clear();
        jettonBalanceUpdatesCallbacks.clear();
        shortPollUpdateListeners.clear();
        preferences.edit().clear().apply();
        recentRecipientsStorage.clear();
        jettonContractsStorage.clear();
    }

    public RecentRecipientsStorage getRecentRecipientsStorage () {
        return recentRecipientsStorage;
    }

    public JettonContractsStorage getJettonContractsStorage () {
        return jettonContractsStorage;
    }

    public void addPendingTransaction (String address, WalletTransaction pendingTransaction) {
        ExtendedAccountState state = accountsCache.get(address);
        if (state == null) return;

        ArrayList<WalletTransaction> pTransactions = state.pendingTransactions.transactions();
        pTransactions.add(0, pendingTransaction);
        controller.getNotificationCenter().postNotificationName(NotificationCenter.walletPendingTransactionsChanged);
        state.pendingTransactions.save();
    }

    public void fetchRawTransactions (String address, boolean reload, @Nullable Callbacks.RawAndParsedTransactionsCallback callback) {
        final TonApi.RawFullAccountState cachedState = getCachedFullAccountState(address);
        final ArrayList<WalletTransaction> cachedTransactions = getCachedTransactions(address);
        final TonApi.InternalTransactionId fromTransactionId;

        if (reload || cachedTransactions.isEmpty()) {
            fromTransactionId = Utilities.getLastTransactionId(cachedState);
            reload = true;
        } else {
            int index = cachedTransactions.size() - 1;
            fromTransactionId = cachedTransactions.get(index).rawTransaction.transactionId;
        }

        boolean finalReload = reload;

        transactionsReceiver.receive(new TonApi.RawGetTransactionsV2(null, new TonApi.AccountAddress(address), fromTransactionId, 25, false), result -> {
            TonApi.RawTransactions transactions = (TonApi.RawTransactions) result;
            ArrayList<WalletTransaction> parsedTransactions = onGetRawTransactions(address, finalReload, transactions);
            if (callback != null) {
                callback.run(transactions, parsedTransactions);
            }
        });
    }

    private ArrayList<WalletTransaction> onGetRawTransactions (String address, boolean isReload, TonApi.RawTransactions transactions) {
        if (transactions == null) return new ArrayList<>();

        ArrayList<WalletTransaction> parsedTransactions = new ArrayList<>(transactions.transactions.length);
        ExtendedAccountState state = accountsCache.get(address);
        if (state == null) {
            for (int a = 0; a < transactions.transactions.length; a++) {
                parsedTransactions.add(new WalletTransaction(transactions.transactions[a]));
            }
            return parsedTransactions;
        }

        ArrayList<WalletTransaction> cachedTransactions = state.transactions.transactions();
        long newestCachedTransactionLt = cachedTransactions.isEmpty() ? 0 :
            cachedTransactions.get(0).rawTransaction.transactionId.lt;
        long oldestCachedTransactionLt = cachedTransactions.isEmpty() ? -1 :
            cachedTransactions.get(cachedTransactions.size() - 1).rawTransaction.transactionId.lt;

        ArrayList<WalletTransaction> pendingTransactions = state.pendingTransactions.transactions();
        ArrayList<WalletTransaction> newTransactions = new ArrayList<>(0);
        ArrayList<WalletTransaction> oldTransactions = new ArrayList<>(0);
        boolean needDropCache = true;
        boolean pendingChanged = false;
        for (int a = 0; a < transactions.transactions.length; a++) {
            TonApi.RawTransaction transaction = transactions.transactions[a];
            boolean isNewTransaction = oldestCachedTransactionLt == -1
                || transaction.transactionId.lt > newestCachedTransactionLt;
            boolean isOldTransaction = transaction.transactionId.lt < oldestCachedTransactionLt;
            needDropCache &= (isNewTransaction || isOldTransaction);
            if (isNewTransaction || isOldTransaction) {
                WalletTransaction transactionToAdd = new WalletTransaction(transaction);
                pendingChanged |= checkIsPendingTransaction(address, pendingTransactions, transactionToAdd);
                (isNewTransaction ? newTransactions : oldTransactions).add(transactionToAdd);
                parsedTransactions.add(transactionToAdd);
            } else {
                boolean isFound = false;
                for (int b = 0; b < cachedTransactions.size(); b++) {
                    WalletTransaction cachedTransaction = cachedTransactions.get(b);
                    if (cachedTransaction.rawTransaction.transactionId == transaction.transactionId) {
                        parsedTransactions.add(cachedTransaction);
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    parsedTransactions.add(new WalletTransaction(transaction));
                }
            }
        }

        if (needDropCache) {
            if (!isReload) throw new RuntimeException();
            cachedTransactions.clear();
            cachedTransactions.addAll(newTransactions);
        } else {
            cachedTransactions.addAll(oldTransactions);
            cachedTransactions.addAll(0, newTransactions);
        }

        state.transactions.save();
        if (pendingChanged) {
            state.pendingTransactions.save();
            controller.getNotificationCenter().postNotificationName(NotificationCenter.walletPendingTransactionsChanged);
        }

        return parsedTransactions;
    }

    private boolean checkIsPendingTransaction (String address, ArrayList<WalletTransaction> pendingTransactions, WalletTransaction transaction) {
        if (pendingTransactions == null || pendingTransactions.isEmpty()) return false;

        for (int a = 0; a < pendingTransactions.size(); a++) {
            WalletTransaction pending = pendingTransactions.get(a);
            if (transaction.rawTransaction.inMsg != null) {
                if (Arrays.equals(pending.rawTransaction.inMsg.bodyHash, transaction.rawTransaction.inMsg.bodyHash)) {
                    for (int c = 0; c < Math.min(pending.outMsgs.length, transaction.outMsgs.length); c++) {
                        transaction.outMsgs[c].setOutInfo(pending.outMsgs[c].getToDomain());
                    }
                    pendingTransactions.remove(a);
                    onPendingTransactionComplete(address, transaction);
                    return true;
                }
            }
        }

        return false;
    }

    private void onPendingTransactionComplete (String address, WalletTransaction transaction) {
        String key = address + Base64.encodeToString(transaction.inMsg.msg.bodyHash, Base64.NO_WRAP);
        ArrayList<Runnable> callbacks = pendingTransactionsCompleteCallbacks.get(key);
        if (callbacks == null) return;
        for (Runnable callback : callbacks) {
            callback.run();
        }
        callbacks.clear();
        pendingTransactionsCompleteCallbacks.remove(key);
    }

    private void checkPendingTransactionsForFailure (String address, TonApi.RawFullAccountState state) {
        ArrayList<WalletTransaction> pendingTransactions = getPendingTransactions(address);
        if (state == null || pendingTransactions.isEmpty()) return;

        long time = Utilities.getLastSyncTime(state);
        boolean pendingChanged = false;

        for (int a = 0, N = pendingTransactions.size(); a < N; a++) {
            WalletTransaction pending = pendingTransactions.get(a);
            if (pending.rawTransaction.otherFee <= time) {
                pendingTransactions.remove(a);
                N--;
                a--;
                pendingChanged = true;
            }
        }

        if (pendingChanged) {
            controller.getNotificationCenter().postNotificationName(NotificationCenter.walletPendingTransactionsChanged);
            saveAccountState(accountsCache.get(address));
        }
    }

    private final HashMap<String, WalletInfo> supportedWalletsMapByAddress = new HashMap<>();

    protected void updateSupportedWalletTypes (byte[] publicKey, long subWalletId) {
        supportedWalletsMapByAddress.clear();
        for (int type : Wallets.SUPPORTED_WALLET_TYPES) {
            TonApi.RawInitialAccountState state = Wallets.makeWalletInitialAccountState(type, publicKey, subWalletId);
            String address = Wallets.getAddressFromInitialAccountState(0, state);
            supportedWalletsMapByAddress.put(address, new WalletInfo(type, address, subWalletId));
        }
    }

    public HashMap<String, WalletInfo> getSupportedWalletsMapWithAddressKey () {
        return supportedWalletsMapByAddress;
    }






    /* Subscriptions */

    public void subscribeToRawAccountStateUpdates (String address, Callbacks.AccountStateCallbackExtended listener) {
        HashSet<Callbacks.AccountStateCallbackExtended> runnableSet = shortPollUpdateListeners.get(address);
        if (runnableSet == null) {
            runnableSet = new HashSet<>();
            shortPollUpdateListeners.put(address, runnableSet);
        }
        runnableSet.add(listener);
        scheduleShortPoll();
    }

    public void unsubscribeFromRawAccountStateUpdates (String address, Callbacks.AccountStateCallbackExtended listener) {
        HashSet<Callbacks.AccountStateCallbackExtended> runnableSet = shortPollUpdateListeners.get(address);
        if (runnableSet == null) return;
        runnableSet.remove(listener);
        if (runnableSet.isEmpty()) {
            shortPollUpdateListeners.remove(address);
        }
        if (shortPollUpdateListeners.isEmpty()) {
            cancelShortPoll();
        }
    }


    public void subscribeToPendingTransactionComplete (String address, WalletTransaction pendingTransaction, Runnable callback) {
        subscribeToPendingTransactionComplete(address, pendingTransaction.rawTransaction, callback);
    }

    public void subscribeToPendingTransactionComplete (String address, TonApi.RawTransaction transaction, Runnable callback) {
        String key = address + Base64.encodeToString(transaction.inMsg.bodyHash, Base64.NO_WRAP);
        ArrayList<Runnable> callbacks = pendingTransactionsCompleteCallbacks.get(key);
        if (callbacks == null) {
            callbacks = new ArrayList<>(1);
            pendingTransactionsCompleteCallbacks.put(key, callbacks);
        }
        callbacks.add(callback);
    }

    public void unsubscribeFromPendingTransactionComplete (String address, TonApi.RawTransaction transaction, Runnable callback) {
        String key = address + Base64.encodeToString(transaction.inMsg.bodyHash, Base64.NO_WRAP);
        ArrayList<Runnable> callbacks = pendingTransactionsCompleteCallbacks.get(key);
        if (callbacks == null) return;

        callbacks.remove(callback);
    }


    private final Callbacks.AccountStateCallbackExtended jettonWalletRawAccountStateUpdateListener = this::onJettonWalletStateUpdate;

    public void subscribeToJettonBalanceUpdates (String jettonWalletAddress, Callbacks.JettonBalanceUpdateCallback callback) {
        HashSet<Callbacks.JettonBalanceUpdateCallback> callbacks = jettonBalanceUpdatesCallbacks.get(jettonWalletAddress);
        if (callbacks == null) {
            callbacks = new HashSet<>(1);
            jettonBalanceUpdatesCallbacks.put(jettonWalletAddress, callbacks);
        }

        callbacks.add(callback);
        subscribeToRawAccountStateUpdates(jettonWalletAddress, jettonWalletRawAccountStateUpdateListener);
        onJettonWalletStateUpdate(jettonWalletAddress, null, true);
    }

    public void unsubscribeFromJettonBalanceUpdates (String jettonWalletAddress, Callbacks.JettonBalanceUpdateCallback callback) {
        HashSet<Callbacks.JettonBalanceUpdateCallback> callbacks = jettonBalanceUpdatesCallbacks.get(jettonWalletAddress);
        if (callbacks == null) return;

        callbacks.remove(callback);
        unsubscribeFromRawAccountStateUpdates(jettonWalletAddress, jettonWalletRawAccountStateUpdateListener);
    }



    /* Account fetching */

    public void fetchRawFullAccountState (String address, Callbacks.AccountStateCallback callback) {
        accountStateReceiver.receive(address, callback);
    }

    private void onFetchRawFullAccountState (String address, @NonNull TonApi.RawFullAccountState newState) {
        ExtendedAccountState cachedExtendedState = accountsCache.get(address);
        boolean needNeedUpdateTransactions = cachedExtendedState == null;
        if (cachedExtendedState == null) {
            cachedExtendedState = new ExtendedAccountState(preferences, ACCOUNT_STATE_KEY, address);
            cachedExtendedState.setState(newState);
            accountsCache.put(address, cachedExtendedState);
        } else {
            needNeedUpdateTransactions = cachedExtendedState.needRequestNewTransactions(newState);
            cachedExtendedState.setState(newState);
        }

        if (needNeedUpdateTransactions) {
            saveAccountState(cachedExtendedState);
        }

        checkPendingTransactionsForFailure(address, newState);
        HashSet<Callbacks.AccountStateCallbackExtended> runnableSet = shortPollUpdateListeners.get(address);
        if (runnableSet == null) return;
        for (Callbacks.AccountStateCallbackExtended callback : runnableSet) {
            callback.run(address, newState, needNeedUpdateTransactions);
        }
    }



    /* Jettons fetching */

    public void fetchJettonWalletAddress (String tonWalletAddress, String rootJettonWalletAddress, Callbacks.StringCallback callback) {
        controller.getJettonWalletAddress(rootJettonWalletAddress, tonWalletAddress, address -> {
            if (address != null) {
                ExtendedAccountState state = accountsCache.get(tonWalletAddress);
                if (state != null) {
                    state.jettonWallets.put(rootJettonWalletAddress, address);
                    state.save();
                }

                RootJettonContract root = jettonContractsStorage.get(rootJettonWalletAddress);
                JettonContract contract = jettonContractsStorage.getWallet(address);
                if (root != null && contract == null) {
                    contract = new JettonContract(address, root);
                    jettonContractsStorage.save(contract);
                }
            }

            callback.run(address);
        });
    }

    private void fetchJettonWalletBalance (String jettonWalletAddress, Callbacks.LongValuedCallback callback) {
        controller.getJettonWalletBalance(jettonWalletAddress, balance -> {
            if (balance != null) {
                JettonContract contract = jettonContractsStorage.getWallet(jettonWalletAddress);
                if (contract != null) {
                    contract.balance.setBalance(balance);
                    jettonContractsStorage.save(contract);
                }
            }

            callback.run(balance);
        });
    }

    private void onJettonWalletStateUpdate (String address, TonApi.RawFullAccountState state, boolean needUpdateTransactions) {
        if (!needUpdateTransactions || !jettonBalanceUpdatesCallbacks.containsKey(address)) return;
        fetchJettonWalletBalance(address, balance -> {
            if (balance == null) return;

            HashSet<Callbacks.JettonBalanceUpdateCallback> callbacks = jettonBalanceUpdatesCallbacks.get(address);
            if (callbacks == null) return;

            for (Callbacks.JettonBalanceUpdateCallback callback : callbacks) {
                callback.run(address, balance);
            }
        });
    }



    /* Save-Load Cache */

    private void saveAccountState (ExtendedAccountState state) {
        if (state == null) return;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("saved_accounts", accountsCache.keySet());
        state.save();
        editor.apply();
    }

    private void loadCache () {
        Set<String> accounts = preferences.getStringSet("saved_accounts", null);
        if (accounts != null) {
            for (String account : accounts) {
                accountsCache.put(account, new ExtendedAccountState(preferences, ACCOUNT_STATE_KEY, account));
            }
        }
    }



    /* Cache Getters */

    public boolean isLoadedAccount (String address) {
        return accountsCache.containsKey(address);
    }

    public boolean isLoadedAccountWithEmptyTransactions (String address) {
        TonApi.RawFullAccountState state = getCachedFullAccountState(address);
        return state != null && state.lastTransactionId.lt == 0;
    }

    public @Nullable TonApi.RawFullAccountState getCachedFullAccountState (String address) {
        ExtendedAccountState state = accountsCache.get(address);
        return state != null ? state.state() : null;
    }

    public @NonNull ArrayList<WalletTransaction> getCachedTransactions (String address) {
        ExtendedAccountState state = accountsCache.get(address);
        return state != null ? state.transactions.transactions() : new ArrayList<>();
    }

    public @NonNull ArrayList<WalletTransaction> getPendingTransactions (String address) {
        ExtendedAccountState state = accountsCache.get(address);
        return state != null ? state.pendingTransactions.transactions() : new ArrayList<>();
    }

    public @Nullable String getCachedJettonWalletAddress (String tonWalletAddress, String rootJettonWalletAddress) {
        ExtendedAccountState state = accountsCache.get(tonWalletAddress);
        return state != null ? state.jettonWallets.get(rootJettonWalletAddress) : null;
    }

    public @Nullable JettonContract getCachedJettonWallet (String jettonWalletAddress) {
        return jettonContractsStorage.getWallet(jettonWalletAddress);
    }



    /* Short Polling */

    private Runnable shortPollRunnable;
    private boolean shortPollingInProgress;

    private void runShortPolling () {
        if (shortPollingInProgress) {
            return;
        }
        ArrayList<String> accountsToPoll = new ArrayList<>(shortPollUpdateListeners.keySet());
        if (accountsToPoll.isEmpty()) return;
        shortPollingInProgress = true;

        fetchRawFullAccountStateList(new ArrayList<>(accountsToPoll), () -> {
            shortPollingInProgress = false;
            shortPollRunnable = null;
            scheduleShortPoll();
        });
    }

    private void scheduleShortPoll () {
        if (shortPollRunnable != null) return;
        AndroidUtilities.runOnUIThread(shortPollRunnable = this::runShortPolling, 3000);
    }

    private void cancelShortPoll () {
        if (shortPollRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(shortPollRunnable);
            shortPollRunnable = null;
        }
        shortPollingInProgress = false;
    }



    /* Short Polling Impl */

    private final ArrayList<Pair<ArrayList<String>, Runnable>> pollListQueue = new ArrayList<>();
    private boolean pollAccountsStarted;

    private void fetchRawFullAccountStateList (ArrayList<String> addresses, Runnable onFinishRunnable) {
        pollListQueue.add(new Pair<>(new ArrayList<>(addresses), onFinishRunnable));
        if (!pollAccountsStarted) {
            pollAccountsStarted = true;
            executeFetchRawFullAccountStateList();
        }
    }

    private void executeFetchRawFullAccountStateList () {
        if (pollListQueue.isEmpty()) {
            pollAccountsStarted = false;
            return;
        }

        Pair<ArrayList<String>, Runnable> info = pollListQueue.get(0);
        String address = info.first.get(0);
        fetchRawFullAccountState(address, state -> {
            Pair<ArrayList<String>, Runnable> info1 = pollListQueue.get(0);

            info.first.remove(0);
            if (info1.first.isEmpty()) {
                pollListQueue.remove(0);
                info1.second.run();
            }

            if (pollListQueue.isEmpty()) {
                pollAccountsStarted = false;
            } else {
                executeFetchRawFullAccountStateList();
            }
        });
    }



    /* Guess Account */

    public static class WalletInfo {
        public final String address;
        public final long walletId;
        public final int type;

        public WalletInfo (int type, String address, long walletId) {
            this.type = type;
            this.address = address;
            this.walletId = walletId;
        }
    }

    public static class WalletInfoWithState extends WalletInfo {
        public final TonApi.RawFullAccountState state;

        public WalletInfoWithState (int type, String address, long walletId, TonApi.RawFullAccountState state) {
            super(type, address, walletId);
            this.state = state;
        }
    }

    public interface GuestAccountResult {
        void run (WalletInfoWithState account);
    }

    protected void guessWalletAccount (byte[] publicKey, long[] walletIds, GuestAccountResult callback) {
        final HashMap<String, WalletInfo> wallets = new HashMap<>();
        for (long subWalletId : walletIds) {
            for (int type : Wallets.SUPPORTED_WALLET_TYPES) {
                TonApi.RawInitialAccountState state = Wallets.makeWalletInitialAccountState(type, publicKey, subWalletId);
                String address = Wallets.getAddressFromInitialAccountState(0, state);
                wallets.put(address, new WalletInfo(type, address, subWalletId));
            }
        }

        ArrayList<String> accounts = new ArrayList<>(wallets.keySet());

        fetchRawFullAccountStateList(accounts, () -> {
            WalletInfoWithState result = null;
            for (String address : accounts) {
                TonApi.RawFullAccountState state = getCachedFullAccountState(address);
                WalletInfo info = wallets.get(address);
                if (info == null || state == null) continue;
                if (result == null && info.walletId != 0 && info.type == Wallets.WALLET_TYPE_V3R2) {
                    result = new WalletInfoWithState(info.type, info.address, info.walletId, state);
                    continue;
                }

                if (state.balance > 0 && (result == null || state.balance > result.state.balance)) {
                    result = new WalletInfoWithState(info.type, info.address, info.walletId, state);
                }
            }
            callback.run(result);
        });
    }

}
