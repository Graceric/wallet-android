package org.UI.Fragments.Create;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.UI.Fragments.Templates.WalletCreateBaseSeedInputActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Locale;

public class WalletCreateSeedInputActivity extends WalletCreateBaseSeedInputActivity {

    @Override
    public View createView (Context context) {
        super.createView(context);

        LinearLayout scrollViewLinearLayout = new LinearLayout(context);
        scrollViewLinearLayout.setOrientation(LinearLayout.VERTICAL);

        maxEditNumberWidth = 0;
        editTexts = new NumericEditText[24];
        for (int a = 0; a < editTexts.length; a++) {
            scrollViewLinearLayout.addView(editTexts[a] = new NumericEditText(context, a, String.format(Locale.US, "%d:", a + 1)), LayoutHelper.createLinear(200, 44, Gravity.CENTER_HORIZONTAL, 0, a == 0 ? 0 : 8, 0, 0));
        }
        if (BuildVars.FAST_IMPORT_MNEMONIC != null) {
            for (int a = 0; a < editTexts.length; a++) {
                editTexts[a].setText(BuildVars.FAST_IMPORT_MNEMONIC[a]);
            }
        }

        this.scrollViewLinearLayout.addView(scrollViewLinearLayout, 1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 80, 28, 80, 28));

        TextView descriptionText2 = LayoutHelper.createTextView(context, 15, 20);
        descriptionText2.setBackground(Theme.getRoundRectSelectorDrawable(Theme.getColor(Theme.key_wallet_defaultLinkTextColor), 8));
        descriptionText2.setTextColor(Theme.getColor(Theme.key_wallet_defaultLinkTextColor));
        descriptionText2.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText2.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(2), AndroidUtilities.dp(12), AndroidUtilities.dp(2));
        descriptionText2.setText(LocaleController.getString("WalletImportDontHave", R.string.WalletImportDontHave));
        descriptionText2.setOnClickListener(v -> {
            WalletCreateTooBadActivity fragment = new WalletCreateTooBadActivity();
            fragment.fragmentToRemove = WalletCreateSeedInputActivity.this;
            presentFragment(fragment);
        });

        actionBar.setTitle(LocaleController.getString("WalletSecretWordsTitle", R.string.WalletSecretWordsTitle));
        titleTextView.setText(LocaleController.getString("WalletSecretWordsTitle", R.string.WalletSecretWordsTitle));
        descriptionText.setText(LocaleController.getString("WalletImportInfo", R.string.WalletImportInfo));
        buttonTextView.setText(LocaleController.getString("WalletContinue", R.string.WalletContinue));


        topLayout.addView(descriptionText2, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 12, 0, 0));

        return fragmentView;
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_recovery_phrase;
    }

    @Override
    protected boolean needImageAutoRepeat () {
        return false;
    }

    @Override
    protected void onMainButtonClick (View v) {
        hideHint();
        if (!checkEditTexts()) {
            return;
        }
        createWallet();
    }

    private void doCreate () {
        String[] importWords = new String[24];
        for (int a = 0; a < 24; a++) {
            importWords[a] = editTexts[a].getText().toString();
        }
        doCreate(importWords);
    }

    private void doCreate (String[] importWords) {
        getTonController().createWallet(importWords, (words) -> {
            hideProgressDialog();
            if (fragmentToRemove != null) {
                fragmentToRemove.removeSelfFromStack();
            }
            presentFragment(new WalletCreatePerfectActivity(), true);
        }, (text, error) -> {
            hideProgressDialog();
            if (text.startsWith("TONLIB")) {
                AlertsCreator.showSimpleAlert(this, LocaleController.getString("WalletImportAlertTitle", R.string.WalletImportAlertTitle), LocaleController.getString("WalletImportAlertText", R.string.WalletImportAlertText));
            } else {
                AlertsCreator.showSimpleAlert(this, LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + text);
            }
        });
    }

    private void createWallet () {
        showProgressDialog();
        doCreate();
    }
}
