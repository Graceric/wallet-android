package org.UI.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.TonConnect.TonConnectController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Components.IconUrlView;
import org.telegram.ui.Components.LayoutHelper;

public class TripleTextWithIconCell extends FrameLayout {
    private final TextView titleView;
    private final TextView descriptionView;
    private final TextView thirdRowView;
    private final IconUrlView appIconView;
    private boolean needDivider;

    public TripleTextWithIconCell (@NonNull Context context) {
        super(context);
        titleView = LayoutHelper.createTextView(context, 18, -1, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText2), 1);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 85, 10, 0, 0));

        descriptionView = LayoutHelper.createTextView(context, 15, -1, Theme.getColor(Theme.key_wallet_grayText), 1);
        addView(descriptionView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 85, 34, 0, 0));

        thirdRowView = LayoutHelper.createTextView(context, 12, -1, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3), 1);
        addView(thirdRowView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 0, 10, 20, 0));

        appIconView = new IconUrlView(context);
        appIconView.setRadius(AndroidUtilities.dp(16));
        addView(appIconView, LayoutHelper.createFrame(56, 56, Gravity.LEFT, 16, 8, 0, 8));
    }

    public void setRadius (int radius) {
        appIconView.setRadius(radius);
    }

    public void setApp (TonConnectController.ConnectedApplication app, boolean divider) {
        this.needDivider = divider;

        titleView.setText(app.manifest.name);
        descriptionView.setText(app.manifest.getHost());
        appIconView.setUrl(app.manifest.iconUrl);
        thirdRowView.setVisibility(GONE);

        setWillNotDraw(!divider);
        invalidate();
    }

    public void setJetton (RootJettonContract contract, boolean divider) {
        this.needDivider = divider;

        titleView.setText(contract.content.name);
        descriptionView.setText(contract.content.description);
        appIconView.setUrl(contract.content.imageUrl);
        thirdRowView.setVisibility(VISIBLE);
        thirdRowView.setText(contract.content.symbol);

        setWillNotDraw(!divider);
        invalidate();
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(72), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw (Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(AndroidUtilities.dp(88), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }
}
