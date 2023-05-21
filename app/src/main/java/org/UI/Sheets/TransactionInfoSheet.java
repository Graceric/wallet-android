package org.UI.Sheets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.WalletTransactionMessage;
import org.TonController.Data.TransactionMessageRepresentation;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsTextCell;
import org.UI.Cells.WalletTransactionBalanceCell;
import org.telegram.ui.Components.LayoutHelper;
import org.Utils.Callbacks;

public class TransactionInfoSheet extends BottomSheetScrollable {
    private AnimatorSet actionBarAnimation;

    private final @Nullable RootJettonContract rootJettonContract;
    private final String senderAddress;
    private final TransactionMessageRepresentation representation;

    private final WalletTransactionMessage currentTransaction;
    private final boolean isEmpty;

    private final WalletTransactionBalanceCell transactionAmountCell;
    private String currentTransactionHash;
    private boolean isPending;



    private Callbacks.StringCallback onClickSendDelegate;

    public TransactionInfoSheet (BaseFragment fragment, WalletTransactionMessage transaction, TransactionMessageRepresentation representation, RootJettonContract rootJettonContract) {
        super(fragment);

        Context context = fragment.getParentActivity();

        this.representation = representation;
        senderAddress = getTonController().getCurrentWalletAddress();
        long currentDate = transaction.transaction.utime;
        currentTransaction = transaction;
        isPending = currentTransaction.transaction.transactionId.lt == 0;
        isEmpty = Utilities.isEmptyTransaction(transaction.transaction);
        if (currentTransaction.transaction.transactionId.hash != null) {
            currentTransactionHash = Base64.encodeToString(currentTransaction.transaction.transactionId.hash, Base64.URL_SAFE);
        }

        transactionAmountCell = new WalletTransactionBalanceCell(context);
        this.rootJettonContract = rootJettonContract;

        init(fragment.getParentActivity());

        getAccountsStateManager().subscribeToPendingTransactionComplete(senderAddress, currentTransaction.transaction, this::onTransactionSendComplete);
        setOnDismissListener(this::onDismiss);
    }

    public void setOnClickSendDelegate (Callbacks.StringCallback onClickSendDelegate) {
        this.onClickSendDelegate = onClickSendDelegate;
    }

    private void init (Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.getBackButton().setAlpha(0f);
        actionBar.getTitleTextView().setTranslationX(-AndroidUtilities.dp(52));
        actionBar.setTitle(LocaleController.getString("WalletTransaction", R.string.WalletTransaction));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    if (actionBar.getBackButton().getAlpha() > 0f) {
                        dismiss();
                    }
                }
            }
        });

        if (!TextUtils.isEmpty(representation.senderOrReceiverAddress)) {
            DefaultButtonView buttonTextView = new DefaultButtonView(context);
            buttonTextView.setText(LocaleController.formatString("WalletTransactionSendX", R.string.WalletTransactionSendX, rootJettonContract != null ? rootJettonContract.content.getSymbolOrName(): "TON"));
            containerView.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM, 16, 16, 16, 16));
            buttonTextView.setOnClickListener(v -> {
                if (onClickSendDelegate != null) {
                    onClickSendDelegate.run(representation.senderOrReceiverAddress.replace("\n", ""));
                }
                dismiss();
            });
        }

        initItems(context);
    }

    private void initItems (Context context) {
        transactionAmountCell.setBalance(representation);
        transactionAmountCell.setStatus(isPending);
        linearLayout.addView(transactionAmountCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        HeaderCell headerCell = new HeaderCell(context, 21, 12, false);
        headerCell.setText(LocaleController.getString("WalletTransactionDetails", R.string.WalletTransactionDetails));
        linearLayout.addView(headerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (!TextUtils.isEmpty(currentTransaction.getToDomain())) {
            SettingsTextCell infoCell = makeInfoCell(context);
            infoCell.setTextAndValue(LocaleController.getString("WalletTransactionRecipientDomain", R.string.WalletTransactionRecipientDomain), currentTransaction.getToDomain(), true);
            infoCell.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard("ton://transfer/" + representation.senderOrReceiverAddress.replace("\n", ""));
                Toast.makeText(v.getContext(), LocaleController.getString("WalletTransactionAddressCopied", R.string.WalletTransactionAddressCopied), Toast.LENGTH_SHORT).show();
            });
            linearLayout.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        if (!TextUtils.isEmpty(representation.senderOrReceiverAddress)) {
            SettingsTextCell infoCell = makeInfoCell(context);
            if (representation.valueChange.isMoreThanZero()) {
                infoCell.setTextAndValue(LocaleController.getString("WalletTransactionSender", R.string.WalletTransactionSender), Utilities.truncateString(representation.senderOrReceiverAddress, 5, 5), true);
            } else {
                infoCell.setTextAndValue(LocaleController.getString("WalletTransactionRecipient", R.string.WalletTransactionRecipient), Utilities.truncateString(representation.senderOrReceiverAddress, 5, 5), true);
            }
            infoCell.setOnClickListener(v -> {
                AndroidUtilities.addToClipboard("ton://transfer/" + representation.senderOrReceiverAddress.replace("\n", ""));
                Toast.makeText(v.getContext(), LocaleController.getString("WalletTransactionAddressCopied", R.string.WalletTransactionAddressCopied), Toast.LENGTH_SHORT).show();
            });
            linearLayout.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        if (!isPending && !TextUtils.isEmpty(currentTransactionHash)) {
            SettingsTextCell infoCell = makeInfoCell(context);
            infoCell.setTextAndValue(LocaleController.getString("WalletTransaction", R.string.WalletTransaction), Utilities.truncateString(currentTransactionHash, 6, 6), true);
            infoCell.setOnClickListener(v -> {

            });
            linearLayout.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        if (!isPending && !TextUtils.isEmpty(currentTransactionHash)) {
            SettingsTextCell infoCell = makeInfoCell(context);
            infoCell.setText(LocaleController.getString("WalletTransactionShowInExplorer", R.string.WalletTransactionShowInExplorer), false);
            infoCell.setTextColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
            infoCell.setOnClickListener(v -> {
                Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tonscan.org/tx/" + currentTransactionHash));
                getContext().startActivity(activityIntent);
            });
            linearLayout.addView(infoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private SettingsTextCell makeInfoCell (Context context) {
        SettingsTextCell infoCell = new SettingsTextCell(context);
        infoCell.setEnabled(true);
        infoCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
        infoCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        return infoCell;
    }




    private void onTransactionSendComplete () {
        transactionAmountCell.setStatus(isPending = false);
    }

    public void onDismiss (DialogInterface var1) {
        getAccountsStateManager().unsubscribeFromPendingTransactionComplete(senderAddress, currentTransaction.transaction, this::onTransactionSendComplete);
    }

    @Override
    protected void onUpdateShowTopShadow (boolean show, boolean animated) {
        if (actionBarAnimation != null) {
            actionBarAnimation.cancel();
            actionBarAnimation = null;
        }
        if (animated) {
            actionBarAnimation = new AnimatorSet();
            actionBarAnimation.setDuration(180);
            actionBarAnimation.playTogether(
                ObjectAnimator.ofFloat(actionBar.getBackButton(), View.ALPHA, show ? 1.0f : 0.0f),
                ObjectAnimator.ofFloat(actionBar.getTitleTextView(), View.TRANSLATION_X, show ? 0.0f : -AndroidUtilities.dp(52))
            );
            actionBarAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd (Animator animation) {
                    actionBarAnimation = null;
                }
            });
            actionBarAnimation.start();
        } else {
            actionBar.getBackButton().setAlpha(show ? 1.0f : 0.0f);
            actionBar.getTitleTextView().setTranslationX(show ? 0.0f : -AndroidUtilities.dp(52));
        }
    }

    @Override
    public int getPopupPaddingTop () {
        return ActionBar.getCurrentActionBarHeight();
    }

    @Override
    public int getPopupPaddingBottom () {
        return AndroidUtilities.dp((isEmpty || representation == null || representation.senderOrReceiverAddress == null) ? 0 : 80);
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp((isEmpty|| representation == null || representation.senderOrReceiverAddress == null) ? 32 : 8);
    }
}
