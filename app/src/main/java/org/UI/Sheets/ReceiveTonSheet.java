package org.UI.Sheets;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class ReceiveTonSheet extends BottomSheetScrollable {
    private final BaseFragment fragment;
    private final String walletAddress;

    public ReceiveTonSheet (BaseFragment fragment, String address) {
        super(fragment);
        this.fragment = fragment;
        this.walletAddress = address;

        init(fragment.getParentActivity());
    }

    private void init (Context context) {
        String url = "ton://transfer/" + walletAddress;
        actionBar.setTitle(LocaleController.getString("WalletReceiveYourAddress", R.string.WalletReceiveYourAddress));
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

        TextView descriptionText = new TextView(context);
        descriptionText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        descriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText.setLineSpacing(AndroidUtilities.dp(3), 1);
        descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        descriptionText.setText(LocaleController.getString("WalletShareInfo", R.string.WalletShareInfo));
        descriptionText.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        linearLayout.addView(descriptionText, LayoutHelper.createLinear(320, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16, 0, 0));

        TextView addressValueTextView = new TextView(context);
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(Utilities.createTonQR(context, url, null));
        linearLayout.addView(imageView, LayoutHelper.createLinear(160, 160, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 26, 0, 0));
        imageView.setOnLongClickListener(v -> {
            Utilities.shareBitmap(fragment.getParentActivity(), v, url);
            return true;
        });

        addressValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        addressValueTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        addressValueTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        StringBuilder stringBuilder = new StringBuilder(walletAddress);
        stringBuilder.insert(stringBuilder.length() / 2, '\n');
        addressValueTextView.setText(stringBuilder);
        linearLayout.addView(addressValueTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 28, 0, 0));
        addressValueTextView.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(url);
            Toast.makeText(fragment.getParentActivity(), LocaleController.getString("WalletTransactionAddressCopied", R.string.WalletTransactionAddressCopied), Toast.LENGTH_SHORT).show();
        });
        addressValueTextView.setBackgroundDrawable(Theme.getSelectorDrawable(false));

        DefaultButtonView buttonTextView = new DefaultButtonView(context);
        buttonTextView.setText(LocaleController.getString("WalletShareAddress", R.string.WalletShareAddress));
        buttonTextView.setOnClickListener(v -> AndroidUtilities.openSharing(fragment, url));
        containerView.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp(12);
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
