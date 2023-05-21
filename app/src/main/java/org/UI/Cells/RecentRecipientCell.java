package org.UI.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.TonController.Storage.RecentRecipientsStorage;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class RecentRecipientCell extends FrameLayout {
    private final TextView addressTextView;
    private final TextView domainTextView;
    private final TextView dateTextView;
    private boolean needDivider;

    public RecentRecipientCell (@NonNull Context context) {
        super(context);

        addressTextView = LayoutHelper.createTextView(context, 16, 20);
        addressTextView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        addView(addressTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 20, 13, 20, 0));

        dateTextView = LayoutHelper.createTextView(context, 15, 18);
        dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        addView(dateTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 20, 35, 20, 0));

        domainTextView = LayoutHelper.createTextView(context, 15, 18);
        domainTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        addView(domainTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 20, 13, 20, 0));
    }

    public void setRecipient (RecentRecipientsStorage.RecentRecipient recipient, boolean divider) {
        addressTextView.setText(Utilities.truncateString(recipient.address, 4, 4));
        dateTextView.setText(LocaleController.getInstance().formatterDayMonth.format(recipient.time * 1000));
        domainTextView.setText(recipient.domain);

        needDivider = divider;
        setWillNotDraw(!divider);
        invalidate();
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw (Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }
}
