package org.UI.Sheets.SendToncoins;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import org.TonController.Data.Jettons.RootJettonContract;
import org.UI.Cells.AddressEditTextCell;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Sheets.Templates.BottomSheetPageScrollable;
import org.UI.Utils.CurrencyUtils;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsTextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

public class SendTonCommentInputSheetPage extends BottomSheetPageScrollable {
    private static final int MAX_COMMENT_LENGTH = 500;

    private final @Nullable RootJettonContract rootJettonContract;

    public SendTonCommentInputSheetPage (@Nullable RootJettonContract rootJettonContract) {
        this.rootJettonContract = rootJettonContract;
    }

    private DefaultButtonView nextButton;
    private AddressEditTextCell commentEditTextCell;
    private TextInfoPrivacyCell descriptionView;
    private SettingsTextCell recipientAddressCell;
    private SettingsTextCell recipientAddressDomainCell;
    private SettingsTextCell recipientAmountCell;
    private SettingsTextCell recipientFeeCell;

    private String inputFieldCommentString = "";
    private long sendFee = -2;

    @Override
    protected ViewGroup createView (Context context) {
        ViewGroup viewGroup = super.createView(context);

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

        HeaderCell headerCell = new HeaderCell(context, 20, 0, false);
        headerCell.setText(LocaleController.getString("WalletComment", R.string.WalletComment));
        linearLayout.addView(headerCell);

        commentEditTextCell = new AddressEditTextCell(context);
        // addressEditTextCell.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        commentEditTextCell.addTextWatcher(new TextWatcher() {
            @Override
            public void beforeTextChanged (CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged (CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged (Editable editable) {
                if (commentEditTextCell.getTag() != null) {
                    return;
                }
                inputFieldCommentString = editable.toString();
                int len = inputFieldCommentString.getBytes().length;
                if (len > MAX_COMMENT_LENGTH) {
                    editable.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_wallet_errorTextColor)), Math.min(MAX_COMMENT_LENGTH, editable.length()), editable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    editable.setSpan(new BackgroundColorSpan(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_wallet_errorTextColor), (int) (0.12f * 255))), Math.min(MAX_COMMENT_LENGTH, editable.length()), editable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                updateDescriptionField();
                startUpdateSendFee();
            }
        });
        linearLayout.addView(commentEditTextCell);

        descriptionView = new TextInfoPrivacyCell(context, 20, 12, 4);
        descriptionView.setBottomPadding(12);
        linearLayout.addView(descriptionView);

        HeaderCell headerCell2 = new HeaderCell(context);
        headerCell2.setText(LocaleController.getString("WalletTransactionDetails", R.string.WalletTransactionDetails));
        linearLayout.addView(headerCell2);

        nextButton = new DefaultButtonView(context);
        nextButton.setText(LocaleController.getString("WalletConfirmAndSend", R.string.WalletConfirmAndSend));
        nextButton.setOnClickListener(this::onCommentInputContinueButtonClick);
        viewGroup.addView(nextButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT, 16, 0, 16, 16));

        recipientAddressCell = new SettingsTextCell(context);
        recipientAddressCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(recipientAddressCell);

        recipientAddressDomainCell = new SettingsTextCell(context);
        recipientAddressDomainCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(recipientAddressDomainCell);

        recipientAmountCell = new SettingsTextCell(context);
        recipientAmountCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(recipientAmountCell);

        recipientFeeCell = new SettingsTextCell(context);
        recipientFeeCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(recipientFeeCell);

        startUpdateSendFee();
        updateAll();

        return viewGroup;
    }


    public void setInputFieldCommentString (String inputFieldCommentString) {
        this.inputFieldCommentString = !TextUtils.isEmpty(inputFieldCommentString)? inputFieldCommentString: "";
        updateEditField();
    }

    private void updateEditField () {
        commentEditTextCell.setTextAndHint(!TextUtils.isEmpty(inputFieldCommentString) ? inputFieldCommentString : getters.getInputFieldCommentString(), LocaleController.getString("WalletEnterWalletAddress", R.string.WalletEnterWalletAddress), false);
        updateDescriptionField();
    }

    private void updateDescriptionField () {
        SpannableStringBuilder builder = new SpannableStringBuilder(LocaleController.getString("WalletSendTransactionCommentWarning", R.string.WalletSendTransactionCommentWarning));
        if (inputFieldCommentString.length() > 0) {
            builder.append("\n");
            int len = inputFieldCommentString.getBytes().length;
            int s = builder.length();
            if (len <= MAX_COMMENT_LENGTH) {
                builder.append(LocaleController.formatString("WalletCommentCharactersLeft", R.string.WalletCommentCharactersLeft, MAX_COMMENT_LENGTH - len));
                if (MAX_COMMENT_LENGTH - len < 100) {
                    builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_wallet_warningTextColor)), s, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                builder.append(LocaleController.formatString("WalletCommentCharactersOverflow", R.string.WalletCommentCharactersOverflow, len - MAX_COMMENT_LENGTH));
                builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_wallet_errorTextColor)), s, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        descriptionView.setText(builder);
    }

    private void updateAll () {
        updateEditField();
        updateDescriptionField();

        String domain = getters.getInputFieldRecipientDomainString();
        if (TextUtils.isEmpty(domain)) {
            recipientAddressDomainCell.setVisibility(View.GONE);
            recipientAddressCell.setTextAndValue(LocaleController.getString("WalletSendTransactionRecipient", R.string.WalletSendTransactionRecipient), Utilities.truncateString(getters.getInputFieldRecipientAddressString(), 6, 6), true);
        } else {
            recipientAddressDomainCell.setVisibility(View.VISIBLE);
            recipientAddressDomainCell.setTextAndValue(LocaleController.getString("WalletSendTransactionRecipient", R.string.WalletSendTransactionRecipient), domain, true);
            recipientAddressCell.setTextAndValue(LocaleController.getString("WalletSendTransactionRecipientAddress", R.string.WalletSendTransactionRecipientAddress), Utilities.truncateString(getters.getInputFieldRecipientAddressString(), 6, 6), true);
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder("- ");
        ssb.setSpan(CurrencyUtils.createSpan(18, rootJettonContract), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(getters.getInputFieldSendAmountValue().toString(true, false));

        recipientAmountCell.setTextAndValue(LocaleController.getString("WalletSendTransactionAmount", R.string.WalletSendTransactionAmount), ssb, true);
        updateSendFee(sendFee);
    }

    private int reqNumber = 0;
    private Runnable checkSendFeeRunnable;

    private void checkUpdateSendFee () {
        int id = ++reqNumber;
        getTonController().queryGetSendFee(getters.getInputFieldRecipientAddressString(), getters.getInputFieldSendAmountValue().getBalanceSafe().longValue(), inputFieldCommentString, fee -> {
            if (id == reqNumber) updateSendFee(fee);
        });
        checkSendFeeRunnable = null;
    }

    private void startUpdateSendFee () {
        if (rootJettonContract != null) {
            updateSendFee(200000000);
            return;
        }

        updateSendFee(-2);
        if (checkSendFeeRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(checkSendFeeRunnable);
        }
        AndroidUtilities.runOnUIThread(checkSendFeeRunnable = this::checkUpdateSendFee, sendFee >= 0 ? 0 : 100);
    }

    private void updateSendFee (long fee) {
        sendFee = fee;
        if (sendFee >= 0) {
            SpannableStringBuilder ssb = new SpannableStringBuilder("≈   ");
            ssb.setSpan(CurrencyUtils.createSpan(18), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(Utilities.formatCurrency(sendFee));
            recipientFeeCell.setTextAndValue(LocaleController.getString("WalletSendTransactionFee", R.string.WalletSendTransactionFee), ssb, false);
        } else {
            if (sendFee == -1) {
                SpannableStringBuilder ssb = new SpannableStringBuilder("  ?");
                ssb.setSpan(CurrencyUtils.createSpan(18), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                recipientFeeCell.setTextAndValue(LocaleController.getString("WalletSendTransactionFee", R.string.WalletSendTransactionFee), ssb, false);
            } else {
                recipientFeeCell.setTextAndValue(LocaleController.getString("WalletSendTransactionFee", R.string.WalletSendTransactionFee), "", false);
            }
        }
        recipientFeeCell.setProgress(fee == -2);
    }

    private void onCommentInputContinueButtonClick (View v) {
        if (inputFieldCommentString.getBytes().length <= MAX_COMMENT_LENGTH) {
            callback.run(inputFieldCommentString);
        } else {
            AndroidUtilities.shakeAndVibrateView(commentEditTextCell, 2, 0);
        }
    }


    private Callbacks.StringCallback callback;
    private Callbacks.SendInfoGetters getters;

    public void setSendInfoGetters (Callbacks.SendInfoGetters sendInfoGetters, Callbacks.StringCallback onInputRunnable) {
        this.callback = onInputRunnable;
        this.getters = sendInfoGetters;
    }

    @Override
    protected void onPrepareToShow () {
        updateAll();
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp(8);
    }

    @Override
    public int getPopupPaddingTop () {
        return ActionBar.getCurrentActionBarHeight();
    }

    @Override
    public int getPopupPaddingBottom () {
        return AndroidUtilities.dp(80);
    }
}
