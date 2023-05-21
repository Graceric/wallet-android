package org.UI.Sheets.Jetton;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.Gravity;
import android.widget.TextView;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Components.IconUrlView;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.UI.Utils.CurrencyUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsTextCell;
import org.telegram.ui.Components.LayoutHelper;

public class TokenInfoSheet extends BottomSheetScrollable {


    public TokenInfoSheet (BaseFragment parent, RootJettonContract contract, Runnable onRemoveDelegate) {
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
                }
            }
        });

        IconUrlView iconImageView = new IconUrlView(context);
        iconImageView.setRadius(AndroidUtilities.dp(20));
        iconImageView.setUrl(contract.content.imageUrl);
        linearLayout.addView(iconImageView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        TextView titleTextView = LayoutHelper.createTextView(context, 24);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_headerBlackText));
        titleTextView.setText(contract.content.name);
        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 20, 40, 0));

        TextView descriptionTextView = LayoutHelper.createTextView(context, 15, 20);
        // descriptionTextView.setGravity(Gravity.CENTER);
        descriptionTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        descriptionTextView.setText(contract.content.description);
        linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 8, 20, 16));

        {
            SettingsTextCell textCell = new SettingsTextCell(context);
            textCell.setTextAndValue("Minter address", contract.adminAddress != null ? Utilities.truncateString(contract.adminAddress, 6, 6): "None", true);
            textCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
            linearLayout.addView(textCell);
        }
        {
            SpannableStringBuilder ssb = new SpannableStringBuilder("  ");
            ssb.setSpan(CurrencyUtils.createSpan(18, contract), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(TonAmount.valueOf(contract.totalSupply, contract.content.decimals).toString());

            SettingsTextCell textCell = new SettingsTextCell(context);
            textCell.setTextValueColor(Theme.getColor(Theme.key_wallet_blackText));
            textCell.setTextAndValue("Total supply", ssb, true);
            linearLayout.addView(textCell);
        }

        SettingsTextCell infoCell = new SettingsTextCell(context);
        infoCell.setText(LocaleController.getString("WalletTransactionShowInExplorer", R.string.WalletTransactionShowInExplorer), false);
        infoCell.setTextColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        infoCell.setBackground(Theme.getSelectorDrawable(false));
        infoCell.setEnabled(true);
        infoCell.setOnClickListener(v -> {
            Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tonscan.org/address/" + contract.address));
            getContext().startActivity(activityIntent);
        });
        linearLayout.addView(infoCell);



        /*
        TextView disclaimerTextView = LayoutHelper.createTextView(context, 15, 20);
        disclaimerTextView.setGravity(Gravity.CENTER);
        disclaimerTextView.setTextColor(Theme.getColor(Theme.key_wallet_grayText));
        disclaimerTextView.setText(LocaleController.formatString("TonConnectConnectDisconnectHint", R.string.TonConnectConnectDisconnectHint, application.manifest.getHost()));
        linearLayout.addView(disclaimerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 40, 36, 40, 0));
        */


        DefaultButtonView buttonTextView = new DefaultButtonView(context);
        buttonTextView.setType(DefaultButtonView.TYPE_DEFAULT);
        buttonTextView.setText(LocaleController.getString("TonTokenRemove", R.string.TonTokenRemove));
        buttonTextView.setCollapseIcon(R.drawable.ic_done);
        buttonTextView.setOnClickListener(v -> {
            buttonTextView.collapse();
            parent.getTonController().removeJettonToken(contract.address);
            onRemoveDelegate.run();
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
