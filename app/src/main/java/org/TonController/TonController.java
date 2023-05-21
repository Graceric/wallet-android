package org.TonController;

import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.WalletTransaction;
import org.TonController.Data.Wallets;
import org.TonController.FirebaseNotifications.FirebaseTokenRetriever;
import org.TonController.Storage.JettonContractsStorage;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonConnect.TonConnectController;
import org.Utils.Callbacks;
import org.Utils.network.JsonFetch;
import org.Utils.network.NetworkChangeReceiver;
import org.Utils.network.WalletConfigLoader;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.ton.java.cell.CellBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.crypto.Cipher;

import drinkless.org.ton.Client;
import drinkless.org.ton.TonApi;
import me.vkryl.core.StringUtils;

public class TonController extends BaseController {
    public final static int KEY_PROTECTION_TYPE_NONE = 0;
    public final static int KEY_PROTECTION_TYPE_BIOMETRIC = 2;

    private static final TonController[] Instance = new TonController[Utilities.MAX_ACCOUNT_COUNT];

    public static TonController getInstance (int num) {
        TonController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (TonController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new TonController(num);
                }
            }
        }
        return localInstance;
    }

    private @Nullable TonConnectController tonConnectController;
    private final AccountsStateManager accountsStateManager;
    private final KeysConfig keysConfig;

    private final Client client;
    private File keyDirectory;
    private boolean initialed;
    private long defaultWalletId;

    private int syncProgress;
    private boolean walletWasImported;

    public TonController (int num) {
        super(num);
        keysConfig = new KeysConfig();
        if (!isWalletNotCreated()) {
            tonConnectController = getTonConnectController();
        }
        accountsStateManager = new AccountsStateManager(this);
        Client.ResultHandler resultHandler = object -> {
            if (object instanceof TonApi.UpdateSyncState) {
                AndroidUtilities.runOnUIThread(() -> {
                    TonApi.UpdateSyncState updateSyncState = (TonApi.UpdateSyncState) object;
                    if (updateSyncState.syncState instanceof TonApi.SyncStateDone) {
                        syncProgress = 100;
                    } else if (updateSyncState.syncState instanceof TonApi.SyncStateInProgress) {
                        TonApi.SyncStateInProgress progress = (TonApi.SyncStateInProgress) updateSyncState.syncState;
                        syncProgress = (int) ((progress.currentSeqno - progress.fromSeqno) / (double) (progress.toSeqno - progress.fromSeqno) * 100);
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.walletSyncProgressChanged, syncProgress);
                });
            }
        };
        client = Client.create(resultHandler, null, null);
        if (BuildVars.LOGS_ENABLED) {
            client.send(new TonApi.SetLogStream(new TonApi.LogStreamFile(FileLog.getTonlibLogPath(), 1024 * 1024 * 5)), null);
        }
        loadTonConfigFromUrl(null);
        checkDeviceToken();

        try {
            IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();
            networkChangeReceiver.setOnConnectionChangeDelegate(this::onConnectionChangeUpdate);
            ApplicationLoader.applicationContext.registerReceiver(networkChangeReceiver, intentFilter);
        } catch (Exception e) {
            FileLog.e(e);
        }



        /* tests */
        // accountsStateManager.addAddressToPollList(getCurrentWalletAddress());
    }

    private void onConnectionChangeUpdate (boolean hasConnection) {
        getNotificationCenter().postNotificationName(NotificationCenter.hasNetworkConnectionUpdated, hasConnection);
    }

    public void cleanup () {
        setNotificationsEnabled(false);

        if (tonConnectController != null) {
            tonConnectController.cleanup();
            tonConnectController = null;
        }
        accountsStateManager.cleanup();
        keysConfig.clear();

        KeysController.deleteKey(keysConfig.mKeyNameBiometric);
        KeysController.deleteKey(keysConfig.mKeyNamePasscode);
        if (keyDirectory != null) {
            initTonLib();
            sendRequest(new TonApi.DeleteAllKeys(), true);
        }
    }

    public int getKeyProtectionType () {
        return KeysController.getKeyProtectionType(getAndroidKeyName(false));
    }

    private boolean isKeyCreated (boolean useBiometric) {
        return KeysController.isKeyCreated(getAndroidKeyName(false), useBiometric);
    }

    public Cipher getCipherForDecrypt () {
        return KeysController.getCipherForDecrypt(getAndroidKeyName(false));
    }

    private boolean initTonLib () {
        if (initialed) {
            return true;
        }
        TonApi.Config config = getConfig();

        keyDirectory = new File(ApplicationLoader.getFilesDirFixed(), "ton" + currentAccount);
        keyDirectory.mkdirs();
        if (!TextUtils.isEmpty(config.config)) {
            TonApi.Options options = new TonApi.Options(config, new TonApi.KeyStoreTypeDirectory(keyDirectory.getAbsolutePath()));
            Object result = sendRequest(new TonApi.Init(options), true);
            if (result instanceof TonApi.OptionsInfo) {
                sendRequest(new TonApi.SetLogVerbosityLevel(3), true);
                TonApi.OptionsInfo optionsInfo = (TonApi.OptionsInfo) result;
                defaultWalletId = optionsInfo.configInfo.defaultWalletId;
                initialed = true;
                return true;
            }
        } else {
            loadTonConfigFromUrl(null);
        }
        return false;
    }


    public void isKeyStoreInvalidated (Callbacks.BooleanCallback callback) {
        Utilities.globalQueue.postRunnable(() -> AndroidUtilities.runOnUIThread(() -> callback.run(KeysController.isKeyInvalidated(getAndroidKeyName(false)))));
    }

    public void finishSettingUserPasscode () {
        if (keysConfig.tmpDecryptedTonWalletData != null) {
            Arrays.fill(keysConfig.tmpDecryptedTonWalletData, (byte) 0);
            keysConfig.tmpDecryptedTonWalletData = null;
        }
    }

    public String[] getHintWords () {
        initTonLib();
        Object response = sendRequest(new TonApi.GetBip39Hints(), true);
        if (response instanceof TonApi.Bip39Hints) {
            return ((TonApi.Bip39Hints) response).words;
        }
        return null;
    }

    @Deprecated
    public JettonContractsStorage getJettonsCache () {
        return accountsStateManager.getJettonContractsStorage();
    }

    public void removeJettonToken (String address) {
        if (TextUtils.equals(getCurrentJettonRootAddress(), address)) {
            updateJettonRootTokenAddress(null);
        }
        getJettonsCache().remove(address);
    }

    public int getSyncProgress () {
        return syncProgress;
    }


    public void createWallet (String[] words, Callbacks.WordsCallback onFinishRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        walletWasImported = words != null;
        if (!initTonLib()) {
            AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("TONLIB_INIT_FAIL", null));
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            if (!KeysController.isReady()) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL", null));
                return;
            }
            cleanup();
            keysConfig.edit().updateKeyNames("walletKey_non_biometric" + Utilities.random.nextLong(), null).apply();
            if (!isKeyCreated(false)) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL", null));
                return;
            }

            byte[] password = Utilities.randomBytes(64);
            TonApi.Key key = createTonWalletKey(words, password, onErrorRunnable);
            if (key == null) return;

            if (words == null) {
                TonApi.ExportedKey exportedKey = exportTonWalletKey(key, password, onErrorRunnable);
                if (exportedKey == null) return;
                onFinishWalletCreate(false, exportedKey.wordList, onFinishRunnable, password, key);
            } else {
                onFinishWalletCreate(true, words, onFinishRunnable, password, key);
            }
        });
    }

    private void onFinishWalletCreate (boolean isImport, String[] words, Callbacks.WordsCallback onFinishRunnable, byte[] password, TonApi.Key key) {
        AndroidUtilities.runOnUIThread(() -> {
            keysConfig.edit().updatePublicKey(key.publicKey).apply();
            if (!isImport) {
                updateWalletAddressAndInfo();
            }

            preloadWallet();


            int len = 1 + 2 + password.length + key.secret.length;
            int padding = len % 16;
            if (padding != 0) {
                padding = 16 - padding;
                len += padding;
            }
            byte[] dataToEncrypt = new byte[len];
            dataToEncrypt[0] = (byte) padding;
            dataToEncrypt[1] = 'o';
            dataToEncrypt[2] = 'k';
            System.arraycopy(password, 0, dataToEncrypt, 3, password.length);
            System.arraycopy(key.secret, 0, dataToEncrypt, 3 + password.length, key.secret.length);
            if (padding != 0) {
                byte[] pad = Utilities.randomBytes(padding);
                System.arraycopy(pad, 0, dataToEncrypt, 3 + password.length + key.secret.length, pad.length);
            }
            keysConfig.tmpDecryptedTonWalletData = dataToEncrypt;
            onFinishRunnable.run(words);
        });
    }

    private void preloadWallet () {
        String currentAddress = getCurrentWalletAddress();
        if (currentAddress == null) {
            guessAccountAddress(address -> {
                if (address != null) {
                    accountsStateManager.fetchRawTransactions(address, true, null);
                }
            });
        } else {
            accountsStateManager.fetchRawFullAccountState(currentAddress, state -> {
                accountsStateManager.fetchRawTransactions(currentAddress, true, null);
            });
        }
    }

    public void guessAccountAddress (@Nullable Callbacks.StringCallback callback) {
        accountsStateManager.guessWalletAccount(getPublicKey(), new long[]{0, defaultWalletId}, result -> {
            if (result == null) {
                if (callback != null) {
                    callback.run(null);
                }
                return;
            }
            if (getCurrentWalletAddress() == null) {
                updateWalletAddressAndInfo(result.address, result.type, result.walletId);
            }
            if (callback != null) {
                callback.run(result.address);
            }
        });
    }


    public void updateJettonRootTokenAddress (@Nullable String address) {
        keysConfig.edit().updateRootJettonAddress(address).apply();
        getNotificationCenter().postNotificationName(NotificationCenter.walletChangeRootJettonAddress);
    }

    public void updateWalletAddressAndInfo (int type) {
        TonApi.RawInitialAccountState state = Wallets.makeWalletInitialAccountState(type, getPublicKey(), getCurrentSubWalletId());
        String address = Wallets.getAddressFromInitialAccountState(0, state);
        updateWalletAddressAndInfo(address, type, getCurrentSubWalletId());
    }

    private void updateWalletAddressAndInfo () {
        updateWalletAddressAndInfo(getCurrentWalletType());
    }

    private void updateWalletAddressAndInfo (String address, int type, long subWalletId) {
        keysConfig.edit().updateWalletAddressAndType(address, type, subWalletId).updateRootJettonAddress(null).apply();
        accountsStateManager.updateSupportedWalletTypes(getPublicKey(), subWalletId);
        getNotificationCenter().postNotificationName(NotificationCenter.walletChangeWalletVersion);
    }



    /* Notifications */

    private void checkDeviceToken () {
        FirebaseTokenRetriever.fetchDeviceToken(this::onUpdateDeviceToken, null);
    }

    public void onUpdateDeviceToken (String token) {
        if (TextUtils.equals(token, keysConfig.mFirebaseFcmToken)) return;
        updateNotificationsSettings(token, keysConfig.mNotificationsEnabled);
    }

    public void setNotificationsEnabled (boolean notificationsEnabled) {
        if (notificationsEnabled == keysConfig.mNotificationsEnabled) return;
        updateNotificationsSettings(keysConfig.mFirebaseFcmToken, notificationsEnabled);
    }

    private void updateNotificationsSettings (String token, boolean notificationsEnabled) {
        boolean notificationsEnabledChanged = notificationsEnabled != keysConfig.mNotificationsEnabled;
        boolean tokenChanged = !TextUtils.equals(token, keysConfig.mFirebaseFcmToken);
        if (!tokenChanged && !notificationsEnabledChanged) return;

        if (keysConfig.mFirebaseFcmToken != null && (keysConfig.mNotificationsEnabled && (!notificationsEnabled || tokenChanged))) {    // unsubscribe
            JsonFetch.fetch("https://tonobserver.com/api/v2/notifications/unsubscribe/" + keysConfig.mFirebaseFcmToken, null);
        }
        if (token != null && notificationsEnabled) {
            Set<String> accounts = getAccountsStateManager().getSupportedWalletsMapWithAddressKey().keySet();
            if (!accounts.isEmpty()) {
                StringBuilder url = new StringBuilder("https://tonobserver.com/api/v2/notifications/subscribe/");
                url.append(token);
                url.append("?accounts=");
                for (String address: accounts) {
                    url.append(address);
                    url.append(",");
                }
                ;
                JsonFetch.fetch(url.substring(0, url.length() - 1), null);
            }
        }

        keysConfig.edit().updateFirebaseFcmToken(token, notificationsEnabled).apply();
    }



    public boolean isNotificationsEnabled () {
        return keysConfig.mNotificationsEnabled;
    }



    /* Getters */

    public TonConnectController getTonConnectController () {
        if (tonConnectController == null) {
            if (keysConfig.mTonConnectSecretKey == null) {
                byte[] tonConnectPublicKey = new byte[32];
                byte[] tonConnectSecretKey = Utilities.randomBytes(32);
                Utilities.privateToPublicX25519(tonConnectSecretKey, tonConnectPublicKey);
                keysConfig.edit().updateTonConnectKeypair(
                    Base64.encodeToString(tonConnectSecretKey, Base64.NO_WRAP),
                    Base64.encodeToString(tonConnectPublicKey, Base64.NO_WRAP)
                ).apply();
            }

            tonConnectController = new TonConnectController(this,
                Base64.decode(keysConfig.mTonConnectSecretKey, Base64.DEFAULT),
                Base64.decode(keysConfig.mTonConnectPublicKey, Base64.DEFAULT)
            );
        }
        return tonConnectController;
    }

    public AccountsStateManager getAccountsStateManager () {
        return accountsStateManager;
    }

    public String getCurrentWalletAddress () {
        return keysConfig.mTonWalletAddress;
    }

    public String getCurrentJettonRootAddress () {
        return keysConfig.mJettonRootAddress;
    }

    public int getCurrentWalletType () {
        return keysConfig.mTonWalletType;
    }

    public long getCurrentSubWalletId () {
        return TextUtils.isEmpty(keysConfig.mTonWalletAddress) ? defaultWalletId : keysConfig.mTonWalletId;
    }

    public boolean isNetworkConfigNotLoaded () {
        return TextUtils.isEmpty(keysConfig.mCachedConfigFromUrl);
    }

    public boolean isWalletWasImported () {
        return walletWasImported;
    }

    public boolean isWalletNotCreated () {
        return keysConfig.mEncryptedTonWalletData == null || keysConfig.mEncryptedPasscodeData == null;
    }

    public byte[] getPublicKey () {
        return Utilities.publicKeyToBytes(keysConfig.mTonWalletPublicKey);
    }

    public String getNetworkId () {
        return keysConfig.mCurrentNetworkId;
    }

    private String getAndroidKeyName (boolean nonBiometric) {
        boolean biometric = !nonBiometric;
        if (biometric && keysConfig.mKeyNameBiometric != null) {
            return keysConfig.mKeyNameBiometric;
        } else {
            return keysConfig.mKeyNamePasscode;
        }
    }



    /* Config */

    public void loadTonConfigFromUrl (Callbacks.BooleanCallback onConfigLoadRunnable) {
        final String idCurrentNetworkId = keysConfig.mCurrentNetworkId;
        final String configUrl = keysConfig.mCurrentConfigUrl;
        final String networkName = keysConfig.mBlockchainName;
        final String cachedConfig = keysConfig.mCachedConfigFromUrl;

        WalletConfigLoader.loadConfig(configUrl, result -> {
            if (TextUtils.isEmpty(result)) {
                if (onConfigLoadRunnable != null) {
                    onConfigLoadRunnable.run(false);
                }
                return;
            }

            boolean needConfigUpdate = !TextUtils.equals(cachedConfig, result)
                && TextUtils.equals(idCurrentNetworkId, keysConfig.mCurrentNetworkId)
                && TextUtils.equals(configUrl, keysConfig.mCurrentConfigUrl)
                && TextUtils.equals(networkName, keysConfig.mBlockchainName);

            if (needConfigUpdate) {
                keysConfig.edit().updateConfig(idCurrentNetworkId, configUrl, networkName, result).apply();
                onTonConfigUpdated();
            }
            if (onConfigLoadRunnable != null) {
                onConfigLoadRunnable.run(true);
            }
        });
    }

    private void onTonConfigUpdated () {
        if (!initialed) {
            return;
        }
        TonApi.Config config = getConfig();

        Object info = sendRequest(new TonApi.OptionsValidateConfig(config), true);
        if (info instanceof TonApi.OptionsConfigInfo) {
            TonApi.OptionsConfigInfo configInfo = (TonApi.OptionsConfigInfo) info;
            defaultWalletId = configInfo.defaultWalletId;
        } else {
            return;
        }
        sendRequest(new TonApi.OptionsSetConfig(config), false);
    }

    private TonApi.Config getConfig () {
        return new TonApi.Config(keysConfig.mCachedConfigFromUrl, keysConfig.mBlockchainName.toLowerCase(), false, false);
    }



    /* Pending Transactions */

    private WalletTransaction generatePendingTransaction (String fromAddress, long amount, String toAddress, String toDomain, byte[] payload, long validUntil) {
        TonApi.RawMessage inMsg = new TonApi.RawMessage();
        inMsg.source = new TonApi.AccountAddress();
        inMsg.destination = new TonApi.AccountAddress(fromAddress);
        inMsg.value = 0;
        inMsg.bodyHash = new byte[0];
        inMsg.msgData = new TonApi.MsgDataRaw(new byte[0], new byte[0]);

        TonApi.RawMessage outMsg = new TonApi.RawMessage();
        outMsg.source = new TonApi.AccountAddress(fromAddress);
        outMsg.destination = new TonApi.AccountAddress(toAddress);
        outMsg.value = amount;
        outMsg.bodyHash = new byte[0];
        outMsg.msgData = new TonApi.MsgDataRaw(payload != null ? payload : new byte[0], new byte[0]);

        WalletTransaction sendingTransaction = new WalletTransaction(
            new TonApi.RawTransaction(
                null,
                System.currentTimeMillis() / 1000,
                new byte[0],
                new TonApi.InternalTransactionId(0, new byte[32]),
                0,
                0,
                validUntil,
                inMsg,
                new TonApi.RawMessage[]{outMsg}));
        sendingTransaction.outMsgs[0].setOutInfo(toDomain);
        return sendingTransaction;
    }

    private WalletTransaction generatePendingTransaction (String fromAddress, RequestSendTransaction request, long validUntil) {
        String from = TextUtils.isEmpty(request.from) ? request.from : fromAddress;

        TonApi.RawMessage inMsg = new TonApi.RawMessage();
        inMsg.source = new TonApi.AccountAddress();
        inMsg.destination = new TonApi.AccountAddress(from);
        inMsg.value = 0;
        inMsg.bodyHash = new byte[0];
        inMsg.msgData = new TonApi.MsgDataRaw(new byte[0], new byte[0]);

        TonApi.RawMessage[] outMsgs = new TonApi.RawMessage[request.messages.length];
        for (int a = 0; a < outMsgs.length; a++) {
            TonApi.RawMessage outMsg = new TonApi.RawMessage();
            outMsg.source = new TonApi.AccountAddress(from);
            outMsg.destination = new TonApi.AccountAddress(request.messages[a].address);
            outMsg.value = request.messages[a].amount;
            outMsg.bodyHash = new byte[0];
            outMsg.msgData = new TonApi.MsgDataRaw(request.messages[a].payload, request.messages[a].stateInit);
            outMsgs[a] = outMsg;
        }

        return new WalletTransaction(
            new TonApi.RawTransaction(
                null,
                System.currentTimeMillis() / 1000,
                new byte[0],
                new TonApi.InternalTransactionId(0, new byte[32]),
                0,
                0,
                validUntil,
                inMsg,
                outMsgs));
    }



    /* Requests sending */

    private Object sendRequest (TonApi.Function query, boolean sync) {
        return sendRequest(query, null, sync);
    }

    private void sendRequest (TonApi.Function query, Callbacks.TonLibCallback callback) {
        sendRequest(query, callback, false);
    }

    private Object sendRequest (TonApi.Function query, Callbacks.TonLibCallback callback, boolean sync) {
        Object[] result = new Object[1];
        CountDownLatch countDownLatch = sync ? new CountDownLatch(1) : null;
        client.send(query, object -> {
            if (callback != null) {
                callback.run(object);
            } else {
                if (object instanceof TonApi.Error) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.e("TonApi query " + query + " error " + ((TonApi.Error) object).message);
                    }
                    result[0] = object;
                } else {
                    result[0] = object;
                }
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }
        }, null);
        if (countDownLatch != null) {
            try {
                countDownLatch.await();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return result[0];
    }

    @UiThread
    protected void sendRequestAndInit (TonApi.Function query, Callbacks.TonLibCallback callback) {
        initTonLib();
        Utilities.globalQueue.postRunnable(() -> {
            sendRequest(query, result -> AndroidUtilities.runOnUIThread(() -> callback.run(result)));
        });
    }



    /* Private Key */

    public static class UserAuthInfo {
        public final String passcode;
        public final Cipher cipher;
        public final TonApi.InputKey key;

        public UserAuthInfo (String passcode, Cipher cipher, TonApi.InputKey key) {
            this.passcode = passcode;
            this.cipher = cipher;
            this.key = key;
        }
    }

    public void getSecretWords (String passcode, Cipher cipherForDecrypt, Callbacks.WordsCallback onFinishRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        initTonLib();
        Utilities.globalQueue.postRunnable(() -> {
            TonApi.InputKey inputKey = getTonWalletInputKey(passcode, cipherForDecrypt, onErrorRunnable);
            if (inputKey == null) {
                return;
            }
            sendRequest(new TonApi.ExportKey(inputKey), exportedKey -> {
                if (exportedKey instanceof TonApi.ExportedKey) {
                    TonApi.ExportedKey exportKey = (TonApi.ExportedKey) exportedKey;
                    AndroidUtilities.runOnUIThread(() -> onFinishRunnable.run(exportKey.wordList));
                } else {
                    AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("TONLIB_FAIL", Utilities.getTonApiErrorSafe(exportedKey)));
                }
            });
        });
    }

    public void sign (UserAuthInfo auth, byte[] hash, Callbacks.BytesCallback callback, Callbacks.ErrorCallback onErrorRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            final byte[] secretKey = getTonWalletUnencryptedKey(auth, onErrorRunnable);
            if (secretKey == null) return;

            final byte[] signature = new byte[64];
            Utilities.signEd25519hash(hash, secretKey, signature);
            Arrays.fill(secretKey, (byte) 0);

            AndroidUtilities.runOnUIThread(() -> callback.run(signature));
        });
    }

    private TonApi.ExportedKey exportTonWalletKey (TonApi.Key key, byte[] password, Callbacks.ErrorCallback onErrorRunnable) {
        Object exportedKey = sendRequest(new TonApi.ExportKey(new TonApi.InputKeyRegular(key, password)), true);
        if (exportedKey instanceof TonApi.ExportedKey) {
            return (TonApi.ExportedKey) exportedKey;
        } else {
            AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("TONLIB_FAIL", Utilities.getTonApiErrorSafe(exportedKey)));
            return null;
        }
    }

    private TonApi.Key createTonWalletKey (@Nullable String[] words, byte[] password, Callbacks.ErrorCallback onErrorRunnable) {
        Object result;
        if (words == null) {
            result = sendRequest(new TonApi.CreateNewKey(password, new byte[0], Utilities.randomBytes(32)), true);
        } else {
            result = sendRequest(new TonApi.ImportKey(password, new byte[0], new TonApi.ExportedKey(words)), true);
        }
        if (result instanceof TonApi.Key) {
            return (TonApi.Key) result;
        } else {
            AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("TONLIB_FAIL", Utilities.getTonApiErrorSafe(result)));
            return null;
        }
    }

    private TonApi.InputKey getTonWalletInputKey (String passcode, Cipher cipherForDecrypt, Callbacks.ErrorCallback onErrorRunnable) {
        byte[] decrypted = KeysController.decryptTonData(keysConfig.mEncryptedTonWalletData, passcode, keysConfig.mPasscodeSalt, cipherForDecrypt, getAndroidKeyName(false), onErrorRunnable);
        if (decrypted == null) return null;
        return KeysController.getTonKeyFromDecrypted(decrypted, keysConfig.mTonWalletPublicKey);
    }

    private byte[] getTonWalletUnencryptedKey (UserAuthInfo auth, Callbacks.ErrorCallback onErrorRunnable) {
        TonApi.InputKey inputKey = auth.key != null ? auth.key : getTonWalletInputKey(auth.passcode, auth.cipher, onErrorRunnable);
        return getTonWalletUnencryptedKey(inputKey, onErrorRunnable);
    }

    private byte[] getTonWalletUnencryptedKey (TonApi.InputKey inputKey, Callbacks.ErrorCallback onErrorRunnable) {
        Object exportedKey = sendRequest(new TonApi.ExportUnencryptedKey(inputKey), true);
        if (exportedKey instanceof TonApi.ExportedUnencryptedKey) {
            return ((TonApi.ExportedUnencryptedKey) exportedKey).data;
        } else {
            onErrorRunnable.run("EXPORT_KEY_FAIL", Utilities.getTonApiErrorSafe(exportedKey));
            return null;
        }
    }



    /* Transaction send */

    public interface SendTransactionCallbacks {
        void onTransactionCreate ();

        void onTransactionSendSuccess ();

        void onTransactionSendError (String text, TonApi.Error error);
    }

    public static class SendTonArgs {
        public final RootJettonContract rootJettonContract;
        public final String toWallet;
        public final String toDomain;
        public final long amount;
        public final String comment;
        public final int sendMode;

        public SendTonArgs (String toWallet, String toDomain, long amount, String comment, int sendMode, RootJettonContract rootJettonContract) {
            this.toWallet = toWallet;
            this.toDomain = toDomain;
            this.amount = amount;
            this.comment = comment;
            this.sendMode = sendMode;
            this.rootJettonContract = rootJettonContract;
        }
    }

    public void sendTransaction (UserAuthInfo auth, RequestSendTransaction request, SendTransactionCallbacks callbacks) {
        Utilities.globalQueue.postRunnable(() -> {
            final String senderWalletAddress = getCurrentWalletAddress();
            if (!validateTransactionBeforeSend(senderWalletAddress, request, callbacks::onTransactionSendError)) {
                return;
            }

            final int walletVersion = getCurrentWalletType();
            final byte[] publicKey = Utilities.publicKeyToBytes(keysConfig.mTonWalletPublicKey);
            final byte[] secretKey = getTonWalletUnencryptedKey(auth, callbacks::onTransactionSendError);
            if (secretKey == null) return;

            final TonApi.RawInitialAccountState state = Wallets.makeWalletInitialAccountState(walletVersion, publicKey, getCurrentSubWalletId());

            Wallets.Message[] messages = new Wallets.Message[request.messages.length];
            for (int a = 0; a < messages.length; a++) {
                messages[a] = Wallets.makeWalletInternalMessage(
                    request.messages[a].address,
                    request.messages[a].amount,
                    request.messages[a].payload,
                    request.messages[a].stateInit, 1);
            }

            TonApi.RawFullAccountState rawFullAccountState = accountsStateManager.getCachedFullAccountState(senderWalletAddress);
            if (rawFullAccountState == null) {
                callbacks.onTransactionSendError("WRONG_ACCOUNT_STATE", null);
                return;
            }

            long estimatedFee = 15000000;
            if (rawFullAccountState.balance < request.totalAmount) {
                callbacks.onTransactionSendError("INSUFFICIENT_FUNDS", null);
                return;
            } else if (rawFullAccountState.balance < request.totalAmount + estimatedFee) {
                callbacks.onTransactionSendError("INSUFFICIENT_FUNDS_TO_PAY_COMMISSION", null);
                return;
            }

            final long seqno = Wallets.getSeqnoFromDataCell(rawFullAccountState.data);
            final long validUntil = System.currentTimeMillis() / 1000 + 90;
            byte[] bodyData = Wallets.makeWalletExternalMessageBodyData(getCurrentSubWalletId(), validUntil, seqno, walletVersion == Wallets.WALLET_TYPE_V4R2, messages);
            byte[] body = Wallets.makeExternalMessageBody(secretKey, bodyData);
            Arrays.fill(secretKey, (byte) 0);
            sendTransactionImpl(senderWalletAddress, state, body, generatePendingTransaction(senderWalletAddress, request, validUntil), callbacks);
        });
    }

    public void sendTransaction (UserAuthInfo auth, SendTonArgs args, SendTransactionCallbacks callbacks) {
        final String senderWalletAddress = getCurrentWalletAddress();
        final String realSendAddress = args.rootJettonContract != null ?
            accountsStateManager.getCachedJettonWalletAddress(senderWalletAddress, args.rootJettonContract.address): args.toWallet;
        final long realSendAmount = args.rootJettonContract != null ? 200000000: args.amount;
        final int realSendMode = args.rootJettonContract != null ? 1: args.sendMode;

        if (realSendAddress == null) {
            callbacks.onTransactionSendError("UNKNOWN_ADDRESS", null);
            return;
        }

        accountsStateManager.getRecentRecipientsStorage().add(args.toWallet, args.toDomain);
        Utilities.globalQueue.postRunnable(() -> {
            final int walletVersion = getCurrentWalletType();
            final byte[] publicKey = Utilities.publicKeyToBytes(keysConfig.mTonWalletPublicKey);
            final byte[] secretKey = getTonWalletUnencryptedKey(auth, callbacks::onTransactionSendError);
            if (secretKey == null) return;

            final byte[] msgBody;
            if (args.rootJettonContract != null) {
                final byte[] payloadComment = !TextUtils.isEmpty(args.comment) ? Wallets.makeWalletInternalMessageCommentBody(args.comment) : null;
                msgBody = Wallets.makeWalletInternalMessageJettonTransfer(args.amount, args.toWallet, senderWalletAddress, null, payloadComment != null ? 20000000: 0, payloadComment);
            } else if (!TextUtils.isEmpty(args.comment)) {
                msgBody = Wallets.makeWalletInternalMessageCommentBody(args.comment);
            } else {
                msgBody = null;
            }

            final TonApi.RawInitialAccountState state = Wallets.makeWalletInitialAccountState(walletVersion, publicKey, getCurrentSubWalletId());

            TonApi.RawFullAccountState rawFullAccountState = accountsStateManager.getCachedFullAccountState(senderWalletAddress);
            if (rawFullAccountState == null) {
                callbacks.onTransactionSendError("WRONG_ACCOUNT_STATE", null);
                return;
            }

            if (realSendMode != 128) {
                long estimatedFee = 15000000;
                if (rawFullAccountState.balance < realSendAmount) {
                    callbacks.onTransactionSendError("INSUFFICIENT_FUNDS", null);
                    return;
                } else if (rawFullAccountState.balance < realSendAmount + estimatedFee) {
                    callbacks.onTransactionSendError("INSUFFICIENT_FUNDS_TO_PAY_COMMISSION", null);
                    return;
                }
            }

            final long seqno = Wallets.getSeqnoFromDataCell(rawFullAccountState.data);
            final long validUntil = System.currentTimeMillis() / 1000 + 90;
            byte[] bodyData = Wallets.makeWalletExternalMessageBodyData(getCurrentSubWalletId(), validUntil, seqno, walletVersion == Wallets.WALLET_TYPE_V4R2, new Wallets.Message[]{
                Wallets.makeWalletInternalMessage(realSendAddress, realSendAmount, msgBody, null, realSendMode)
            });
            byte[] body = Wallets.makeExternalMessageBody(secretKey, bodyData);
            Arrays.fill(secretKey, (byte) 0);
            sendTransactionImpl(senderWalletAddress, state, body, generatePendingTransaction(senderWalletAddress, realSendAmount, realSendAddress, args.toDomain, msgBody, validUntil), callbacks);
        });
    }

    private void sendTransactionImpl (String senderAddress, TonApi.RawInitialAccountState initState, byte[] body, WalletTransaction pendingTransactionToAdd, SendTransactionCallbacks callbacks) {
        final TonApi.RawCreateQuery req = new TonApi.RawCreateQuery(new TonApi.AccountAddress(senderAddress), initState.code, initState.data, body);
        queryCreate(req, queryInfo -> querySend(queryInfo.id, () -> {
            pendingTransactionToAdd.inMsg.msg.bodyHash = queryInfo.bodyHash;
            accountsStateManager.subscribeToPendingTransactionComplete(senderAddress, pendingTransactionToAdd, callbacks::onTransactionSendSuccess);
            accountsStateManager.addPendingTransaction(senderAddress, pendingTransactionToAdd);
            callbacks.onTransactionCreate();
            queryForget(queryInfo.id);
        }, error -> {
            callbacks.onTransactionSendError("TONLIB_FAIL", Utilities.getTonApiErrorSafe(error));
            queryForget(queryInfo.id);
        }), error -> onCreateTransactionError(error, callbacks::onTransactionSendError));
    }

    private boolean validateTransactionBeforeSend (String senderWalletAddress, RequestSendTransaction request, Callbacks.ErrorCallback callback) {
        if (request.network != null && !TextUtils.equals(request.network, getNetworkId())) {
            callback.run("WRONG_NETWORK_ID", null);
            return false;
        }
        if (request.from != null && !TextUtils.equals(request.from, senderWalletAddress)) {
            callback.run("WRONG_WALLET", null);
            return false;
        }

        return true;
    }

    private void onCreateTransactionError (Object rawError, Callbacks.ErrorCallback onErrorRunnable) {
        TonApi.Error error = Utilities.getTonApiErrorSafe(rawError);
        onErrorRunnable.run("TONLIB_FAIL", error);
    }



    /* Passcode Protection */

    public int getPasscodeLength () {
        return keysConfig.mPasscodeLength;
    }

    public long getPasscodeRepeatAttemptBanTime () {
        final int wrongAttempts = keysConfig.mPasscodeInputWrongAttempts;
        final long banTime = (wrongAttempts < 3) ? 0 : (5000 * Math.min(wrongAttempts - 2, 12));
        return (keysConfig.mPasscodeInputLastAttemptTime + banTime) - System.currentTimeMillis();
    }

    public void checkPasscode (String passcode, Runnable onFinishRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            boolean correct = KeysController.checkPasscode(passcode, keysConfig.mEncryptedPasscodeData, keysConfig.mPasscodeSalt, getAndroidKeyName(true), onErrorRunnable);
            AndroidUtilities.runOnUIThread(() -> {
                if (!correct) {
                    keysConfig.edit().updatePasscodeAttempts(System.currentTimeMillis(), keysConfig.mPasscodeInputWrongAttempts + 1).apply();
                    onErrorRunnable.run("PASSCODE_INVALID", null);
                    return;
                } else {
                    keysConfig.edit().updatePasscodeAttempts(System.currentTimeMillis(), 0).apply();
                }
                onFinishRunnable.run();
            });
        });
    }

    public void setupUserPasscode (String passcode, Runnable onFinishRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            byte[] unencryptedTonData = (getKeyProtectionType() == KEY_PROTECTION_TYPE_NONE) ? keysConfig.tmpDecryptedTonWalletData : null;
            KeysController.PasscodeCreateResult result = KeysController.createAndEncryptPasscode(passcode, getAndroidKeyName(true), unencryptedTonData);
            String passcodeSalt = Base64.encodeToString(result.salt, Base64.DEFAULT);

            KeysConfig.Editor editor = keysConfig.edit();
            editor.updatePasscode(passcode.length(), passcodeSalt, result.encryptedPasscode);
            if (unencryptedTonData != null) {
                editor.updateEncryptedData(result.encryptedTonData);
            }
            editor.apply();
            AndroidUtilities.runOnUIThread(onFinishRunnable);
        });
    }

    public void prepareForPasscodeChange (String passcode, Runnable onFinishRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            if (getKeyProtectionType() == KEY_PROTECTION_TYPE_NONE) {
                byte[] decrypted = KeysController.decryptTonData(keysConfig.mEncryptedTonWalletData, passcode, keysConfig.mPasscodeSalt, null, getAndroidKeyName(false), onErrorRunnable);
                if (decrypted == null) return;
                keysConfig.tmpDecryptedTonWalletData = decrypted;
            }
            AndroidUtilities.runOnUIThread(onFinishRunnable);
        });
    }



    /* Biometric Protection */

    public void setupBiometricAuth (String passcode, Runnable successRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            byte[] decrypted = keysConfig.tmpDecryptedTonWalletData != null ? keysConfig.tmpDecryptedTonWalletData :
                KeysController.decryptTonData(keysConfig.mEncryptedTonWalletData, passcode, keysConfig.mPasscodeSalt, null, getAndroidKeyName(false), onErrorRunnable);
            if (decrypted == null) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("NOTHING_TO_ENCRYPT", null));
                return;
            }

            String oldBiometricKeyName = keysConfig.mKeyNameBiometric;
            String newBiometricKeyName = "walletKey_biometric" + Utilities.random.nextLong();

            if (!KeysController.isKeyCreated(newBiometricKeyName, true)) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL", null));
                return;
            }

            keysConfig.edit()
                .updateKeyNames(keysConfig.mKeyNamePasscode, newBiometricKeyName)
                .updateEncryptedData(KeysController.encrypt(decrypted, newBiometricKeyName))
                .apply();

            if (oldBiometricKeyName != null) {
                KeysController.deleteKey(oldBiometricKeyName);
            }
            AndroidUtilities.runOnUIThread(successRunnable);
        });
    }

    public void disableBiometricAuth (Cipher cipherForDecrypt, String passcodeToSet, Runnable onFinishRunnable, Callbacks.ErrorCallback onErrorRunnable) {
        Utilities.globalQueue.postRunnable(() -> {
            byte[] decrypted = KeysController.decryptTonData(keysConfig.mEncryptedTonWalletData, null, keysConfig.mPasscodeSalt, cipherForDecrypt, getAndroidKeyName(false), onErrorRunnable);
            if (decrypted == null) return;

            KeysController.PasscodeCreateResult result = KeysController.createAndEncryptPasscode(passcodeToSet, getAndroidKeyName(true), decrypted);

            int passcodeLength = passcodeToSet.length();
            String passcodeSalt = Base64.encodeToString(result.salt, Base64.DEFAULT);

            keysConfig.edit()
                .updateKeyNames(keysConfig.mKeyNamePasscode, null)
                .updatePasscode(passcodeLength, passcodeSalt, result.encryptedPasscode)
                .updateEncryptedData(result.encryptedTonData)
                .apply();

            AndroidUtilities.runOnUIThread(onFinishRunnable);
        });
    }



    /* Wallets */

    public String getWalletAddressFromType (int type) {
        return Wallets.getAddressFromInitialAccountState(0, getWalletInitAccountState(type));
    }

    public final TonApi.RawInitialAccountState getWalletInitAccountState (int type) {
        final byte[] publicKey = Utilities.publicKeyToBytes(keysConfig.mTonWalletPublicKey);
        return Wallets.makeWalletInitialAccountState(type, publicKey, getCurrentSubWalletId());
    }

    public final byte[] getWalletInitAccountStateRaw () {
        return Wallets.makeInitialAccountState(Wallets.makeWalletInitialAccountState(getCurrentWalletType(), getPublicKey(), getCurrentSubWalletId()));
    }



    /* Jetton Controller */

    public void fetchJettonToken (String address, Callbacks.RootJettonContractCallback callback, Callbacks.ErrorCallback onErrorCallback) {
        getJettonDataRawImpl(address, smcResult -> RootJettonContract.valueOf(address, smcResult, callback, onErrorCallback));
    }

    private void getJettonDataRawImpl (String address, Callbacks.SmcRunResultCallback callback) {
        smartContractLoad(address, contractId -> {
            if (contractId == -1) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
                return;
            }

            smartContractCallMethod(contractId, "get_jetton_data", smcResult -> {
                callback.run(smcResult);
                smartContractForget(contractId);
            });
        });
    }

    protected void getJettonWalletAddress (String rootTokenAddress, String walletAddress, Callbacks.StringCallback callback) {
        smartContractLoad(rootTokenAddress, contractId -> {
            if (contractId == -1) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
                return;
            }

            smartContractCallMethod(contractId, "get_wallet_address", new TonApi.TvmStackEntry[]{
                new TonApi.TvmStackEntrySlice(new TonApi.TvmSlice(Wallets.makeAddressSlice(walletAddress)))
            }, smcResult -> {
                if (smcResult == null || smcResult.exitCode != 0 || smcResult.stack.length != 1) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(null));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.run(Utilities.getAddressFromTmvCell(smcResult.stack[0])));
                }
                smartContractForget(contractId);
            });
        });
    }

    protected void getJettonWalletBalance (String jettonWalletBalance, Callbacks.LongValuedCallback callback) {
        smartContractLoad(jettonWalletBalance, contractId -> {
            if (contractId == -1) {
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
                return;
            }

            smartContractCallMethod(contractId, "get_wallet_data", smcResult -> {
                if (smcResult != null && smcResult.exitCode == -13) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(0L));
                } else if (smcResult == null || smcResult.exitCode != 0 || smcResult.stack.length != 4 && smcResult.stack[0] instanceof TonApi.TvmStackEntryNumber) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(null));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.run(Utilities.parseLong(((TonApi.TvmStackEntryNumber)smcResult.stack[0]).number.number)));
                }
                smartContractForget(contractId);
            });
        });
    }



    /* TonLib Query */

    public void queryGetSendFee (String toAddress, long amount, String comment, Callbacks.LongCallback feeCallback) {
        TonApi.MsgMessage[] msgMessages = new TonApi.MsgMessage[]{
            new TonApi.MsgMessage(new TonApi.AccountAddress(toAddress), null, amount, new TonApi.MsgDataText(comment.getBytes()), 1)
        };
        TonApi.ActionMsg actionMsg = new TonApi.ActionMsg(msgMessages, true);
        TonApi.CreateQuery req = new TonApi.CreateQuery(new TonApi.InputKeyFake(), new TonApi.AccountAddress(getCurrentWalletAddress()), 0, actionMsg, new TonApi.WalletV3InitialAccountState(keysConfig.mTonWalletPublicKey, getCurrentSubWalletId()));


        // crutch. todo: fix this when tonlib updated
        if (getCurrentWalletType() == Wallets.WALLET_TYPE_V4R2) {
            TonApi.RawFullAccountState rawFullAccountState = accountsStateManager.getCachedFullAccountState(getCurrentWalletAddress());
            long fee = 1345381L
                + (!StringUtils.isEmpty(comment) ? comment.getBytes().length * 9512L: 0)
                + ((rawFullAccountState == null || Utilities.isEmpty(rawFullAccountState.code)) ? 7865425L: 0);
            AndroidUtilities.runOnUIThread(() -> {
                feeCallback.run(fee);
            });
        } else {
            queryGetSendFee(req, feeCallback);
        }
    }

    public void queryGetSendFee (RequestSendTransaction.MessageRequest[] messages, Callbacks.LongCallback feeCallback) {
        TonApi.MsgMessage[] msgMessages = new TonApi.MsgMessage[messages.length];

        for (int a = 0; a < messages.length; a++) {
            RequestSendTransaction.MessageRequest message = messages[a];
            TonApi.MsgData msgData = new TonApi.MsgDataRaw(
                message.payload != null ? message.payload : CellBuilder.beginCell().toBoc(false, true, false), message.stateInit
            );
            msgMessages[a] = new TonApi.MsgMessage(new TonApi.AccountAddress(message.address), null, message.amount, msgData, 1);
        }
        TonApi.ActionMsg actionMsg = new TonApi.ActionMsg(msgMessages, true);
        TonApi.CreateQuery req = new TonApi.CreateQuery(new TonApi.InputKeyFake(), new TonApi.AccountAddress(getCurrentWalletAddress()), 0, actionMsg, new TonApi.WalletV3InitialAccountState(keysConfig.mTonWalletPublicKey, getCurrentSubWalletId()));
        queryGetSendFee(req, feeCallback);
    }

    private void queryGetSendFee (TonApi.Function req, Callbacks.LongCallback feeCallback) {
        queryCreate(req, queryInfo -> {
            if (queryInfo == null) {
                feeCallback.run(-1);
                return;
            }
            queryEstimateFees(queryInfo.id, fee -> {
                feeCallback.run(fee);
                queryForget(queryInfo.id);
            });
        });
    }

    private void queryCreate (TonApi.Function function, Callbacks.QueryInfoCallback callback) {
        queryCreate(function, callback, err -> callback.run(null));
    }

    private void queryCreate (TonApi.Function function, Callbacks.QueryInfoCallback callback, Callbacks.TonLibCallback errorCallback) {
        Utilities.globalQueue.postRunnable(() -> sendRequest(function, res -> {
            if (res instanceof TonApi.QueryInfo) {
                AndroidUtilities.runOnUIThread(() -> callback.run((TonApi.QueryInfo) res));
            } else {
                AndroidUtilities.runOnUIThread(() -> errorCallback.run(res));
            }
        }));
    }

    private void querySend (long queryId, Runnable callback, Callbacks.TonLibCallback errorCallback) {
        Utilities.globalQueue.postRunnable(() -> sendRequest(new TonApi.QuerySend(queryId), res -> {
            if (res instanceof TonApi.Ok) {
                AndroidUtilities.runOnUIThread(callback);
            } else {
                AndroidUtilities.runOnUIThread(() -> errorCallback.run(res));
            }
        }));
    }

    private void queryEstimateFees (long queryId, Callbacks.LongCallback callback) {
        Utilities.globalQueue.postRunnable(() -> sendRequest(new TonApi.QueryEstimateFees(queryId, true), res -> {
            if (res instanceof TonApi.QueryFees) {
                TonApi.QueryFees queryFees = (TonApi.QueryFees) res;
                long fee = queryFees.sourceFees.fwdFee + queryFees.sourceFees.gasFee + queryFees.sourceFees.inFwdFee + queryFees.sourceFees.storageFee;
                for (int a = 0; a < queryFees.destinationFees.length; a++) {
                    fee +=
                        queryFees.destinationFees[a].fwdFee +
                        queryFees.destinationFees[a].gasFee +
                        queryFees.destinationFees[a].inFwdFee +
                        queryFees.destinationFees[a].storageFee;
                }

                long finalFee = fee;
                AndroidUtilities.runOnUIThread(() -> callback.run(finalFee));
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(-1));
            }
        }));
    }

    private void queryForget (long queryId) {
        Utilities.globalQueue.postRunnable(() -> sendRequest(new TonApi.QueryForget(queryId), null));
    }



    /* TonLib Smart contracts */

    private void smartContractLoad (String address, Callbacks.LongCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            TonApi.SmcLoad req = new TonApi.SmcLoad(new TonApi.AccountAddress(address));
            sendRequest(req, res -> {
                if (res instanceof TonApi.SmcInfo) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(((TonApi.SmcInfo) res).id));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.run(-1));
                }
            });
        });
    }

    private void smartContractCallMethod (long contractId, String methodName, Callbacks.SmcRunResultCallback callback) {
        smartContractCallMethod(contractId, methodName, new TonApi.TvmStackEntry[0], callback);
    }

    private void smartContractCallMethod (long contractId, String methodName, TonApi.TvmStackEntry[] stack, Callbacks.SmcRunResultCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            TonApi.SmcRunGetMethod req = new TonApi.SmcRunGetMethod(contractId, new TonApi.SmcMethodIdName(methodName), stack);
            sendRequest(req, res -> {
                if (res instanceof TonApi.SmcRunResult) {
                    AndroidUtilities.runOnUIThread(() -> callback.run((TonApi.SmcRunResult) res));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.run(null));
                }
            });
        });
    }

    private void smartContractForget (long contractId) {
        Utilities.globalQueue.postRunnable(() -> sendRequest(new TonApi.SmcForget(contractId), null));
    }



    /* TonLib DNS */

    public void resolveDns (String domain, Callbacks.DnsResolvedResultCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            TonApi.DnsResolve dnsResolve = new TonApi.DnsResolve(new TonApi.AccountAddress("Ef-OJd0IF0yc0xkhgaAirq12WawqnUoSuE9RYO3S7McG6lDh"), domain, new byte[32], 10);
            sendRequest(dnsResolve, resultGet -> {
                if (resultGet instanceof TonApi.DnsResolved) {
                    AndroidUtilities.runOnUIThread(() -> callback.run((TonApi.DnsResolved) resultGet));
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.run(null));
                }
            });
        });
    }

    public void resolveDnsWallet (String domain, Callbacks.StringCallback callback) {
        resolveDns(domain, dnsResolved -> {
            if (dnsResolved == null) {
                callback.run(null);
                return;
            }

            for (int a = 0; a < dnsResolved.entries.length; a++) {
                TonApi.DnsEntry dnsEntry = dnsResolved.entries[a];
                if (dnsEntry.entry instanceof TonApi.DnsEntryDataSmcAddress) {
                    callback.run(((TonApi.DnsEntryDataSmcAddress) dnsEntry.entry).smcAddress.accountAddress);
                    return;
                }
            }

            callback.run(null);
        });
    }



    /* * */

    public static class GuaranteedReceiver {
        private final ArrayList<Pair<TonApi.Function, Callbacks.TonLibCallback>> queue = new ArrayList<>();
        private boolean loading = false;
        private final int constructor;

        public GuaranteedReceiver (int constructor) {
            this.constructor = constructor;
        }

        public void receive (TonApi.Function req, Callbacks.TonLibCallback callback) {
            queue.add(new Pair<>(req, callback));
            if (loading) return;
            loading = true;

            receiveImpl(queue.get(0));
        }

        private void receiveImpl (Pair<TonApi.Function, Callbacks.TonLibCallback> req) {
            TonController.getInstance(Utilities.selectedAccount).sendRequestAndInit(req.first, result -> {
                if (result instanceof TonApi.Object) {
                    if (((TonApi.Object) result).getConstructor() == constructor) {
                        req.second.run(result);
                        queue.remove(0);
                        if (queue.isEmpty()) {
                            loading = false;
                        } else {
                            receiveImpl(queue.get(0));
                        }
                        return;
                    }
                }

                TonApi.Error error = Utilities.getTonApiErrorSafe(result);
                Log.w("GuaranteedReceiver", "Failed to get: " + req.first.toString().replaceAll("\n", "") + " " + (error != null ? error.message: "-"));
                AndroidUtilities.runOnUIThread(() -> receiveImpl(req), 500);
            });
        }
    }
}
