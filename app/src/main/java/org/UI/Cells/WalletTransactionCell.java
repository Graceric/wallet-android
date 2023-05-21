package org.UI.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.TonController.Data.TransactionMessageRepresentation;
import org.TonController.Data.WalletTransaction;
import org.TonController.Data.WalletTransactionMessage;
import org.UI.Components.IconUrlView;
import org.UI.Components.Text.BalanceValueTextView;
import org.UI.Components.Text.MessageActionTextView;
import org.UI.Fragments.Main.WalletActivityListAdapter;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class WalletTransactionCell extends LinearLayout {

    private final BalanceValueTextView valueTextView;
    private final TextView fromTextView;
    private final TextView dateTextView;
    private final TextView addressValueTextView;
    private final MessageActionTextView commentTextView;
    private final TextView feeTextView;
    private final ImageView clockImage;
    private final LinearLayout addressLinearLayout;
    private final IconUrlView currencyIcon;

    private WalletTransactionMessage walletTransactionMessage;
    private @Nullable RootJettonContract rootJettonContract;

    private boolean drawDivider;
    private boolean isEmpty;
    private int mode;

    public WalletTransactionCell (Context context) {
        super(context);
        setOrientation(VERTICAL);

        LinearLayout valueLinearLayout = new LinearLayout(context);
        valueLinearLayout.setOrientation(HORIZONTAL);
        addView(valueLinearLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        currencyIcon = new IconUrlView(context);
        currencyIcon.setRadius(AndroidUtilities.dp(9));
        valueLinearLayout.addView(currencyIcon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 3, 0));

        valueTextView = new BalanceValueTextView(context);
        valueTextView.setCurrencyDrawable(R.drawable.baseline_gem_static_18, 3);
        valueLinearLayout.addView(valueTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 3, 0));

        fromTextView = new TextView(context);
        fromTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        fromTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        valueLinearLayout.addView(fromTextView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        clockImage = new ImageView(context);
        clockImage.setImageResource(R.drawable.msg_clock);
        clockImage.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6), PorterDuff.Mode.MULTIPLY));
        valueLinearLayout.addView(clockImage, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 0, 8, 0, 0));

        dateTextView = new TextView(context);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        valueLinearLayout.addView(dateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 4, 0, 0, 0));

        addressLinearLayout = new LinearLayout(context);
        addressLinearLayout.setOrientation(HORIZONTAL);
        addView(addressLinearLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        addressValueTextView = new TextView(context);
        addressValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addressValueTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        addressLinearLayout.addView(addressValueTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.LEFT | Gravity.TOP));

        feeTextView = new TextView(context);
        feeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        feeTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        addView(feeTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        commentTextView = new MessageActionTextView(context);
        addView(commentTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        setNeedTopPadding(true);
    }

    public void setRootJettonContract (@Nullable RootJettonContract rootJettonContract) {
        this.rootJettonContract = rootJettonContract;
        if (rootJettonContract != null) {
            currencyIcon.setVisibility(VISIBLE);
            currencyIcon.setUrl(rootJettonContract.content.imageUrl);
            valueTextView.setCurrencyDrawable(null, 0);
        } else {
            currencyIcon.setVisibility(GONE);
            valueTextView.setCurrencyDrawable(R.drawable.baseline_gem_static_18, 3);
        }
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    public void setNeedTopPadding (boolean needTopPadding) {
        setPadding(AndroidUtilities.dp(16), needTopPadding ? AndroidUtilities.dp(14) : 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));
    }

    public void setTransaction (WalletTransactionMessage transactionMessage, int mode, boolean divider) {
        walletTransactionMessage = transactionMessage;
        WalletTransaction transaction = transactionMessage.walletTransaction;
        final boolean isPending = transaction.isPending();
        isEmpty = transaction.isEmpty();

        if (isPending) {
            Log.wtf("TAG", "?");
        }

       /* if (isEmpty && isPending && mode == WalletActivityListAdapter.TRANSACTIONS_MODE_JETTON && transaction.isInternalTokenSend()) {
            isEmpty = false;
        }*/

        this.mode = mode;

        addressLinearLayout.setVisibility(isEmpty ? GONE: VISIBLE);
        valueTextView.setVisibility(isEmpty ? GONE: VISIBLE);

        TransactionMessageRepresentation info = transactionMessage.getRepresentation(mode, rootJettonContract);
        dateTextView.setText(info.dateTimeShort);
        commentTextView.setTransactionMessage(info.payload,
            mode == WalletActivityListAdapter.TRANSACTIONS_MODE_DEFAULT ?
            transactionMessage.stateInit: null, true);
        setCellValue(info.valueChange, info.feeTextShort, isPending, info.senderOrReceiverAddress);

        if (info.senderOrReceiverAddress == null) {
            addressLinearLayout.setVisibility(GONE);
            valueTextView.setVisibility(GONE);
            fromTextView.setText(LocaleController.getString("WalletEmptyTransaction", R.string.WalletEmptyTransaction));
        }

        drawDivider = divider;
        setWillNotDraw(!divider);
    }

    private void setCellValue (TonAmount value, CharSequence feeText, boolean isPending, String address) {
        clockImage.setVisibility(isPending ? VISIBLE : GONE);
        valueTextView.setTextColor(Theme.getColor(value.isMoreThanZero() ? Theme.key_wallet_greenText: Theme.key_wallet_redText));
        valueTextView.setValue(value);
        feeTextView.setVisibility(TextUtils.isEmpty(feeText) ? GONE: VISIBLE);
        feeTextView.setText(feeText);
        addressValueTextView.setText(address != null ? Utilities.truncateString(address, 6, 7): "");
        if (value.isMoreThanZero()) {
            fromTextView.setText(LocaleController.getString("WalletFrom", R.string.WalletFrom));
        } else {
            if (!isEmpty) {
                StringBuilder dest = new StringBuilder(LocaleController.getString("WalletTo", R.string.WalletTo));
                if (walletTransactionMessage != null && walletTransactionMessage.getToDomain() != null) {
                    dest.append(" ");
                    dest.append(walletTransactionMessage.getToDomain());
                }
                fromTextView.setText(dest.toString());
            } else {
                fromTextView.setText(LocaleController.getString("WalletEmptyTransaction", R.string.WalletEmptyTransaction));
            }
        }
    }

    @Override
    protected void onDraw (Canvas canvas) {
        if (drawDivider) {
            canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Nullable
    public RootJettonContract getRootJettonContract () {
        return rootJettonContract;
    }

    public WalletTransactionMessage getWalletTransactionMessage () {
        return walletTransactionMessage;
    }

    public TransactionMessageRepresentation getWalletTransactionRepresentation () {
        return walletTransactionMessage.getRepresentation(mode, rootJettonContract);
    }

    @Deprecated
    public boolean isEmpty () {
        return isEmpty;
    }
}
