package org.UI.Fragments.Create;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;

public class WalletCreateCongratulationsActivity extends WalletCreateBaseLinearActivity {
    public void setSecretWords (String[] secretWords) {
        this.secretWords = secretWords;
    }

    @Override
    public View createView (Context context) {
        super.createView(context);

        titleTextView.setText(LocaleController.getString("WalletCongratulations", R.string.WalletCongratulations));
        descriptionText.setText(LocaleController.getString("WalletCongratulationsinfo", R.string.WalletCongratulationsinfo));
        buttonTextView.setText(LocaleController.getString("WalletProceed", R.string.WalletProceed));

        return fragmentView;
    }

    @Override
    protected void onMainButtonClick (View v) {
        WalletCreateSeedShowActivity fragment = new WalletCreateSeedShowActivity();
        fragment.setSecretWords(secretWords, false);
        presentFragment(fragment, true);
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_congratulations;
    }

    @Override
    protected int getTopLayoutOffset () {
        return 50;
    }
}