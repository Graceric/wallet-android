package org.UI.Fragments.Create;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;

public class WalletCreateTooBadActivity extends WalletCreateBaseLinearActivity {

    @Override
    public View createView (Context context) {
        super.createView(context);

        titleTextView.setText(LocaleController.getString("WalletTooBad", R.string.WalletTooBad));
        descriptionText.setText(LocaleController.getString("WalletTooBadInfo", R.string.WalletTooBadInfo));
        buttonTextView.setText(LocaleController.getString("WalletTooBadEnter", R.string.WalletTooBadEnter));

        return fragmentView;
    }

    @Nullable
    @Override
    protected String getSecondButtonText () {
        return LocaleController.getString("WalletTooBadCreate", R.string.WalletTooBadCreate);
    }

    @Override
    protected void onMainButtonClick (View v) {
        finishFragment();
    }

    @Override
    protected void onSecondButtonClick (View v) {
        if (fragmentToRemove != null) {
            fragmentToRemove.removeSelfFromStack();
        }
        finishFragment();
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_too_bad;
    }
}