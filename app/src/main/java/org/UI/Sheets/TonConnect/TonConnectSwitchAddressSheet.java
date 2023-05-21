package org.UI.Sheets.TonConnect;

import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.TonController.AccountsStateManager;
import org.TonController.Data.Jettons.JettonContract;
import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.Wallets;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonConnect.TonConnectController;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Components.Buttons.DefaultButtonsPairView;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Components.IconUrlView;
import org.telegram.ui.Components.LayoutHelper;

public class TonConnectSwitchAddressSheet extends BottomSheetScrollable {
    private final TonConnectController.ConnectedApplication application;
    private final RequestSendTransaction request;

    private final @Nullable RootJettonContract rootJettonContract;
    private final @Nullable JettonContract jettonWallet;

    private final IconUrlView iconImageView;
    private final TextView titleTextView;
    private final TextView descriptionTextView;
    private final TextView disclaimerTextView;
    private final DefaultButtonsPairView buttonsPairView;
    private Runnable onSwitchDelegate;
    private boolean needRepeatRequest;
    private int walletType = -1;

    public TonConnectSwitchAddressSheet (BaseFragment parent, RequestSendTransaction requestSendTransaction, @Nullable RootJettonContract rootJettonContract, @Nullable JettonContract jettonWallet) {
        super(parent);
        this.request = requestSendTransaction;
        this.application = request.application;
        this.rootJettonContract = rootJettonContract;
        this.jettonWallet = jettonWallet;

        Context context = parent.getParentActivity();
        iconImageView = new IconUrlView(context);
        titleTextView = LayoutHelper.createTextView(context, 24);
        descriptionTextView = LayoutHelper.createTextView(context, 15, 20);
        disclaimerTextView = LayoutHelper.createTextView(context, 15, 20);
        buttonsPairView = new DefaultButtonsPairView(context);

        init();
        initTexts();
        initButtons();
    }

    public void onDismiss (DialogInterface var1) {
        if (needRepeatRequest && onSwitchDelegate != null) {
            onSwitchDelegate.run();
        }
    }

    public void setOnSwitchDelegate (Runnable onSwitchDelegate) {
        this.onSwitchDelegate = onSwitchDelegate;
    }

    private void init () {
        iconImageView.setRadius(AndroidUtilities.dp(20));
        iconImageView.setUrl(application.manifest.iconUrl);
        linearLayout.addView(iconImageView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_headerBlackText));
        titleTextView.setText(LocaleController.formatString("TonConnectApp", R.string.TonConnectApp, application.manifest.name));
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 20, 40, 0));

        descriptionTextView.setGravity(Gravity.CENTER);
        descriptionTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 8, 40, 0));

        disclaimerTextView.setGravity(Gravity.CENTER);
        disclaimerTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        disclaimerTextView.setText(LocaleController.getString("TonConnectRequestingAddressSwitch", R.string.TonConnectRequestingAddressSwitch));
        linearLayout.addView(disclaimerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 36, 40, 0));

        containerView.addView(buttonsPairView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));
    }

    private void initTexts () {
        SpannableStringBuilder builder = new SpannableStringBuilder(LocaleController.formatString("TonConnectRequestingAccessTransaction", R.string.TonConnectRequestingAccessTransaction, application.manifest.getHost()));
        builder.append(" ");
        int len = builder.length();
        builder.append(Utilities.truncateString(request.from, 4, 4));
        builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_dialogTextGray2)), len, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(" ");
        AccountsStateManager.WalletInfo info = parentFragment.getTonAccountStateManager().getSupportedWalletsMapWithAddressKey().get(request.from);
        if (info == null) return;
        builder.append(Wallets.getWalletVersionName(walletType = info.type));
        builder.append(".\n\n");

        String address = rootJettonContract != null ? (jettonWallet != null ? jettonWallet.address: null): getTonController().getCurrentWalletAddress();

        builder.append(LocaleController.getString("TonConnectRequestingAccessTransactionBut", R.string.TonConnectRequestingAccessTransactionBut));
        builder.append(" ");
        len = builder.length();
        if (address != null) {
            builder.append(Utilities.truncateString(address, 4, 4));
        } else {
            builder.append("Unknown");
        }
        builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_dialogTextGray2)), len, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(" ");
        builder.append(
            rootJettonContract != null ? rootJettonContract.content.getSymbolOrName() + " wallet":
            Wallets.getWalletVersionName(getTonController().getCurrentWalletType())
        );
        builder.append(".");

        descriptionTextView.setText(builder);
    }

    private void initButtons () {
        buttonsPairView.cancelButton.setText(LocaleController.getString("Cancel", R.string.Cancel));
        buttonsPairView.cancelButton.setCollapseIcon(R.drawable.baseline_close_24);
        buttonsPairView.cancelButton.setType(DefaultButtonView.TYPE_DEFAULT);
        buttonsPairView.cancelButton.setOnClickListener(v -> {
            buttonsPairView.setProgress(false, true, true);
            parentFragment.getTonConnectController().sendErrorToApplication(request, 300, "User declined the request", j -> {
                buttonsPairView.setProgress(false, false, true);
                buttonsPairView.collapseButtons(false);
            });
        });

        buttonsPairView.confirmButton.setText(LocaleController.getString("Switch", R.string.Switch));
        buttonsPairView.confirmButton.setCollapseIcon(R.drawable.ic_done);
        buttonsPairView.confirmButton.setOnClickListener(v -> {
            if (walletType == -1) return;
            buttonsPairView.setProgress(true, true, true);
            getTonController().updateWalletAddressAndInfo(walletType);
            needRepeatRequest = true;
            buttonsPairView.setProgress(true, false, true);
            dismiss();
        });
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp(8);
    }

    @Override
    public int getPopupPaddingTop () {
        return AndroidUtilities.dp(44);
    }

    @Override
    public int getPopupPaddingBottom () {
        return AndroidUtilities.dp(80);
    }
}
