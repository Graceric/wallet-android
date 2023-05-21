package org.UI.Fragments.Create;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.UI.Fragments.Templates.WalletCreateBaseScrollActivity;

import java.util.Locale;

public class WalletCreateSeedShowActivity extends WalletCreateBaseScrollActivity {
    private boolean exportingWords;
    private boolean isNotFirstWarning;
    private int maxNumberWidth;

    @Override
    public boolean onFragmentCreate () {
        if (secretWords == null || secretWords.length != 24) {
            return false;
        }
        if (!exportingWords) {
            cancelOnDestroyRunnable = () -> {
                if (fragmentView != null) {
                    fragmentView.setKeepScreenOn(false);
                }
                cancelOnDestroyRunnable = null;
            };
            AndroidUtilities.runOnUIThread(cancelOnDestroyRunnable, 60 * 1000);
        }
        return super.onFragmentCreate();
    }

    @Override
    public View createView (Context context) {
        super.createView(context);

        LinearLayout leftColumn = new LinearLayout(context);
        LinearLayout rightColumn = new LinearLayout(context);
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        rightColumn.setOrientation(LinearLayout.VERTICAL);

        LinearLayout columnsLayout = new LinearLayout(context);
        columnsLayout.setOrientation(LinearLayout.HORIZONTAL);
        columnsLayout.addView(leftColumn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 2f));
        columnsLayout.addView(rightColumn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 8, 0));

        for (int a = 0; a < 12; a++) {
            for (int b = 0; b < 2; b++) {
                NumericTextView textView = new NumericTextView(context);
                textView.setGravity(Gravity.LEFT | Gravity.TOP);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                textView.setNumber(String.format(Locale.US, "%d.", b == 0 ? 1 + a : 13 + a));
                textView.setText(secretWords[b == 0 ? a : 12 + a]);
                (b == 0 ? leftColumn : rightColumn).addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, a == 0 ? 0 : 10, 0, 0));
            }
        }

        scrollViewLinearLayout.addView(columnsLayout, 1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 40, 40, 40, 44));

        actionBar.setTitle(LocaleController.getString("WalletSecretWordsTitle", R.string.WalletSecretWords));
        titleTextView.setText(LocaleController.getString("WalletSecretWords", R.string.WalletSecretWords));
        descriptionText.setText(LocaleController.getString("WalletSecretWordsInfo", R.string.WalletSecretWordsInfo));
        buttonTextView.setText(LocaleController.getString("WalletDone", R.string.WalletDone));
        fragmentView.setKeepScreenOn(true);

        return fragmentView;
    }

    public void setSecretWords (String[] words, boolean isExport) {
        secretWords = words;
        exportingWords = isExport;
    }

    @Override
    protected void onMainButtonClick (View v) {
        if (exportingWords) {
            finishFragment();
            return;
        }
        if (SystemClock.uptimeMillis() - showTime < 60 * 1000) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("WalletSecretWordsAlertTitle", R.string.WalletSecretWordsAlertTitle));
            builder.setMessage(LocaleController.getString("WalletSecretWordsAlertText", R.string.WalletSecretWordsAlertText));
            builder.setPositiveButton(LocaleController.getString("WalletSecretWordsAlertButton", R.string.WalletSecretWordsAlertButton), (d, a) -> isNotFirstWarning = true);
            builder.setOnCancelListener(d -> {
                isNotFirstWarning = true;
            });
            if (isNotFirstWarning) {
                builder.setNegativeButton(LocaleController.getString("WalletSecretWordsAlertButtonSkip", R.string.WalletSecretWordsAlertButtonSkip), (d, a) -> goToWordsCheck());
            }
            showDialog(builder.create());
            return;
        }
        goToWordsCheck();
    }

    private void goToWordsCheck () {
        WalletCreateSeedCheckActivity fragment = new WalletCreateSeedCheckActivity();
        fragment.setSecretWords(secretWords);
        fragment.fragmentToRemove = this;
        presentFragment(fragment);
    }

    @Override
    protected boolean needSetFlagSecure () {
        return true;
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_recovery_phrase;
    }

    @Override
    protected boolean needImageAutoRepeat () {
        return false;
    }

    private class NumericTextView extends TextView {
        private final TextPaint numericPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        private String number;
        private int numberWidth;

        public NumericTextView (Context context) {
            super(context);
            setPadding(AndroidUtilities.dp(32), 0, 0, 0);

            numericPaint.setTextSize(AndroidUtilities.dp(15));
        }

        public void setNumber (String value) {
            number = value;
            numberWidth = (int) Math.ceil(numericPaint.measureText(number));
            maxNumberWidth = Math.max(maxNumberWidth, numberWidth);
        }

        @Override
        protected void onDraw (Canvas canvas) {
            super.onDraw(canvas);
            if (number != null) {
                numericPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
                canvas.drawText(number, maxNumberWidth - numberWidth, AndroidUtilities.dp(17), numericPaint);
            }
        }
    }
}
