package org.UI.Fragments.Main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.UI.Cells.WalletDateCell;
import org.UI.Cells.WalletTransactionCell;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class WalletActivityContainerView extends FrameLayout {
    private final GradientDrawable backgroundDrawable;
    private final Paint blackPaint = new Paint();
    public final Interpolator interpolator = new CubicBezierInterpolator(0.65f, -0.25f, 0.75f, 0.75f);
    private float walletPresentVisibilityFactor = 0f;


    public WalletActivityContainerView (@NonNull Context context) {
        super(context);

        int r = AndroidUtilities.dp(12);
        backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        backgroundDrawable.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        backgroundDrawable.setColor(Theme.getColor(Theme.key_wallet_whiteBackground));

        blackPaint.setColor(Theme.getColor(Theme.key_wallet_blackBackground));
    }

    private final RectF clipRect = new RectF();
    private final Path clipPath = new Path();
    private final float[] radii = new float[8];

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onUpdateLayoutRunnable.run();
        radii[0] = AndroidUtilities.dp(12);
        radii[1] = AndroidUtilities.dp(12);
        radii[2] = AndroidUtilities.dp(12);
        radii[3] = AndroidUtilities.dp(12);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (walletPresentVisibilityFactor > 0 && (child instanceof ActionBar || child instanceof WalletActivityInfoLayoutView)) {
            int bottom = (int) getInterpolatedBottom();

            canvas.save();
            canvas.clipRect(0, 0, getMeasuredWidth(), bottom);
            boolean r = super.drawChild(canvas, child, drawingTime);
            canvas.restore();
            return r;
        } else if (child instanceof WalletTransactionCell || child instanceof WalletDateCell) {
            int bottom = (int) getInterpolatedBottom();
            clipRect.set(0, bottom, getMeasuredWidth(), getMeasuredHeight());
            clipPath.reset();
            clipPath.addRoundRect(clipRect, radii, Path.Direction.CW);
            clipPath.close();

            canvas.save();
            canvas.clipPath(clipPath);
            boolean r = super.drawChild(canvas, child, drawingTime);
            canvas.restore();
            return r;
        } else {
            return super.drawChild(canvas, child, drawingTime);
        }
    }

    public void setWalletPresentVisibilityFactor (float walletPresentVisibilityFactor) {
        this.walletPresentVisibilityFactor = interpolator.getInterpolation(walletPresentVisibilityFactor);
        float r = AndroidUtilities.dp(12 * (1f - walletPresentVisibilityFactor));
        backgroundDrawable.setCornerRadii(new float[] { r, r, r, r, 0, 0, 0, 0 });
        invalidate();
    }

    public float getInterpolatedBottom () {
        return ((bottomCellPositionGetter.get()
            + ActionBar.getCurrentActionBarHeight()
            + AndroidUtilities.statusBarHeight) * (1f - walletPresentVisibilityFactor));
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        final int bottom = (int) getInterpolatedBottom();

        canvas.drawRect(0, 0, getMeasuredWidth(), bottom + AndroidUtilities.dp(12), blackPaint);
        backgroundDrawable.setBounds(0, bottom, getMeasuredWidth(), getMeasuredHeight());
        backgroundDrawable.draw(canvas);

        super.dispatchDraw(canvas);

        /*if (walletPresentVisibilityFactor > 0f) {
            foregroundDrawable.setBounds(0, bottom, getMeasuredWidth(), getMeasuredHeight());
            // foregroundDrawable.setAlpha((int) (MathUtils.clamp(walletPresentVisibilityFactor) * 255f));
            foregroundDrawable.draw(canvas);
        }*/
    }

    public GradientDrawable getBackgroundDrawable () {
        return backgroundDrawable;
    }

    private Runnable onUpdateLayoutRunnable;
    private FloatGetter bottomCellPositionGetter;
    public void setOnUpdateLayoutRunnable (Runnable onUpdateLayoutRunnable) {
        this.onUpdateLayoutRunnable = onUpdateLayoutRunnable;
    }

    public void setBottomCellPositionGetter (FloatGetter bottomCellPositionGetter) {
        this.bottomCellPositionGetter = bottomCellPositionGetter;
    }

    public interface FloatGetter {
        float get ();
    }
}
