package org.UI.Sheets.TonConnect;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Base64;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.TonController.Data.TonAmount;
import org.TonController.Parsers.PayloadParser;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonController;
import org.UI.Cells.WalletTransactionBalanceCell;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Components.Buttons.DefaultButtonsPairView;
import org.UI.Sheets.Templates.BottomSheetPageScrollable;
import org.UI.Utils.CurrencyUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsTextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UndoView;

import java.math.BigInteger;

import drinkless.org.ton.TonApi;

public class TonConnectSendPage extends BottomSheetPageScrollable {
    private final RequestSendTransaction request;
    private final TonAmount walletBalance;

    private TonController.SendTransactionCallbacks callbacks;
    private @Nullable WalletTransactionBalanceCell balanceCell;
    private DefaultButtonsPairView buttonsPairView;
    private SettingsTextCell feeCell;
    private UndoView undoView;

    public TonConnectSendPage (RequestSendTransaction request, TonAmount walletBalance) {
        this.request = request;
        this.walletBalance = walletBalance;
    }

    public void setSendCallbacks (TonController.SendTransactionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    protected ViewGroup createView (Context context) {
        ViewGroup viewGroup = super.createView(context);

        actionBar.setTitle(LocaleController.getString("TonConnectTransfer", R.string.TonConnectTransfer));

        if (request.messages.length == 1) {
            balanceCell = new WalletTransactionBalanceCell(context);
            balanceCell.setBalance(TonAmount.valueOf(request.messages[0].amount));
            linearLayout.addView(balanceCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        for (int a = 0; a < request.messages.length; a++) {
            if (request.messages.length > 1) {
                HeaderCell headerCell = new HeaderCell(context);
                headerCell.setText("Transaction " + (a + 1));
                linearLayout.addView(headerCell);
            }
            initMessage(context, request.messages[a]);
        }

        if (request.messages.length > 1) {
            HeaderCell headerCell = new HeaderCell(context);
            headerCell.setText("Total");
            linearLayout.addView(headerCell);
        }
        feeCell = new SettingsTextCell(context);
        feeCell.setText("Fee", false);
        feeCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        feeCell.setProgress(true);
        getTonController().queryGetSendFee(request.messages, fee -> {
            feeCell.setProgress(false);
            if (fee == -1) {
                SpannableStringBuilder ssb = new SpannableStringBuilder("  ?");
                ssb.setSpan(CurrencyUtils.createSpan(18), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                feeCell.setTextAndValue(LocaleController.getString("WalletSendTransactionFee", R.string.WalletSendTransactionFee), ssb, false);
            } else {
                SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
                ssb.setSpan(CurrencyUtils.createSpan(18), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(TonAmount.valueOf(fee).toString());
                feeCell.setTextAndValue(LocaleController.getString("WalletSendTransactionFee", R.string.WalletSendTransactionFee), ssb, false);
            }
        });
        linearLayout.addView(feeCell);

        buttonsPairView = new DefaultButtonsPairView(context);
        viewGroup.addView(buttonsPairView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));
        initButtons();

        undoView = new UndoView(context);
        viewGroup.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        return viewGroup;
    }

    private void initMessage (Context context, RequestSendTransaction.MessageRequest messageRequest) {
        SettingsTextCell recipient = new SettingsTextCell(context);
        recipient.setTextAndValue(LocaleController.getString("WalletSendTransactionRecipient", R.string.WalletSendTransactionRecipient), Utilities.truncateString(messageRequest.address, 4, 4), true);
        recipient.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(recipient);

        boolean forceDivider = request.messages.length == 1;
        boolean hasPayload = messageRequest.payload != null;
        boolean hasStateInit = messageRequest.stateInit != null;

        if (request.messages.length > 1) {
            SpannableStringBuilder ssb = new SpannableStringBuilder("  ");
            ssb.setSpan(CurrencyUtils.createSpan(18), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(Utilities.formatCurrency(messageRequest.amount));

            SettingsTextCell amount = new SettingsTextCell(context);
            amount.setTextAndValue(LocaleController.getString("WalletSendTransactionAmount", R.string.WalletSendTransactionAmount), ssb, hasPayload || hasStateInit || forceDivider);
            amount.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
            linearLayout.addView(amount);
        }

        if (messageRequest.payload != null) {
            SettingsTextCell comment = new SettingsTextCell(context);
            if (messageRequest.parsedPayload instanceof PayloadParser.ResultComment) {
                comment.setTextAndValue(LocaleController.getString("WalletTransactionComment", R.string.WalletTransactionComment), ((PayloadParser.ResultComment) messageRequest.parsedPayload).comment, hasStateInit || forceDivider);
            } else if (messageRequest.parsedPayload instanceof PayloadParser.ResultUnknown || messageRequest.parsedPayload == null) {
                comment.setTextAndValue(LocaleController.getString("WalletPayload", R.string.WalletPayload), Base64.encodeToString(messageRequest.payload, Base64.NO_WRAP), hasStateInit || forceDivider);
            } else {
                comment.setTextAndValue(LocaleController.getString("WalletTransactionAction", R.string.WalletTransactionAction), messageRequest.parsedPayload.getPayloadActionName(), hasStateInit || forceDivider);
            }

            comment.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
            linearLayout.addView(comment);
        }

        if (messageRequest.stateInit != null) {
            SettingsTextCell amount = new SettingsTextCell(context);
            amount.setTextAndValue(
                LocaleController.getString("WalletTransactionAction", R.string.WalletTransactionAction),
                LocaleController.getString("WalletTransactionActionDeploy", R.string.WalletTransactionActionDeploy), forceDivider);
            amount.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
            linearLayout.addView(amount);
        }
    }

    private void initButtons () {

        buttonsPairView.cancelButton.setText(LocaleController.getString("Cancel", R.string.Cancel));
        buttonsPairView.cancelButton.setCollapseIcon(R.drawable.baseline_close_24);
        buttonsPairView.cancelButton.setType(DefaultButtonView.TYPE_DEFAULT);
        buttonsPairView.cancelButton.setOnClickListener(v -> {
            buttonsPairView.setProgress(false, true, true);
            getTonController().getTonConnectController().sendErrorToApplication(request, 300, "User declined the request", j -> {
                buttonsPairView.setProgress(false, false, true);
                buttonsPairView.collapseButtons(false);
            });
        });

        buttonsPairView.confirmButton.setText(LocaleController.getString("Confirm", R.string.Confirm));
        buttonsPairView.confirmButton.setCollapseIcon(R.drawable.ic_done);
        buttonsPairView.confirmButton.setOnClickListener(v -> {
            if (walletBalance.getBalanceSafe().compareTo(BigInteger.valueOf(request.totalAmount)) <= 0) {
                if (balanceCell != null) {
                    AndroidUtilities.shakeAndVibrateView(balanceCell, 2, 0);
                } else {
                    AndroidUtilities.vibrate(getParentFragment().getParentActivity());
                }
                undoView.showWithAction(UndoView.ACTION_INSUFFICIENT_FUNDS);
                return;
            }

            getParentFragment().requestPasscodeOrBiometricAuth(LocaleController.getString("WalletSendConfirmCredentials", R.string.WalletSendConfirmCredentials), (auth -> {
                buttonsPairView.setProgress(true, true, true);
                getTonController().sendTransaction(auth, request, new TonController.SendTransactionCallbacks() {
                    @Override
                    public void onTransactionCreate () {
                        callbacks.onTransactionCreate();
                        getTonController().getTonConnectController().sendResponseToApplication(request, "", j -> {
                            buttonsPairView.setProgress(true, false, true);
                            buttonsPairView.collapseButtons(true);
                        });
                    }

                    @Override
                    public void onTransactionSendSuccess () {
                        callbacks.onTransactionSendSuccess();
                    }

                    @Override
                    public void onTransactionSendError (String text, TonApi.Error error) {
                        callbacks.onTransactionSendError(text, error);
                    }
                });
            }));
        });
    }

    @Override
    protected int getHeight () {
        return linearLayout.getMeasuredHeight() + getPopupPaddingTop() + getScrollPaddingBottom() + getPopupPaddingBottom();
    }

    @Override
    public boolean usePadding () {
        return true;
    }

    @Override
    public int getScrollPaddingBottom () {
        if (request == null || request.messages.length == 1) {
            return AndroidUtilities.dp(64);
        } else {
            return AndroidUtilities.dp(8);
        }
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
