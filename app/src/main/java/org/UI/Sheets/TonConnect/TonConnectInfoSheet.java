package org.UI.Sheets.TonConnect;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.TextView;

import org.TonController.TonConnect.TonConnectController;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Components.IconUrlView;
import org.telegram.ui.Components.LayoutHelper;

public class TonConnectInfoSheet extends BottomSheetScrollable {
    private static final int menu_more = 1;
    private static final int menu_more_terms = 2;
    private static final int menu_more_privacy = 3;

    public TonConnectInfoSheet (BaseFragment parent, TonConnectController.ConnectedApplication application, Runnable onDisconnect) {
        super(parent);

        Context context = parent.getParentActivity();

        actionBar.setBackButtonImage(R.drawable.baseline_close_24);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    if (actionBar.getBackButton().getAlpha() > 0f) {
                        dismiss();
                    }
                } else if (id == menu_more_terms || id == menu_more_privacy) {
                    if (application != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(id == menu_more_terms ? application.manifest.termsOfUseUrl : application.manifest.privacyPolicyUrl));
                        context.startActivity(intent);
                    }
                }
            }
        });
        if (!TextUtils.isEmpty(application.manifest.termsOfUseUrl) || !TextUtils.isEmpty(application.manifest.privacyPolicyUrl)) {
            ActionBarMenu menu = actionBar.createMenu();
            ActionBarMenuItem moreItem = menu.addItem(menu_more, R.drawable.baseline_more_vert_24);
            if (!TextUtils.isEmpty(application.manifest.termsOfUseUrl)) {
                moreItem.addSubItem(menu_more_terms, LocaleController.getString("TonConnectConnectReadTerms", R.string.TonConnectConnectReadTerms));
            }
            if (!TextUtils.isEmpty(application.manifest.privacyPolicyUrl)) {
                moreItem.addSubItem(menu_more_privacy, LocaleController.getString("TonConnectConnectReadPrivacy", R.string.TonConnectConnectReadPrivacy));
            }
        }

        IconUrlView iconImageView = new IconUrlView(context);
        iconImageView.setRadius(AndroidUtilities.dp(20));
        iconImageView.setUrl(application.manifest.iconUrl);
        linearLayout.addView(iconImageView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        TextView titleTextView = LayoutHelper.createTextView(context, 24);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_headerBlackText));
        titleTextView.setText(application.manifest.name);
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 20, 40, 0));

        TextView descriptionTextView = LayoutHelper.createTextView(context, 15, 20);
        descriptionTextView.setGravity(Gravity.CENTER);
        descriptionTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        descriptionTextView.setText(LocaleController.formatString("TonConnectConnectHaveAccess", R.string.TonConnectConnectHaveAccess, application.manifest.getHost()));
        linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 8, 40, 0));

        TextView disclaimerTextView = LayoutHelper.createTextView(context, 15, 20);
        disclaimerTextView.setGravity(Gravity.CENTER);
        disclaimerTextView.setTextColor(Theme.getColor(Theme.key_wallet_grayText));
        disclaimerTextView.setText(LocaleController.formatString("TonConnectConnectDisconnectHint", R.string.TonConnectConnectDisconnectHint, application.manifest.getHost()));
        linearLayout.addView(disclaimerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 36, 40, 0));

        DefaultButtonView buttonTextView = new DefaultButtonView(context);
        buttonTextView.setText(LocaleController.getString("TonConnectDisconnect", R.string.TonConnectDisconnect));
        buttonTextView.setOnClickListener(v -> {
            buttonTextView.showProgress(true, true);
            onDisconnect.run();
            parent.getTonConnectController().disconnect(application, r -> {
                buttonTextView.showProgress(false, true);
                dismiss();
            });
        });
        containerView.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp(8);
    }

    @Override
    public int getPopupPaddingTop () {
        return Math.min(ActionBar.getCurrentActionBarHeight(), AndroidUtilities.dp(44));
    }

    @Override
    public int getPopupPaddingBottom () {
        return AndroidUtilities.dp(80);
    }
}
