package org.UI.Fragments.Create;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.UI.Components.Text.PasscodeTextView;
import org.UI.Fragments.Main.WalletActivity;
import org.UI.Fragments.Templates.WalletCreateBaseLinearActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CustomPhoneKeyboardView;
import org.telegram.ui.Components.LayoutHelper;

public class WalletCreateSetPasscodeActivity extends WalletCreateBaseLinearActivity {

    private int passcodeLength = 4;

    private PasscodeTextView passcodeNumbersView;
    private boolean changingPasscode;
    private String checkingPasscode;

    @Override
    public void onFragmentDestroy () {
        super.onFragmentDestroy();
        getTonController().finishSettingUserPasscode();
    }

    @Override
    public View createView (Context context) {
        super.createView(context);
        buttonTextView.setVisibility(View.GONE);

        passcodeNumbersView = new PasscodeTextView(context);
        passcodeNumbersView.setMaxLength(passcodeLength, false);
        topLayout.addView(passcodeNumbersView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40, Gravity.CENTER, 0, 28, 0, 0));

        titleTextView.setText(LocaleController.getString("WalletSetPasscode", R.string.WalletSetPasscode));
        updateDescription();

        CustomPhoneKeyboardView keyboardView = new CustomPhoneKeyboardView(context);
        keyboardView.setEditCallback(new CustomPhoneKeyboardView.EditCallback() {
            @Override
            public void onNewCharacter (String symbol) {
                passcodeNumbersView.appendCharacter(symbol);
                if (passcodeNumbersView.length() == passcodeLength) {
                    onPasscodeEnter();
                }
            }

            @Override
            public void onRemoveCharacter () {
                passcodeNumbersView.eraseLastCharacter();
            }
        });

        ((LinearLayout) fragmentView).addView(keyboardView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, CustomPhoneKeyboardView.KEYBOARD_HEIGHT_DP));

        imageView.getDrawable().setCustomEndFrame(49);

        return fragmentView;
    }

    public void updateDescription () {
        descriptionText.setText(LocaleController.formatString("WalletEnterPasscodeDigit", R.string.WalletEnterPasscodeDigit, passcodeLength));
    }

    public void setChangingPasscode () {
        changingPasscode = true;
    }

    private void showPasscodeConfirm () {
        if (passcodeNumbersView == null) {
            return;
        }
        String passcode = passcodeNumbersView.getString();
        if (passcode.length() == 0) {
            return;
        }
        checkingPasscode = passcode;
        passcodeNumbersView.eraseAllCharacters();
        titleTextView.setText(LocaleController.getString("WalletSetPasscodeRepeat", R.string.WalletSetPasscodeRepeat));
    }

    private void onPasscodeEnter () {
        if (checkingPasscode == null) {
            int length = passcodeNumbersView.length();
            if (length != passcodeLength) {
                onPasscodeError();
                return;
            }
            AndroidUtilities.runOnUIThread(this::showPasscodeConfirm, 150);
        } else {
            String passcode = passcodeNumbersView.getString();
            if (!passcode.equals(checkingPasscode)) {
                Toast.makeText(getParentActivity(), LocaleController.getString("WalletSetPasscodeError", R.string.WalletSetPasscodeError), Toast.LENGTH_SHORT).show();
                titleTextView.setText(LocaleController.getString("WalletSetPasscode", R.string.WalletSetPasscode));
                onPasscodeError();
                checkingPasscode = null;
                passcodeNumbersView.eraseAllCharacters();
                return;
            }
            showProgressDialog();
            getTonController().setupUserPasscode(passcode, () -> {
                hideProgressDialog();
                if (changingPasscode) {
                    finishFragment();
                } else {
                    for (BaseFragment fragment : parentLayout.fragmentsStack) {
                        if (fragment instanceof WalletCreateStartActivity) {
                            fragment.removeSelfFromStack();
                            break;
                        }
                    }

                    int type = getTonController().isWalletWasImported() ?
                        WalletCreateReadyActivity.TYPE_READY_IMPORT :
                        WalletCreateReadyActivity.TYPE_READY_CREATE;
                    presentFragment(new WalletActivity(type), true);
                }
            });
        }
    }

    private void onPasscodeError () {
        AndroidUtilities.shakeAndVibrateView(passcodeNumbersView, 2, 0);
    }


    @Override
    protected int getTopLayoutOffset () {
        return 0;
    }

    @Override
    protected int getBottomLayoutOffset () {
        return 0;
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_password;
    }

    @Override
    protected boolean needImageAutoRepeat () {
        return false;
    }

    @Override
    protected String getSecondButtonText () {
        return LocaleController.getString("WalletSetPasscodeOptions", R.string.WalletSetPasscodeOptions);
    }

    @Override
    protected void onSecondButtonClick (View v) {
        CharSequence[] items = new CharSequence[]{
            LocaleController.getString("WalletSetPasscode4Digit", R.string.WalletSetPasscode4Digit),
            LocaleController.getString("WalletSetPasscode6Digit", R.string.WalletSetPasscode6Digit),
        };

        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(v.getContext());
        ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);

        for (int a = 0; a < items.length; a++) {
            TextView textView = new TextView(v.getContext());
            textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            textView.setBackground(Theme.getSelectorDrawable(false));
            if (!LocaleController.isRTL) {
                textView.setGravity(Gravity.CENTER_VERTICAL);
            } else {
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            }
            textView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setMinWidth(AndroidUtilities.dp(196));
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setTag(0);
            textView.setText(items[a]);
            popupLayout.addView(textView);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
            if (LocaleController.isRTL) {
                layoutParams.gravity = Gravity.RIGHT;
            }
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(48);
            textView.setLayoutParams(layoutParams);
            int finalA = a;
            textView.setOnClickListener(v1 -> {
                popupWindow.dismiss();
                passcodeLength = finalA == 0 ? 4 : 6;
                passcodeNumbersView.eraseAllCharacters();
                passcodeNumbersView.setMaxLength(passcodeLength, true);
                updateDescription();
            });
        }

        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);

        popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        popupWindow.setFocusable(true);

        int[] location = new int[2];
        secondButton.getLocationOnScreen(location);
        location[1] -= AndroidUtilities.statusBarHeight;
        location[1] -= AndroidUtilities.dp(48 * 2 - 24);

        popupWindow.showAtLocation(fragmentView, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, location[1]);
    }
}
