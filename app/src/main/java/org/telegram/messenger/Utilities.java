/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import drinkless.org.ton.TonApi;

public class Utilities {

    public final static int MAX_ACCOUNT_COUNT = 1;
    public static Pattern pattern = Pattern.compile("[\\-0-9]+");
    public static SecureRandom random = new SecureRandom();

    public static volatile DispatchQueue globalQueue = new DispatchQueue("globalQueue");
    public static volatile DispatchQueue searchQueue = new DispatchQueue("searchQueue");

    public static int selectedAccount;

    static {
        try {
            File URANDOM_FILE = new File("/dev/urandom");
            FileInputStream sUrandomIn = new FileInputStream(URANDOM_FILE);
            byte[] buffer = new byte[1024];
            sUrandomIn.read(buffer);
            sUrandomIn.close();
            random.setSeed(buffer);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private native static void aesIgeEncryptionByteArray (byte[] buffer, byte[] key, byte[] iv, boolean encrypt, int offset, int length);

    private native static int pbkdf2 (byte[] password, byte[] salt, byte[] dst, int iterations);

    public native static int signEd25519hash (byte[] hash, byte[] privateKey, byte[] result);

    public native static int privateToPublicX25519 (byte[] privateKey, byte[] result);

    public static void aesIgeEncryptionByteArray (byte[] buffer, byte[] key, byte[] iv, boolean encrypt, boolean changeIv, int offset, int length) {
        aesIgeEncryptionByteArray(buffer, key, changeIv ? iv : iv.clone(), encrypt, offset, length);
    }

    public static Integer parseInt (CharSequence value) {
        if (value == null) {
            return 0;
        }
        int val = 0;
        try {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Integer.parseInt(num);
            }
        } catch (Exception ignore) {

        }
        return val;
    }

    public static Long parseLong (String value) {
        if (value == null) {
            return 0L;
        }
        long val = 0L;
        try {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String num = matcher.group(0);
                val = Long.parseLong(num);
            }
        } catch (Exception ignore) {

        }
        return val;
    }

    public static byte[] computePBKDF2 (byte[] password, byte[] salt) {
        byte[] dst = new byte[64];
        Utilities.pbkdf2(password, salt, dst, 100000);
        return dst;
    }

    public static byte[] publicKeyToBytes (String publicKey) {
        return Arrays.copyOfRange(Base64.decode(publicKey, Base64.URL_SAFE), 2, 34);
    }

    public static String truncateString (String inputString, int n, int k) {
        if (inputString == null || inputString.length() <= n + k) {
            return inputString;
        }
        String firstNChars = inputString.substring(0, n);
        String lastNChars = inputString.substring(inputString.length() - k);
        return firstNChars + "..." + lastNChars;
    }

    @Deprecated
    public static CharSequence formatCurrency (Long value) {
        if (value == null) return "";
        if (value == 0) {
            return "0";
        }
        String sign = value < 0 ? "-" : "";
        StringBuilder builder = new StringBuilder(String.format(Locale.US, "%s%d.%09d", sign, Math.abs(value / 1000000000L), Math.abs(value % 1000000000)));
        while (builder.length() > 1 && builder.charAt(builder.length() - 1) == '0' && builder.charAt(builder.length() - 2) != '.') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder;
    }

    public static Bitmap createTonQR (Context context, String key, Bitmap oldBitmap) {
        try {
            HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            return new QRCodeWriter().encode(key, BarcodeFormat.QR_CODE, 768, 768, hints, oldBitmap, context);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static void shareBitmap (Activity activity, View view, String text) {
        try {
            ImageView imageView = (ImageView) view;
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            File f = AndroidUtilities.getSharingDirectory();
            f.mkdirs();
            f = new File(f, "qr.jpg");
            FileOutputStream outputStream = new FileOutputStream(f.getAbsolutePath());
            bitmapDrawable.getBitmap().compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
            outputStream.close();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            if (!TextUtils.isEmpty(text)) {
                intent.putExtra(Intent.EXTRA_TEXT, text);
            }
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", f));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignore) {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                }
            } else {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            }
            activity.startActivityForResult(Intent.createChooser(intent, LocaleController.getString("WalletShareQr", R.string.WalletShareQr)), 500);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isValidTonDomain (String address) {
        return address.length() > 7 && address.endsWith(".ton");
    }

    public static TonApi.Error getTonApiErrorSafe (Object error) {
        if (error instanceof TonApi.Error) {
            return (TonApi.Error) error;
        }
        return null;
    }

    public static TonApi.InternalTransactionId getLastTransactionId (TonApi.RawFullAccountState accountState) {
        if (accountState == null) {
            return new TonApi.InternalTransactionId(0, new byte[32]);
        }
        return accountState.lastTransactionId;
    }

    public static long getLastSyncTime (TonApi.RawFullAccountState accountState) {
        return accountState.syncUtime;
    }

    public static byte[] reverse (byte[] arr) {
        if (arr == null) {
            return null;
        }
        byte[] array = new byte[arr.length];
        System.arraycopy(arr, 0, array, 0, arr.length);
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }

        return array;
    }

    public static boolean isEmptyTransaction (TonApi.RawTransaction transaction) {
        long value = 0;
        boolean hasMessage = false;
        boolean emptySource = true;
        if (transaction.inMsg != null) {
            value += transaction.inMsg.value;
            if (!(transaction.inMsg.msgData instanceof TonApi.MsgDataRaw)) {
                hasMessage = true;
            }
            emptySource = TextUtils.isEmpty(transaction.inMsg.source.accountAddress);
        }
        if (transaction.outMsgs != null && transaction.outMsgs.length > 0) {
            for (int a = 0; a < transaction.outMsgs.length; a++) {
                value -= transaction.outMsgs[a].value;
                if (!(transaction.outMsgs[a].msgData instanceof TonApi.MsgDataRaw)) {
                    hasMessage = true;
                }
            }
        }
        return !hasMessage && emptySource && value == 0;
    }

    public static String normalizeAddress (Address address) {
        if (address == null) return null;
        return normalizeAddress(address.toString());
    }

    public static String normalizeAddress (String address) {
        return new Address(address.replaceAll("\n", "")).toString(true, true, true, false).replaceAll("\n", "");
    }

    public static boolean isValidWalletAddress (String address) {
        return Address.isValid(address);
    }

    public static byte[] nonNull (byte[] bytes) {
        if (bytes == null) {
            return new byte[0];
        } else {
            return bytes;
        }
    }

    public static boolean isEmpty (Object[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isEmpty (byte[] arr) {
        return arr == null || arr.length == 0;
    }

    public static byte[] randomBytes (int n) {
        byte[] bytes = new byte[n];
        Utilities.random.nextBytes(bytes);
        return bytes;
    }

    public static String getDateKey (long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        int dateDay = calendar.get(Calendar.DAY_OF_YEAR);
        int dateYear = calendar.get(Calendar.YEAR);
        int dateMonth = calendar.get(Calendar.MONTH);
        return String.format(Locale.US, "%d_%02d_%02d", dateYear, dateMonth, dateDay);
    }

    public static String fixUrl (String url) {
        if (url.startsWith("ipfs://")) {
            return url.replace("ipfs://", "https://ipfs.io/ipfs/");
        }
        return url;
    }


    public static String getAddressFromTmvCell (TonApi.TvmStackEntry entry) {
        try {
            final byte[] addressBytes = (entry instanceof TonApi.TvmStackEntrySlice) ?
                ((TonApi.TvmStackEntrySlice) entry).slice.bytes :
                ((TonApi.TvmStackEntryCell) entry).cell.bytes;
            Address address = CellSlice.beginParse(Cell.deserializeBoc(addressBytes)).loadAddress();
            return address != null ? normalizeAddress(address.toString(true)) : null;
        } catch (Throwable e) {
            return null;
        }
    }
}
