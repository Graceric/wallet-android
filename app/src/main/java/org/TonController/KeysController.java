package org.TonController;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.Utils.Callbacks;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.security.auth.x500.X500Principal;

import drinkless.org.ton.TonApi;

class KeysController {
    public final static int CIPHER_INIT_FAILED = 0;
    public final static int CIPHER_INIT_OK = 1;
    public final static int CIPHER_INIT_KEY_INVALIDATED = 2;

    private static KeyPairGenerator keyGenerator;
    private static KeyStore keyStore;
    private static Cipher cipher;

    static {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (Build.VERSION.SDK_INT >= 23) {
                keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            } else {
                keyGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isReady () {
        return cipher != null;
    }

    public static int initCipher (int mode, String keyName) {
        try {
            switch (mode) {
                case Cipher.ENCRYPT_MODE: {
                    PublicKey key = keyStore.getCertificate(keyName).getPublicKey();
                    PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(new X509EncodedKeySpec(key.getEncoded()));
                    if (Build.VERSION.SDK_INT >= 23) {
                        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
                        cipher.init(mode, unrestricted, spec);
                    } else {
                        cipher.init(mode, unrestricted);
                    }
                    break;
                }
                case Cipher.DECRYPT_MODE: {
                    PrivateKey key = (PrivateKey) keyStore.getKey(keyName, null);
                    if (Build.VERSION.SDK_INT >= 23) {
                        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
                        cipher.init(mode, key, spec);
                    } else {
                        cipher.init(mode, key);
                    }
                    break;
                }
                default:
                    return CIPHER_INIT_FAILED;
            }
            return CIPHER_INIT_OK;
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (e instanceof KeyPermanentlyInvalidatedException) {
                    return CIPHER_INIT_KEY_INVALIDATED;
                }
            } else if (e instanceof InvalidKeyException) {
                try {
                    if (!keyStore.containsAlias(keyName)) {
                        return CIPHER_INIT_KEY_INVALIDATED;
                    }
                } catch (Exception ignore) {

                }
            }
            if (e instanceof UnrecoverableKeyException) {
                return CIPHER_INIT_KEY_INVALIDATED;
            }
            FileLog.e(e);
        }
        return CIPHER_INIT_FAILED;
    }

    public static boolean createKeyPair (String keyName, boolean useBiometricAuth) {
        if (Build.VERSION.SDK_INT >= 23) {
            for (int a = 0; a < 2; a++) {
                try {
                    KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setKeySize(2048);
                    if (a == 0 && Build.VERSION.SDK_INT >= 28) {
                        builder.setIsStrongBoxBacked(true);
                    }
                    KeyguardManager keyguardManager = (KeyguardManager) ApplicationLoader.applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
                    if (keyguardManager.isDeviceSecure() && useBiometricAuth) {
                        builder.setUserAuthenticationRequired(true);
                        if (Build.VERSION.SDK_INT >= 24) {
                            builder.setInvalidatedByBiometricEnrollment(true);
                        }
                    }
                    keyGenerator.initialize(builder.build());
                    keyGenerator.generateKeyPair();
                    return true;
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        } else {
            try {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ApplicationLoader.applicationContext)
                    .setAlias(keyName)
                    .setSubject(new X500Principal("CN=Telegram, O=Telegram C=UAE"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();
                keyGenerator.initialize(spec);
                keyGenerator.generateKeyPair();
                return true;
            } catch (Throwable ignore) {

            }
        }
        return false;
    }

    public static boolean isKeyCreated (String keyName, boolean useBiometricAuth) {
        try {
            return keyStore.containsAlias(keyName) || createKeyPair(keyName, useBiometricAuth);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static int getKeyProtectionType (String keyName) {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Key key = keyStore.getKey(keyName, null);
                KeyFactory factory = KeyFactory.getInstance(key.getAlgorithm(), "AndroidKeyStore");
                KeyInfo keyInfo = factory.getKeySpec(key, KeyInfo.class);
                if (keyInfo.isUserAuthenticationRequired()) {
                    return TonController.KEY_PROTECTION_TYPE_BIOMETRIC;
                }
            } catch (Exception ignore) {

            }
        }
        return TonController.KEY_PROTECTION_TYPE_NONE;
    }

    public static Cipher getCipherForDecrypt (String keyName) {
        try {
            PrivateKey key = (PrivateKey) keyStore.getKey(keyName, null);
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String encrypt (byte[] input, String keyName) {
        try {
            if (initCipher(Cipher.ENCRYPT_MODE, keyName) == CIPHER_INIT_OK) {
                byte[] bytes = cipher.doFinal(input);
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static byte[] decrypt (String encodedString, Cipher decryptCipher, String keyName) {
        try {
            if (decryptCipher == null) {
                initCipher(Cipher.DECRYPT_MODE, keyName);
                decryptCipher = cipher;
            }
            byte[] bytes = Base64.decode(encodedString, Base64.NO_WRAP);
            return decryptCipher.doFinal(bytes);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static void deleteKey (String keyName) {
        try {
            keyStore.deleteEntry(keyName);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isKeyInvalidated (String keyName) {
        return initCipher(Cipher.DECRYPT_MODE, keyName) == CIPHER_INIT_KEY_INVALIDATED;
    }


    public static class PasscodeCreateResult {
        public byte[] salt;
        public String encryptedPasscode;
        public @Nullable String encryptedTonData;
    }

    public static PasscodeCreateResult createAndEncryptPasscode (String passcode, String keyName, @Nullable byte[] unencryptedTonData) {
        byte[] creatingPasscodeSalt = new byte[32];
        Utilities.random.nextBytes(creatingPasscodeSalt);
        byte[] hash = Utilities.computePBKDF2(passcode.getBytes(), creatingPasscodeSalt);
        byte[] key = new byte[32];
        byte[] iv = new byte[32];
        System.arraycopy(hash, 0, key, 0, key.length);
        System.arraycopy(hash, key.length, iv, 0, iv.length);

        PasscodeCreateResult result = new PasscodeCreateResult();
        result.salt = creatingPasscodeSalt;

        if (unencryptedTonData != null) {
            Utilities.aesIgeEncryptionByteArray(unencryptedTonData, key, iv, true, false, 0, unencryptedTonData.length);
            result.encryptedTonData = encrypt(unencryptedTonData, keyName);
        }

        byte[] dataToPasscodeCheck = "_____PASSCODE_CHECK_SUCCESS_____".getBytes();
        Utilities.aesIgeEncryptionByteArray(dataToPasscodeCheck, key, iv, true, false, 0, dataToPasscodeCheck.length);
        result.encryptedPasscode = encrypt(dataToPasscodeCheck, keyName);

        return result;
    }

    public static boolean checkPasscode (String passcode, String encryptedPasscode, String passcodeSalt, String keyName, Callbacks.ErrorCallback onErrorRunnable) {
        byte[] decrypted = decrypt(encryptedPasscode, null, keyName);
        if (decrypted == null) {
            if (onErrorRunnable != null) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL", null));
            }
            return false;
        }

        byte[] hash = Utilities.computePBKDF2(passcode.getBytes(), Base64.decode(passcodeSalt, Base64.DEFAULT));
        byte[] key = new byte[32];
        byte[] iv = new byte[32];
        System.arraycopy(hash, 0, key, 0, key.length);
        System.arraycopy(hash, key.length, iv, 0, iv.length);
        Utilities.aesIgeEncryptionByteArray(decrypted, key, iv, false, false, 0, decrypted.length);

        return Arrays.equals("_____PASSCODE_CHECK_SUCCESS_____".getBytes(), decrypted);
    }


    public static byte[] decryptTonData (String encryptedTonData, @Nullable String passcode, String passcodeSalt, Cipher cipherForDecrypt, String keyName, Callbacks.ErrorCallback onErrorRunnable) {
        byte[] decrypted = decrypt(encryptedTonData, cipherForDecrypt, keyName);
        if (decrypted == null || decrypted.length <= 3) {
            if (onErrorRunnable != null) {
                AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL", null));
            }
            return null;
        }
        if (passcode != null) {
            byte[] hash = Utilities.computePBKDF2(passcode.getBytes(), Base64.decode(passcodeSalt, Base64.DEFAULT));
            byte[] key = new byte[32];
            byte[] iv = new byte[32];
            System.arraycopy(hash, 0, key, 0, key.length);
            System.arraycopy(hash, key.length, iv, 0, iv.length);
            Utilities.aesIgeEncryptionByteArray(decrypted, key, iv, false, false, 0, decrypted.length);
        }
        if (decrypted[1] == 'o' && decrypted[2] == 'k') {
            return decrypted;
        } else {
            if (onErrorRunnable != null) {
                if (!TextUtils.isEmpty(passcode)) {
                    AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("PASSCODE_INVALID", null));
                } else {
                    AndroidUtilities.runOnUIThread(() -> onErrorRunnable.run("KEYSTORE_FAIL_DECRYPT", null));
                }
            }
            return null;
        }
    }

    public static TonApi.InputKey getTonKeyFromDecrypted (byte[] decrypted, String tonPublicKey) {
        int padding = decrypted[0];
        byte[] password = new byte[64];
        byte[] secret = new byte[decrypted.length - 64 - padding - 3];
        System.arraycopy(decrypted, 3, password, 0, password.length);
        System.arraycopy(decrypted, 3 + password.length, secret, 0, secret.length);
        return new TonApi.InputKeyRegular(new TonApi.Key(tonPublicKey, secret), password);
    }
}
