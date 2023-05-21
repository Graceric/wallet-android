package org.UI.Components.Text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.UI.Components.IconUrlView;
import org.UI.Utils.BalanceCounter;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.UI.Utils.Drawables;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

public class WalletBalanceCounterView extends FrameLayout implements FactorAnimator.Target {
    private final BoolAnimator isTokenLogoVisible = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);

    private final BalanceCounter balanceCounter;
    private final RLottieDrawable gemDrawable;
    private final IconUrlView iconUrlView;
    private final float padding;
    private final float drawableSizeDp;

    private Runnable onCounterAppearanceChangedDelegate;

    public WalletBalanceCounterView (Context context) {
        this(context, 44, 4);
    }

    public WalletBalanceCounterView (Context context, float drawableSizeDp, float drawablePaddingDp) {
        super(context);

        balanceCounter = new BalanceCounter(this::onCounterAppearanceChanged);
        iconUrlView = new IconUrlView(context);
        iconUrlView.setRadius(AndroidUtilities.dp(drawableSizeDp / 2f));
        iconUrlView.setVisibility(GONE);
        iconUrlView.setAlpha(0f);
        // iconUrlView.setNeedDrawLoadingBg(false);
        addView(iconUrlView, LayoutHelper.createFrame(drawableSizeDp, drawableSizeDp, Gravity.CENTER_VERTICAL));

        gemDrawable = new RLottieDrawable(R.raw.wallet_main, "" + R.raw.wallet_main, AndroidUtilities.dp(drawableSizeDp), AndroidUtilities.dp(drawableSizeDp), false);
        gemDrawable.setAutoRepeat(1);
        gemDrawable.setAllowDecodeSingleFrame(true);
        gemDrawable.addParentView(this);
        gemDrawable.setCallback(this);
        gemDrawable.start();

        this.drawableSizeDp = drawableSizeDp;
        this.padding = drawableSizeDp + drawablePaddingDp;
    }

    public void setParams (float textSizeDp, boolean needScale, Typeface typeface) {
        balanceCounter.setParams(textSizeDp, needScale, typeface);
    }

    public void setTokenUrl (@Nullable String tokenIconUrl, boolean animated) {
        isTokenLogoVisible.setValue(!TextUtils.isEmpty(tokenIconUrl), animated);
        if (!TextUtils.isEmpty(tokenIconUrl)) {
            iconUrlView.setUrl(tokenIconUrl);
        }
        iconUrlView.setVisibility(VISIBLE);
    }

    public void setTextColor (int color) {
        balanceCounter.setTextColor(color);
    }

    public void setBalance (String balanceRepr, boolean animated) {
        balanceCounter.setBalance(balanceRepr, animated);
    }

    public int getTextWidth () {
        return balanceCounter.getWidth();
    }

    private void onCounterAppearanceChanged (BalanceCounter counter, boolean sizeChanged) {
        if (onCounterAppearanceChangedDelegate != null) {
            onCounterAppearanceChangedDelegate.run();
        }
        invalidate();
    }

    public void setOnCounterAppearanceChangedDelegate (Runnable onCounterAppearanceChangedDelegate) {
        this.onCounterAppearanceChangedDelegate = onCounterAppearanceChangedDelegate;
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        super.dispatchDraw(canvas);

        gemDrawable.setAlpha((int) (255 * (1f - isTokenLogoVisible.getFloatValue())));
        Drawables.draw(canvas, gemDrawable, 0, (getMeasuredHeight() - AndroidUtilities.dp(drawableSizeDp)) / 2f, null);
        balanceCounter.draw(canvas, AndroidUtilities.dp(padding), getMeasuredHeight() / 2, 1f);
    }


    @Override
    public void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        iconUrlView.setAlpha(factor);
        invalidate();
    }

    @Override
    public void onFactorChangeFinished (int id, float finalFactor, FactorAnimator callee) {
        if (finalFactor == 0f) {
            iconUrlView.setVisibility(GONE);
        }
    }
}