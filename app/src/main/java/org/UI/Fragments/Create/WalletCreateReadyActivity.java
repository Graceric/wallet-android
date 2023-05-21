package org.UI.Fragments.Create;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.UI.Fragments.Main.WalletActivity;
import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;

public class WalletCreateReadyActivity extends WalletCreateBaseLinearActivity {
    public static final int TYPE_READY_NONE = -1;
    public static final int TYPE_READY_CREATE = 0;
    public static final int TYPE_READY_IMPORT = 1;

    private final int currentType;

    public WalletCreateReadyActivity (int type) {
        currentType = type;
    }

    @Override
    public View createView (Context context) {
        super.createView(context);

        if (currentType == TYPE_READY_CREATE) {
            titleTextView.setText(LocaleController.getString("WalletReady", R.string.WalletReady));
            descriptionText.setText(LocaleController.getString("WalletReadyInfo", R.string.WalletReadyInfo));
            buttonTextView.setText(LocaleController.getString("WalletView", R.string.WalletView));
        } else if (currentType == TYPE_READY_IMPORT) {
            titleTextView.setText(LocaleController.getString("WalletImportDone", R.string.WalletImportDone));
            descriptionText.setVisibility(View.GONE);
            buttonTextView.setText(LocaleController.getString("WalletProceed", R.string.WalletProceed));
        }

        return fragmentView;
    }

    @Override
    protected void onMainButtonClick (View v) {
        presentFragment(new WalletActivity(), true);
    }

    @Override
    protected int getImageAnimation () {
        return currentType == TYPE_READY_CREATE ?
            R.raw.wallet_success :
            R.raw.wallet_congratulations;
    }

    @Override
    protected boolean showBackButton () {
        return false;
    }
}