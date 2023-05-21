package org.UI.Fragments.Create;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BiometricPromtHelper;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.LayoutHelper;
import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;

public class WalletCreatePerfectActivity extends WalletCreateBaseLinearActivity {

    private CheckBoxSquare checkBox;

    @Override
    public View createView (Context context) {
        super.createView(context);

        titleTextView.setText(LocaleController.getString("WalletPerfect", R.string.WalletPerfect));
        descriptionText.setText(LocaleController.getString("WalletPerfectInfo", R.string.WalletPerfectInfo));
        buttonTextView.setText(LocaleController.getString("WalletPerfectSetPasscode", R.string.WalletPerfectSetPasscode));

        LinearLayout useBiometryCell = new LinearLayout(context);
        useBiometryCell.setOrientation(LinearLayout.HORIZONTAL);
        useBiometryCell.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(16), 0);
        useBiometryCell.setOnClickListener(this::onCheckBoxClick);
        useBiometryCell.setBackground(Theme.getSelectorDrawable(false));

        checkBox = new CheckBoxSquare(context);
        useBiometryCell.addView(checkBox, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(LocaleController.getString("WalletEnableBiometricAuth", R.string.WalletEnableBiometricAuth));
        useBiometryCell.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        useBiometryCell.setVisibility(BiometricPromtHelper.canAddBiometric() ? View.VISIBLE : View.GONE);

        bottomLayout.addView(useBiometryCell, 0, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 34, Gravity.CENTER, 0, 0, 0, 20));

        return fragmentView;
    }

    private void onCheckBoxClick (View v) {
        boolean needEnableBiometricAuth = !checkBox.isChecked();
        if (needEnableBiometricAuth && !BiometricPromtHelper.askForBiometricIfNoBiometricEnrolled(this)) {
            return;
        }

        checkBox.setChecked(needEnableBiometricAuth, true);
    }

    @Override
    public void onResume () {
        checkBox.setChecked(BiometricPromtHelper.canAddBiometric() && BiometricPromtHelper.hasBiometricEnrolled(), false);
        super.onResume();
    }

    @Override
    protected void onMainButtonClick (View v) {
        if (checkBox.isChecked()) {
            getTonController().setupBiometricAuth(null, () ->
                    presentFragment(new WalletCreateSetPasscodeActivity(), true),
                this::defaultErrorCallback);
        } else {
            presentFragment(new WalletCreateSetPasscodeActivity(), true);
        }
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_success;
    }

    @Override
    protected int getTopLayoutOffset () {
        return 100;
    }
}