package org.UI.Fragments.Create;

import android.content.Context;
import android.view.View;

import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;

public class WalletCreateStartActivity extends WalletCreateBaseLinearActivity {

    @Override
    public View createView (Context context) {
        super.createView(context);

        titleTextView.setText(LocaleController.getString("GramWallet", R.string.GramWallet));
        descriptionText.setText(LocaleController.getString("GramWalletInfo", R.string.GramWalletInfo));
        buttonTextView.setText(LocaleController.getString("CreateMyWallet", R.string.CreateMyWallet));

        return fragmentView;
    }

    private void createWallet () {
        if (checkNetworkConfig(this::createWallet)) {
            showProgressDialog();
            getTonController().createWallet(null, (words) -> {
                hideProgressDialog();
                WalletCreateCongratulationsActivity fragment = new WalletCreateCongratulationsActivity();
                fragment.setSecretWords(words);
                presentFragment(fragment);
            }, (text, error) -> {
                hideProgressDialog();
                defaultErrorCallback(text, error);
            });
        }
    }

    private void importWallet () {
        if (checkNetworkConfig(this::importWallet)) {
            WalletCreateSeedInputActivity fragment = new WalletCreateSeedInputActivity();
            fragment.fragmentToRemove = this;
            presentFragment(fragment);
        }
    }

    private boolean checkNetworkConfig (Runnable onLoad) {
        if (getTonController().isNetworkConfigNotLoaded()) {
            showProgressDialog();
            getTonController().loadTonConfigFromUrl(loaded -> {
                hideProgressDialog();
                if (!loaded) {
                    AlertsCreator.showSimpleAlert(this, LocaleController.getString("WalletError", R.string.WalletError), LocaleController.getString("WalletCreateBlockchainConfigLoadError", R.string.WalletCreateBlockchainConfigLoadError));
                } else {
                    onLoad.run();
                }
            });
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getSecondButtonText () {
        return LocaleController.getString("ImportExistingWallet", R.string.ImportExistingWallet);
    }

    @Override
    protected void onMainButtonClick (View v) {
        createWallet();
    }

    @Override
    protected void onSecondButtonClick (View v) {
        importWallet();
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_start;
    }

    @Override
    protected boolean showBackButton () {
        return false;
    }
}