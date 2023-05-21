package org.UI.Sheets.TonConnect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.TonController.Data.Wallets;
import org.TonController.TonConnect.TonConnectController;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Components.IconUrlView;
import org.UI.Sheets.Templates.BottomSheetPageScrollable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class TonConnectInitialPage extends BottomSheetPageScrollable {
    private static final int menu_more = 1;
    private static final int menu_more_terms = 2;
    private static final int menu_more_privacy = 3;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private DefaultButtonView connectButton;
    private IconUrlView iconImageView;

    private TonConnectController.ConnectedApplication application;
    private ActionBarMenuItem moreItem;

    @Override
    protected ViewGroup createView (Context context) {
        ViewGroup viewGroup = super.createView(context);

        actionBar.setBackButtonImage(R.drawable.baseline_close_24);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    dismiss();
                } else if (id == menu_more_terms || id == menu_more_privacy) {
                    if (application != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(id == menu_more_terms ? application.manifest.termsOfUseUrl: application.manifest.privacyPolicyUrl));
                        context.startActivity(intent);
                    }
                }
            }
        });

        iconImageView = new IconUrlView(context);
        iconImageView.setRadius(AndroidUtilities.dp(20));
        linearLayout.addView(iconImageView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL));

        titleTextView = LayoutHelper.createTextView(context, 24);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_headerBlackText));
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 20, 40, 0));

        descriptionTextView = LayoutHelper.createTextView(context, 15, 20);
        descriptionTextView.setGravity(Gravity.CENTER);
        descriptionTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 8, 40, 0));

        TextView disclaimerTextView = LayoutHelper.createTextView(context, 15, 20);
        disclaimerTextView.setGravity(Gravity.CENTER);
        disclaimerTextView.setTextColor(Theme.getColor(Theme.key_wallet_grayText));
        disclaimerTextView.setText(LocaleController.getString("TonConnectConnectDisclaimer", R.string.TonConnectConnectDisclaimer));
        linearLayout.addView(disclaimerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 36, 40, 0));

        connectButton = new DefaultButtonView(context);
        connectButton.setText(LocaleController.getString("WalletConnect", R.string.WalletConnect));
        connectButton.setCollapseIcon(R.drawable.ic_done);
        connectButton.setOnClickListener(this::onConnectClick);
        viewGroup.addView(connectButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));

        return viewGroup;
    }

    public void setApplication (TonConnectController.ConnectedApplication application) {
        this.application = application;

        titleTextView.setText(LocaleController.formatString("TonConnectConnectTo", R.string.TonConnectConnectTo, application.manifest.name));

        SpannableStringBuilder builder = new SpannableStringBuilder(LocaleController.formatString("TonConnectRequestingAccess", R.string.TonConnectRequestingAccess, application.manifest.getHost()));
        builder.append(" ");
        int len = builder.length();
        builder.append(Utilities.truncateString(getTonController().getCurrentWalletAddress(), 4, 4));
        builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_dialogTextGray2)), len, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(" ");
        builder.append(Wallets.getWalletVersionName(getTonController().getCurrentWalletType()));
        descriptionTextView.setText(builder);

        iconImageView.setUrl(application.manifest.iconUrl);

        if (moreItem == null && (!TextUtils.isEmpty(application.manifest.termsOfUseUrl) || !TextUtils.isEmpty(application.manifest.privacyPolicyUrl))) {
            ActionBarMenu menu = actionBar.createMenu();
            moreItem = menu.addItem(menu_more, R.drawable.baseline_more_vert_24);
            if (!TextUtils.isEmpty(application.manifest.termsOfUseUrl)) {
                moreItem.addSubItem(menu_more_terms, LocaleController.getString("TonConnectConnectReadTerms", R.string.TonConnectConnectReadTerms));
            }
            if (!TextUtils.isEmpty(application.manifest.privacyPolicyUrl)) {
                moreItem.addSubItem(menu_more_privacy, LocaleController.getString("TonConnectConnectReadPrivacy", R.string.TonConnectConnectReadPrivacy));
            }
        }
    }

    private void onConnectClick (View v) {
        if (application == null) return;

        getParentFragment().requestPasscodeOrBiometricAuth(LocaleController.getString("WalletExportConfirmContinue", R.string.WalletExportConfirmContinue), (auth) -> {
            TonConnectController controller = getTonController().getTonConnectController();
            TonConnectController.InitialResponse response2 = controller.makeNewInitialResponse(application.clientId);
            if (response2.walletProofHash != null) {
                getTonController().sign(auth, response2.walletProofHash, sign -> {
                    response2.setWalletProofSignature(sign);
                    onResponseGenerated(response2);
                }, getParentFragment()::defaultErrorCallback);
            } else {
                onResponseGenerated(response2);
            }
        });
    }

    private void onResponseGenerated (TonConnectController.InitialResponse response) {
        connectButton.setEnabled(false);
        connectButton.showProgress(true, true);
        getTonController().getTonConnectController().finishInitNewConnect(response, () -> {
            connectButton.showProgress(false, true);
            connectButton.collapse();
        }, getParentFragment()::defaultErrorCallback);
    }

    @Override
    public boolean usePadding () {
        return true;
    }

    @Override
    protected int getHeight () {
        return linearLayout.getMeasuredHeight() + getPopupPaddingTop() + getPopupPaddingBottom() + getScrollPaddingBottom();
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
