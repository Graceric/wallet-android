package org.UI.Cells;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.TonController.Data.TonAmount;
import org.TonController.Data.TransactionMessageRepresentation;
import org.UI.Components.Text.BalanceValueTextView;
import org.UI.Components.Text.MessageActionTextView;
import org.UI.Utils.Typefaces;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RadialProgressView;

public class WalletTransactionBalanceCell extends LinearLayout {
    private final BalanceValueTextView balanceTextView;
    private final TextView feeAndDateTextView;
    private final MessageActionTextView commentTextView;

    private final LinearLayout statusLinearLayout;
    private final TextView statusTextView;

    public WalletTransactionBalanceCell (Context context) {
        super(context);

        setPadding(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(12));
        setOrientation(VERTICAL);

        balanceTextView = new BalanceValueTextView(context);
        RLottieDrawable gemDrawable = new RLottieDrawable(R.raw.wallet_main, "" + R.raw.wallet_main, AndroidUtilities.dp(44), AndroidUtilities.dp(44), false);
        gemDrawable.setAutoRepeat(1);
        gemDrawable.setAllowDecodeSingleFrame(true);
        gemDrawable.addParentView(balanceTextView);
        balanceTextView.setCurrencyDrawable(gemDrawable, 4);
        gemDrawable.start();

        balanceTextView.setTypeface(Typefaces.GOOGLE_SANS_MEDIUM);
        balanceTextView.setTextSize(44);
        balanceTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        addView(balanceTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        feeAndDateTextView = new TextView(context);
        feeAndDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        feeAndDateTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        feeAndDateTextView.setLineSpacing(AndroidUtilities.dp(3), 1);
        feeAndDateTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        addView(feeAndDateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        statusLinearLayout = new LinearLayout(context);
        statusLinearLayout.setVisibility(GONE);
        statusLinearLayout.setOrientation(HORIZONTAL);

        RadialProgressView progressView = new RadialProgressView(context);
        progressView.setSize(AndroidUtilities.dp(11));
        progressView.setStrokeWidth(1.5f);
        progressView.setProgressColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        statusLinearLayout.addView(progressView, LayoutHelper.createLinear(14, 14, Gravity.BOTTOM, 0, 0, 6, 2));

        statusTextView = new TextView(context);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        statusLinearLayout.addView(statusTextView);

        addView(statusLinearLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        commentTextView = new MessageActionTextView(context);
        commentTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        commentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        commentTextView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), Theme.getColor(Theme.key_wallet_transactionCommentBackground)));
        commentTextView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        addView(commentTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 16, 16, 0));
    }

    public void setStatus (boolean isPending) {
        if (isPending) {
            statusTextView.setText(LocaleController.getString("WalletPendingTransactions", R.string.WalletPendingTransactions));
            statusTextView.setTextColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        }
        statusLinearLayout.setVisibility(isPending ? VISIBLE : GONE);
    }

    public void setBalance (TonAmount balance) {
        setPadding(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(24));
        balanceTextView.setValue(balance);
        feeAndDateTextView.setVisibility(GONE);
        commentTextView.setVisibility(GONE);
        statusLinearLayout.setVisibility(GONE);
    }

    public void setBalance (TransactionMessageRepresentation representation) {
        balanceTextView.setValue(representation.valueChange);
        if (representation.valueChange.isMoreThanZero()) {
            balanceTextView.setTextColor(Theme.getColor(Theme.key_wallet_greenText));
        } else {
            balanceTextView.setTextColor(Theme.getColor(Theme.key_wallet_redText));
        }

        StringBuilder builder = new StringBuilder();
        if (representation.feeTextFull != null) {
            builder.append(representation.feeTextFull);
            builder.append('\n');
        }
        builder.append(representation.dateTimeFull);
        commentTextView.setTransactionMessage(representation.payload, null, false);
        feeAndDateTextView.setText(builder);
        feeAndDateTextView.setVisibility(VISIBLE);
    }
}
