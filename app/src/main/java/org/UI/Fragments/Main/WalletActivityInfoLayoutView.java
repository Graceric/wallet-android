package org.UI.Fragments.Main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.UI.Cells.WalletActivityButtonsCell;
import org.UI.Cells.WalletCreatedCell;
import org.UI.Cells.WalletSyncCell;
import org.UI.Components.Text.WalletBalanceCounterView;
import org.UI.Utils.Typefaces;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.ListAnimator;
import me.vkryl.android.animator.ReplaceAnimator;
import me.vkryl.core.MathUtils;

public class WalletActivityInfoLayoutView extends FrameLayout {

    private final BoolAnimator actionBarIsFullVisible = new BoolAnimator(0, (id, factor, fraction, callee) -> checkActionBarViewsPosition(), AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final BoolAnimator actionBarStatusTextVisible = new BoolAnimator(0, (id, factor, fraction, callee) -> checkActionBarViewsAlpha(), AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final ReplaceAnimator<Text> walletAddressAnimator = new ReplaceAnimator<>(a -> invalidate(), AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final BoolAnimator walletIsEmptyCellVisible = new BoolAnimator(0, (id, factor, fraction, callee) -> checkWalletStatusCellsAlpha(), AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final BoolAnimator walletIsSyncVisible = new BoolAnimator(0, (id, factor, fraction, callee) -> checkWalletStatusCellsAlpha(), AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final BoolAnimator walletPriceVisibility = new BoolAnimator(0, (id, factor, fraction, callee) -> checkActionBarViewsPosition(), AnimatorUtils.DECELERATE_INTERPOLATOR, 250L);

    private String currentAddress;
    private float addressTextDrawY;
    private boolean isDisplayJettonToken;

    private final WalletBalanceCounterView mainBalanceTextView;
    private final WalletBalanceCounterView balanceCollapsedTextView;
    public final SimpleTextView walletPriceTextView;
    private final WalletCreatedCell walletCreatedCell;
    private final WalletSyncCell walletSyncCell;

    private FloatGetter balanceBottomCellGetter;
    private Runnable onPositionsUpdate;



    public interface FloatGetter {
        float get ();
    }

    public WalletActivityInfoLayoutView (@NonNull Context context) {
        super(context);

        mainBalanceTextView = new WalletBalanceCounterView(context);
        mainBalanceTextView.setOnCounterAppearanceChangedDelegate(this::checkActionBarViewsPosition);
        addView(mainBalanceTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.TOP));

        balanceCollapsedTextView = new WalletBalanceCounterView(context, 22, 2);
        balanceCollapsedTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        balanceCollapsedTextView.setParams(18, false, AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addView(balanceCollapsedTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 22, Gravity.TOP));

        walletPriceTextView = new SimpleTextView(context);
        walletPriceTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        walletPriceTextView.setTypeface(Typefaces.INTER_REGULAR);
        walletPriceTextView.setTextSize(14);
        walletPriceTextView.setAlpha(0f);
        addView(walletPriceTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 16, 31, 100, 0));

        walletCreatedCell = new WalletCreatedCell(context);
        addView(walletCreatedCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM, 0, 56, 0, 0));

        walletSyncCell = new WalletSyncCell(context);
        addView(walletSyncCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM, 0, 56, 0, 0));
    }

    public void setOnBalanceClickListener (@Nullable OnClickListener l) {
        mainBalanceTextView.setOnClickListener(v -> {
            if (actionBarIsFullVisible.getValue() && l != null) {
                l.onClick(v);
            }
        });
    }

    public void setOnPositionsUpdateListener (Runnable onPositionsUpdate) {
        this.onPositionsUpdate = onPositionsUpdate;
    }

    public void setTokenIconUrl (@Nullable String url, boolean animated) {
        mainBalanceTextView.setTokenUrl(url, animated);
        balanceCollapsedTextView.setTokenUrl(url, animated);
    }

    public void setBalance (String balanceRepr, boolean animated) {
        mainBalanceTextView.setBalance(balanceRepr, animated);
        balanceCollapsedTextView.setBalance(balanceRepr, animated);
    }

    public void setAddress (String address, boolean animated) {
        if (TextUtils.equals(address, currentAddress)) {
            return;
        }
        currentAddress = address;
        walletCreatedCell.setAddress(currentAddress);

        if (TextUtils.isEmpty(address)) {
            walletAddressAnimator.clear(animated);
            return;
        }

        walletAddressAnimator.replace(new Text(Utilities.truncateString(address, 4, 4)), animated);
    }

    public void setBalanceBottomCellGetter (FloatGetter balanceBottomCellGetter) {
        this.balanceBottomCellGetter = balanceBottomCellGetter;
    }

    public void checkActionBarViewsPosition () {
        float progress = actionBarIsFullVisible.getFloatValue();
        float bottom = balanceBottomCellGetter != null ? balanceBottomCellGetter.get() : 0;
        int offsetX = (getMeasuredWidth() - mainBalanceTextView.getTextWidth() - AndroidUtilities.dp(48)) / 2;
        int offsetY = (int) Math.max(0, bottom - AndroidUtilities.dp(WalletActivityButtonsCell.HEIGHT));

        float balanceCollapsedStartPosition = AndroidUtilities.dp(7)
            + AndroidUtilities.dp(9) * (1f - walletPriceVisibility.getFloatValue());

        balanceCollapsedTextView.setPivotX(0);
        balanceCollapsedTextView.setPivotY(0);
        balanceCollapsedTextView.setTranslationX(MathUtils.fromTo(AndroidUtilities.dp(16), offsetX, progress));
        balanceCollapsedTextView.setTranslationY(offsetY + MathUtils.fromTo(balanceCollapsedStartPosition, ActionBar.getCurrentActionBarHeight() + AndroidUtilities.dp(39), progress));
        balanceCollapsedTextView.setScaleX(MathUtils.fromTo(1f, 44f / 18f, progress));
        balanceCollapsedTextView.setScaleY(MathUtils.fromTo(1f, 44f / 18f, progress));

        mainBalanceTextView.setPivotX(0);
        mainBalanceTextView.setPivotY(0);
        mainBalanceTextView.setTranslationY(offsetY + MathUtils.fromTo(AndroidUtilities.dp(7), ActionBar.getCurrentActionBarHeight() + AndroidUtilities.dp(39), progress));
        mainBalanceTextView.setTranslationX(MathUtils.fromTo(AndroidUtilities.dp(16), offsetX, progress));
        mainBalanceTextView.setScaleX(MathUtils.fromTo(18f / 44f, 1f, progress));
        mainBalanceTextView.setScaleY(MathUtils.fromTo(18f / 44f, 1f, progress));

        walletSyncCell.setScrollPadding(bottom / 2f);
        walletCreatedCell.setScrollPadding(bottom / 2f);

        addressTextDrawY = offsetY + ActionBar.getCurrentActionBarHeight() + AndroidUtilities.dp(32) * progress;

        checkActionBarViewsAlpha();
        if (onPositionsUpdate != null) {
            onPositionsUpdate.run();
        }
        invalidate();
    }

    public void setStatusBarVisible (boolean statusBarVisible, boolean animated) {
        actionBarStatusTextVisible.setValue(statusBarVisible, animated);
    }

    public void setActionBarIsFullVisible (boolean actionBarFullVisible, boolean animated) {
        actionBarIsFullVisible.setValue(actionBarFullVisible, animated);
    }

    public void setDisplayJettonToken (boolean displayJettonToken) {
        isDisplayJettonToken = displayJettonToken;
        walletPriceVisibility.setValue(!isDisplayJettonToken && !TextUtils.isEmpty(walletPriceTextView.getText()),true);
    }

    public void setCurrencyPriceText (@Nullable String value) {
        walletPriceTextView.setText(value);
        walletPriceVisibility.setValue(!isDisplayJettonToken && !TextUtils.isEmpty(value),true);
    }

    public void setWalletStatusCells (boolean isEmpty, boolean isSync, boolean animated) {
        walletIsEmptyCellVisible.setValue(isEmpty, animated);
        walletIsSyncVisible.setValue(isSync, animated);

        if (isEmpty) walletCreatedCell.start();
        if (isSync) walletSyncCell.start();

        checkWalletStatusCellsAlpha();
    }

    private void checkActionBarViewsAlpha () {
        final float statusTextVisibility = actionBarStatusTextVisible.getFloatValue();
        final float actionBarIsFullVisibility = actionBarIsFullVisible.getFloatValue();
        walletPriceTextView.setAlpha(Math.min(1f - Math.max(actionBarIsFullVisibility, statusTextVisibility), walletPriceVisibility.getFloatValue()));
        balanceCollapsedTextView.setAlpha(1f - Math.max(actionBarIsFullVisibility, statusTextVisibility));
        mainBalanceTextView.setAlpha(actionBarIsFullVisibility);

        invalidate();
    }



    private void checkWalletStatusCellsAlpha () {
        walletSyncCell.setAlpha(walletIsSyncVisible.getFloatValue());
        walletSyncCell.setTranslationY((1f - walletIsSyncVisible.getFloatValue()) * AndroidUtilities.dp(56));
        walletCreatedCell.setAlpha(walletIsEmptyCellVisible.getFloatValue());
        walletCreatedCell.setTranslationY((1f - walletIsEmptyCellVisible.getFloatValue()) * AndroidUtilities.dp(56));
    }

    public float getMainBalanceViewTranslationX () {
        return mainBalanceTextView.getTranslationX();
    }

    public float getMainBalanceViewTranslationY () {
        return mainBalanceTextView.getTranslationY();
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        super.dispatchDraw(canvas);

        for (ListAnimator.Entry<Text> entry : walletAddressAnimator) {
            // float offset = AndroidUtilities.dp(-5) * (1f - entry.getVisibility());
            entry.item.textPaint.setAlpha((int) ((actionBarIsFullVisible.getFloatValue() * entry.getVisibility()) * 255));
            canvas.save();
            canvas.translate((getMeasuredWidth() - entry.item.staticLayout.getLineWidth(0)) / 2f, addressTextDrawY /*+ offset*/);
            entry.item.staticLayout.draw(canvas);
            canvas.restore();
        }
    }

    private static class Text {
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final StaticLayout staticLayout;
        private final String text;

        private Text (String text) {
            this.text = text;
            this.staticLayout = new StaticLayout(
                text, textPaint, AndroidUtilities.dp(300),
                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
            );

            textPaint.setColor(Theme.getColor(Theme.key_wallet_whiteText));
            textPaint.setTextSize(AndroidUtilities.dp(15));
        }

        @Override
        public boolean equals (@Nullable Object obj) {
            return obj instanceof Text && ((Text) obj).text.equals(this.text);
        }

        @Override
        public int hashCode () {
            return text.hashCode();
        }
    }
}
