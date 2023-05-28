package org.UI.Sheets.SendToncoins;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.UI.Cells.AmountEditTextCell;
import org.UI.Sheets.Templates.BottomSheetPageSimple;
import org.UI.Utils.CurrencyUtils;
import org.UI.Utils.Typefaces;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSwitchCell;
import org.telegram.ui.Components.CustomPhoneKeyboardView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SmallTextButton;

import java.math.BigInteger;

public class SendTonAmountInputSheetPage extends BottomSheetPageSimple {
    private final @Nullable RootJettonContract rootJettonContract;

    public SendTonAmountInputSheetPage (@Nullable RootJettonContract rootJettonContract) {
        this.rootJettonContract = rootJettonContract;
    }

    private AmountEditTextCell editTextCell;
    private SettingsSwitchCell sendAllCheckCell;
    private TextView sendToTextView;
    private @Nullable BigInteger inputFieldSendAmountValue;

    @Override
    protected ViewGroup createView (Context context) {
        ViewGroup frameLayout = super.createView(context);

        editTextCell = new AmountEditTextCell(context);
        if (rootJettonContract != null) {
            editTextCell.setDecimals(rootJettonContract.content.decimals);
            editTextCell.setCurrencyUrl(rootJettonContract.content.imageUrl);
        }
        editTextCell.setDelegate(new AmountEditTextCell.OnUpdateAmountValue() {
            @Override
            public void onUpdate (BigInteger i, String value) {
                inputFieldSendAmountValue = i;
                sendAllCheckCell.setChecked(i.compareTo(getters.getCurrentWalletBalance().getBalanceSafe()) == 0);
            }

            @Override
            public TonAmount getCurrentWalletBalance () {
                return getters.getCurrentWalletBalance();
            }
        });
        frameLayout.addView(editTextCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.NO_GRAVITY, 0, 56 + 48, 0, 7 + 56 + 48));

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(LocaleController.getString("WalletSendAll", R.string.WalletSendAll));
        ssb.append("   ");
        ssb.setSpan(CurrencyUtils.createSpan(18, rootJettonContract), ssb.length() - 2, ssb.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(getters.getCurrentWalletBalance().toString());

        sendAllCheckCell = new SettingsSwitchCell(context);
        sendAllCheckCell.getTextView().setTypeface(Typefaces.INTER_REGULAR);
        sendAllCheckCell.setTextAndCheck(ssb, false, false);
        sendAllCheckCell.setBackground(Theme.getSelectorDrawable(false));
        sendAllCheckCell.setOnClickListener(v -> {
            boolean newChecked = !sendAllCheckCell.isChecked();
            sendAllCheckCell.setChecked(newChecked);
            if (newChecked) {
                editTextCell.setText(getters.getCurrentWalletBalance().toString());
            } else {
                editTextCell.setText("");
            }
        });
        frameLayout.addView(sendAllCheckCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 0, 0, 0, 7 + 56));

        sendToTextView = new TextView(context);
        sendToTextView.setTypeface(Typefaces.INTER_REGULAR);
        sendToTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        sendToTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        frameLayout.addView(sendToTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 20, 56 + 14, 0, 0));

        SmallTextButton backButton = new SmallTextButton(context);
        backButton.setText(LocaleController.getString("WalletEdit", R.string.WalletEdit));
        backButton.setOnClickListener(v -> showPrevPage());
        frameLayout.addView(backButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 20, 56 + 8, 20, 0));

        nextButton.setText(LocaleController.getString("WalletContinue", R.string.WalletContinue));
        actionBar.setTitle(LocaleController.formatString("WalletSendX", R.string.WalletSendX,
            rootJettonContract != null ? rootJettonContract.content.getSymbolOrName(): "TON"));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    showPrevPage();
                }
            }
        });

        nextButton.setOnClickListener(this::onAmountInputContinueButtonClick);
        FrameLayout.LayoutParams nextButtonLayoutParams = (FrameLayout.LayoutParams) nextButton.getLayoutParams();
        nextButtonLayoutParams.bottomMargin = AndroidUtilities.dp(7);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(frameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1f));

        CustomPhoneKeyboardView keyboardView = new CustomPhoneKeyboardView(context, true);
        keyboardView.setEditText(editTextCell.getTextView());
        linearLayout.addView(keyboardView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, CustomPhoneKeyboardView.KEYBOARD_HEIGHT_DP, 0f));

        return linearLayout;
    }

    public void setInputFieldSendAmountValue (long amountValue) {
        if (inputFieldSendAmountValue == null && amountValue == 0) return;

        inputFieldSendAmountValue = BigInteger.valueOf(amountValue);
        sendAllCheckCell.setChecked(inputFieldSendAmountValue.compareTo(getters.getCurrentWalletBalance().getBalanceSafe()) == 0);
        editTextCell.setText(getters.getInputFieldSendAmountValue().toString());
    }

    @Override
    protected void onPrepareToShow () {
        AndroidUtilities.hideKeyboard(editTextCell);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("Send to:");
        if (getters.getInputFieldRecipientAddressString() != null) {
            builder.append(" ");
            int c = builder.length();
            builder.append(Utilities.truncateString(getters.getInputFieldRecipientAddressString(), 4, 4));
            builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_wallet_blackText)), c, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (getters.getInputFieldRecipientDomainString() != null) {
            builder.append(" ");
            builder.append(getters.getInputFieldRecipientDomainString());
        }



        if (inputFieldSendAmountValue == null && getters.getInputFieldSendAmountValue().isMoreThanZero()) {
            editTextCell.setText(getters.getInputFieldSendAmountValue().toString());
        }
        sendToTextView.setText(builder);
    }

    private void onAmountInputContinueButtonClick (View v) {
        if (inputFieldSendAmountValue == null
          || inputFieldSendAmountValue.compareTo(getters.getCurrentWalletBalance().getBalanceSafe()) > 0
          || inputFieldSendAmountValue.compareTo(BigInteger.ZERO) == 0
        ) {
            AndroidUtilities.shakeAndVibrateView(editTextCell, 2, 0);
            return;
        }

        callback.run(inputFieldSendAmountValue.longValue(), inputFieldSendAmountValue.compareTo(getters.getCurrentWalletBalance().getBalanceSafe()) == 0);
    }

    private Callbacks.SendAmountInputCallback callback;
    private Callbacks.SendInfoGetters getters;

    public void setSendInfoGetters (Callbacks.SendInfoGetters sendInfoGetters, Callbacks.SendAmountInputCallback onInputRunnable) {
        this.callback = onInputRunnable;
        this.getters = sendInfoGetters;
    }
}
