package org.TonController.Data;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.ton.java.utils.Utils;

import java.math.BigInteger;

public class TonAmount {
    public static final TonAmount EMPTY = new TonAmount(0);

    private final int decimals;

    private final BigInteger divider;
    private @Nullable BigInteger value;
    private boolean isInvalidated;

    public TonAmount (int decimals) {
        this.decimals = decimals;
        this.divider = BigInteger.valueOf(10).pow(decimals);
    }

    public void setBalance (BigInteger value) {
        this.value = value;
        this.isInvalidated = false;
    }

    public void setBalance (Long value) {
        if (value == null) {
            clear();
            return;
        }
        setBalance(BigInteger.valueOf(value));
    }

    public void setBalance (String value) {
        if (TextUtils.isEmpty(value)) {
            clear();
            return;
        }
        setBalance(new BigInteger(value));
    }

    public void clear () {
        this.value = null;
        this.isInvalidated = true;
    }

    public boolean isInvalidated () {
        return isInvalidated;
    }

    public void invalidate () {
        isInvalidated = true;
    }

    public @Nullable BigInteger getBalance () {
        return value;
    }

    public @NonNull BigInteger getBalanceSafe () {
        return value != null ? value: BigInteger.ZERO;
    }

    public boolean isMoreThanZero () {
        if (value == null) return false;
        return value.compareTo(BigInteger.ZERO) > 0;
    }

    @NonNull
    @Override
    public String toString () {
        return toString(false, true);
    }

    public String toString (boolean ignoreSign) {
        return toString(ignoreSign, true);
    }

    public String toString (boolean ignoreSign, boolean forceFraction) {
        if (value == null) return "";
        if (decimals == 0) return value.toString();
        if (value.equals(BigInteger.ZERO)) return "0";
        String sign = (value.signum() == -1 && !ignoreSign) ? "-" : "";

        String wholePart = value.abs().divide(divider).toString();

        BigInteger fractionPartN = value.abs().mod(divider);
        if (!forceFraction && fractionPartN.compareTo(BigInteger.ZERO) == 0) {
            return sign + wholePart;
        }

        String fractionPart = fractionPartN.toString();
        String fractionPartFixed = decimals != fractionPart.length() ?
            Utils.repeatString("0", decimals - fractionPart.length()) + fractionPart:
            fractionPart;

        StringBuilder builder = new StringBuilder(sign);
        builder.append(wholePart);
        builder.append(".");
        builder.append(fractionPartFixed);

        while (builder.length() > 1 && builder.charAt(builder.length() - 1) == '0' && builder.charAt(builder.length() - 2) != '.') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    public CharSequence toSpannableString (boolean ignoreSign) {
        CharSequence text = toString(ignoreSign);
        int index = TextUtils.indexOf(text, '.');
        if (index >= 0) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
            spannableStringBuilder.setSpan(new RelativeSizeSpan(0.78f), index + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text = spannableStringBuilder;
        }
        return text;
    }

    public static TonAmount valueOf (long balance) {
        TonAmount b = new TonAmount(9);
        b.setBalance(balance);
        return b;
    }

    public static TonAmount valueOf (long balance, int decimals) {
        TonAmount b = new TonAmount(decimals);
        b.setBalance(balance);
        return b;
    }
}
