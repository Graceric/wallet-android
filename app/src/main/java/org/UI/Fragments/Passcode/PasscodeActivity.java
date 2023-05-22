package org.UI.Fragments.Passcode;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;

import org.TonController.TonController;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;

import drinkless.org.ton.TonApi;

public class PasscodeActivity extends BaseFragment {
    private PasscodeView passcodeView;

    @Override
    protected ActionBar createActionBar (Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setBackgroundDrawable(null);
        actionBar.setTitleColor(Theme.getColor(Theme.key_wallet_whiteText));
        actionBar.setItemsColor(Theme.getColor(Theme.key_wallet_whiteText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackgroundSelector), false);
        actionBar.setAddToContainer(false);
        actionBar.setOnTouchListener(null);
        actionBar.setOnClickListener(null);
        actionBar.setClickable(false);
        actionBar.setFocusable(false);
        return actionBar;
    }

    @Override
    public void onFragmentDestroy () {
        super.onFragmentDestroy();
        if (Build.VERSION.SDK_INT >= 23 && AndroidUtilities.allowScreenCapture()) {
            AndroidUtilities.setFlagSecure(this, false);
        }
        hideProgressDialog();
    }

    @Override
    public void finishFragment (boolean animated) {
        super.finishFragment(animated);
        hideProgressDialog();
    }

    @Override
    public View createView (Context context) {
        FrameLayout frameLayout = new FrameLayout(context) {
            @Override
            protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        };
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = frameLayout;

        int maxCharacters = TonController.getInstance(Utilities.selectedAccount).getPasscodeLength();
        passcodeView = new PasscodeView(context);
        passcodeView.setTonController(getTonController());
        passcodeView.setMaxCharacters(maxCharacters, false);
        passcodeView.setPasscodeDelegate(this::checkPasscode);
        frameLayout.addView(passcodeView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        passcodeView.onShow();


        frameLayout.addView(actionBar);

        if (Build.VERSION.SDK_INT >= 23 && AndroidUtilities.allowScreenCapture()) {
            AndroidUtilities.setFlagSecure(this, true);
        }

        return fragmentView;
    }

    @Override
    public void onResume () {
        super.onResume();
        if (passcodeView != null) {
            passcodeView.onResume();
        }
    }

    @Override
    public void onPause () {
        super.onPause();
        if (passcodeView != null) {
            passcodeView.onPause();
        }
    }

    private Callbacks.StringCallback onSuccessPasscodeEnterDelegate;

    public void setOnSuccessPasscodeDelegate (Callbacks.StringCallback onSuccessPasscodeEnterDelegate) {
        this.onSuccessPasscodeEnterDelegate = onSuccessPasscodeEnterDelegate;
    }

    public void checkPasscode (String passcode) {
        if (getParentActivity() == null) {
            return;
        }
        showProgressDialog();
        getTonController().checkPasscode(passcode, () -> {
            passcodeView.onGoodPasscode(false);
            if (onSuccessPasscodeEnterDelegate != null) {
                onSuccessPasscodeEnterDelegate.run(passcode);
            } else {
                hideProgressDialog();
            }
        }, this::onPasscodeError);
    }

    private void onPasscodeError (String text, TonApi.Error error) {
        AndroidUtilities.runOnUIThread(() -> {
            hideProgressDialog();
            if ("PASSCODE_INVALID".equals(text)) {
                passcodeView.onWrongPasscode();
            } else {
                AlertsCreator.showSimpleAlert(this, LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + (error != null ? error.message : text));
            }
        });
    }

    @Override
    public boolean onBackPressed () {
        return swipeBackEnabled;
    }
}
