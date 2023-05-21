package org.TonController;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.TonController.Data.Wallets;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;

class KeysConfig {
    public static final String NETWORK_TYPE_MAINNET = "-239";
    public static final String NETWORK_TYPE_TESTNET = "-3";

    private final SharedPreferences preferences;

    public int mPasscodeLength;
    public String mTonConnectSecretKey;
    public String mTonConnectPublicKey;
    public String mPasscodeSalt;
    public String mEncryptedPasscodeData;
    public String mEncryptedTonWalletData;
    public String mTonWalletPublicKey;
    public String mKeyNameBiometric;
    public String mKeyNamePasscode;

    public String mTonWalletAddress;
    public String mJettonRootAddress;
    public int mTonWalletType;
    public long mTonWalletId;

    public String mBlockchainName;
    public String mCurrentNetworkId;
    public String mCurrentConfigUrl;
    public String mCachedConfigFromUrl;

    public String mFirebaseFcmToken;
    public boolean mNotificationsEnabled;

    public long mPasscodeInputLastAttemptTime;
    public int mPasscodeInputWrongAttempts;

    public byte[] tmpDecryptedTonWalletData;

    public KeysConfig () {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("keys_config", Context.MODE_PRIVATE);

        mPasscodeLength = preferences.getInt("passcode_length", -1);
        mPasscodeSalt = preferences.getString("passcode_salt", null);
        mEncryptedPasscodeData = preferences.getString("encrypted_passcode_data", null);
        mEncryptedTonWalletData = preferences.getString("encrypted_wallet_data", null);
        mTonWalletPublicKey = preferences.getString("ton_wallet_public_key", null);
        mKeyNameBiometric = preferences.getString("stored_key_biometric", null);
        mKeyNamePasscode = preferences.getString("stored_key_passcode", null);
        mTonWalletAddress = preferences.getString("ton_wallet_address", null);
        mJettonRootAddress = preferences.getString("jetton_root_address", null);
        mTonConnectSecretKey = preferences.getString("ton_connect_secret_key", null);
        mTonConnectPublicKey = preferences.getString("ton_connect_public_key", null);
        mTonWalletType = preferences.getInt("ton_wallet_type", Wallets.WALLET_TYPE_V3R2);
        mTonWalletId = preferences.getLong("ton_wallet_id", 0);
        mCurrentNetworkId = preferences.getString("network_id", BuildVars.USE_TESTNET ? NETWORK_TYPE_TESTNET: NETWORK_TYPE_MAINNET);
        mBlockchainName = preferences.getString("network_name", BuildVars.USE_TESTNET ? "testnet": "mainnet");
        mCurrentConfigUrl = preferences.getString("network_config_url_", BuildVars.USE_TESTNET ?
            "https://ton-blockchain.github.io/testnet-global.config.json":
            "https://ton-blockchain.github.io/global.config.json"
        );
        mPasscodeInputLastAttemptTime = preferences.getLong("passcode_last_attempt_time", 0);
        mPasscodeInputWrongAttempts = preferences.getInt("passcode_wrong_attempts", 0);
        mFirebaseFcmToken = preferences.getString("firebase_fcm_token", null);
        mNotificationsEnabled = preferences.getBoolean("notifications_enabled", false);
        mCachedConfigFromUrl = preferences.getString("network_config", null);
    }

    public void clear () {
        preferences.edit()
            .remove("passcode_length")
            .remove("passcode_salt")
            .remove("encrypted_passcode_data")
            .remove("encrypted_wallet_data")
            .remove("ton_wallet_public_key")
            .remove("stored_key_biometric")
            .remove("stored_key_passcode")
            .remove("ton_wallet_address")
            .remove("jetton_root_address")
            .remove("ton_connect_secret_key")
            .remove("ton_connect_public_key")
            .remove("ton_wallet_type")
            .remove("ton_wallet_id")
            .remove("passcode_last_attempt_time")
            .remove("passcode_wrong_attempts")
            .remove("firebase_fcm_token")
            .remove("notifications_enabled")
            .apply();
        mPasscodeLength = -1;
        mPasscodeSalt = null;
        mEncryptedPasscodeData = null;
        mTonWalletPublicKey = null;
        mEncryptedTonWalletData = null;
        mKeyNameBiometric = null;
        mKeyNamePasscode = null;
        mTonWalletAddress = null;
        mJettonRootAddress = null;
        mTonConnectSecretKey = null;
        mTonConnectPublicKey = null;
        mTonWalletType = Wallets.WALLET_TYPE_V3R2;
        mTonWalletId = 0;
        mPasscodeInputLastAttemptTime = 0;
        mPasscodeInputWrongAttempts = 0;
        mFirebaseFcmToken = null;
        mNotificationsEnabled = false;
    }

    public Editor edit () {
        return new Editor(this);
    }

    public static class Editor {
        private final KeysConfig config;

        private int updatedPasscodeLength = -1;
        private String updatedPasscodeSalt;
        private String updatedEncryptedPasscodeData;
        private String updatedEncryptedTonWalletData;
        private String updatedTonWalletPublicKey;
        private boolean keysUpdated;
        private String updatedKeyNameBiometric;
        private String updatedKeyNamePasscode;
        private String updatedTonWalletAddress;
        private String updatedJettonRootAddress;
        private boolean isUpdatedJettonRootAddress;
        private String updatedTonConnectPublicKey;
        private String updatedTonConnectSecretKey;
        private int updatedTonWalletType;
        private long updatedTonWalletId;
        private String updatedCurrentNetworkId;
        private String updatedCurrentConfigUrl;
        private String updatedCachedConfigFromUrl;
        private String updatedBlockchainName;

        private String updatedFirebaseFcmToken;
        private boolean updatedNotificationsEnabled;

        private long updatedPasscodeInputLastAttemptTime;
        private int updatedPasscodeInputWrongAttempts;

        public Editor (KeysConfig config) {
            this.config = config;
        }

        public Editor updateFirebaseFcmToken (String token, boolean notificationsEnabled) {
            this.updatedFirebaseFcmToken = token;
            this.updatedNotificationsEnabled = notificationsEnabled;
            return this;
        }

        public Editor updatePasscodeAttempts (long attemptTime, int wrongAttemptsCount) {
            this.updatedPasscodeInputLastAttemptTime = attemptTime;
            this.updatedPasscodeInputWrongAttempts = wrongAttemptsCount;
            return this;
        }

        public Editor updateConfig (String networkId, String configUrl, String blockchainName, @Nullable String cachedConfig) {
            this.updatedCurrentNetworkId = networkId;
            this.updatedCurrentConfigUrl = configUrl;
            this.updatedBlockchainName = blockchainName;
            this.updatedCachedConfigFromUrl = cachedConfig;
            return this;
        }

        public Editor updatePasscode (int passcodeLength, String passcodeSalt, String passcodeData) {
            this.updatedPasscodeLength = passcodeLength;
            this.updatedPasscodeSalt = passcodeSalt;
            this.updatedEncryptedPasscodeData = passcodeData;
            return this;
        }

        public Editor updateTonConnectKeypair (String secretKey, String publicKey) {
            this.updatedTonConnectSecretKey = secretKey;
            this.updatedTonConnectPublicKey = publicKey;
            return this;
        }

        public Editor updateEncryptedData (String encryptedTonWalletData) {
            this.updatedEncryptedTonWalletData = encryptedTonWalletData;
            return this;
        }

        public Editor updateKeyNames (String keyNamePasscode, String keyNameBiometric) {
            this.updatedKeyNamePasscode = keyNamePasscode;
            this.updatedKeyNameBiometric = keyNameBiometric;
            this.keysUpdated = true;
            return this;
        }

        public Editor updatePublicKey (String publicKey) {
            this.updatedTonWalletPublicKey = publicKey;
            return this;
        }

        public Editor updateWalletAddressAndType (String address, int type, long subWalletId) {
            this.updatedTonWalletAddress = address;
            this.updatedTonWalletType = type;
            this.updatedTonWalletId = subWalletId;
            return this;
        }

        public Editor updateRootJettonAddress (@Nullable String address) {
            this.updatedJettonRootAddress = address;
            this.isUpdatedJettonRootAddress = true;
            return this;
        }

        public void apply () {
            synchronized (KeysConfig.class) {
                SharedPreferences.Editor editor = config.preferences.edit();
                if (updatedPasscodeLength != -1) {
                    editor.putInt("passcode_length", config.mPasscodeLength = updatedPasscodeLength);
                    editor.putString("passcode_salt", config.mPasscodeSalt = updatedPasscodeSalt);
                    editor.putString("encrypted_passcode_data", config.mEncryptedPasscodeData = updatedEncryptedPasscodeData);
                }
                if (updatedEncryptedTonWalletData != null) {
                    editor.putString("encrypted_wallet_data", config.mEncryptedTonWalletData = updatedEncryptedTonWalletData);
                }
                if (updatedTonWalletPublicKey != null) {
                    editor.putString("ton_wallet_public_key", config.mTonWalletPublicKey = updatedTonWalletPublicKey);
                }
                if (keysUpdated) {
                    editor.putString("stored_key_biometric", config.mKeyNameBiometric = updatedKeyNameBiometric);
                    editor.putString("stored_key_passcode", config.mKeyNamePasscode = updatedKeyNamePasscode);
                }
                if (isUpdatedJettonRootAddress) {
                    config.mJettonRootAddress = updatedJettonRootAddress;
                    if (updatedJettonRootAddress != null) {
                        editor.putString("jetton_root_address", updatedJettonRootAddress);
                    } else {
                        editor.remove("jetton_root_address");
                    }
                }
                if (updatedTonWalletAddress != null) {
                    editor.putString("ton_wallet_address", config.mTonWalletAddress = updatedTonWalletAddress);
                    editor.putInt("ton_wallet_type", config.mTonWalletType = updatedTonWalletType);
                    editor.putLong("ton_wallet_id", config.mTonWalletId = updatedTonWalletId);
                }
                if (updatedTonConnectSecretKey != null) {
                    editor.putString("ton_connect_secret_key", config.mTonConnectSecretKey = updatedTonConnectSecretKey);
                    editor.putString("ton_connect_public_key", config.mTonConnectPublicKey = updatedTonConnectPublicKey);
                }
                if (updatedCurrentNetworkId != null) {
                    editor.putString("network_id", config.mCurrentNetworkId = updatedCurrentNetworkId);
                    editor.putString("network_config_url_", config.mCurrentConfigUrl = updatedCurrentConfigUrl);
                    editor.putString("network_config", config.mCachedConfigFromUrl = updatedCachedConfigFromUrl);
                    editor.putString("network_name", config.mBlockchainName = updatedBlockchainName);
                }
                if (updatedPasscodeInputLastAttemptTime != 0) {
                    editor.putLong("passcode_last_attempt_time", config.mPasscodeInputLastAttemptTime = updatedPasscodeInputLastAttemptTime);
                    editor.putInt("passcode_wrong_attempts", config.mPasscodeInputWrongAttempts = updatedPasscodeInputWrongAttempts);
                }
                if (updatedFirebaseFcmToken != null) {
                    editor.putString("firebase_fcm_token", config.mFirebaseFcmToken = updatedFirebaseFcmToken);
                    editor.putBoolean("notifications_enabled", config.mNotificationsEnabled = updatedNotificationsEnabled);
                }
                editor.apply();
            }
        }
    }
}
