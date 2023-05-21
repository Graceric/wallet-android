package org.UI.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.Utils.network.BitmapReceiver;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class IconUrlView extends FrameLayout {
    private final ImageView imageView;
    private final RectF rectF = new RectF();
    private final Path path = new Path();
    private final Paint loadingPaint = new Paint();
    private float radius;

    private final BitmapReceiver bitmapReceiver;
    private boolean drawLoading = true;

    public IconUrlView (@NonNull Context context) {
        super(context);

        bitmapReceiver = new BitmapReceiver();

        imageView = new ImageView(context);
        imageView.setVisibility(GONE);
        imageView.setAlpha(0f);
        loadingPaint.setColor(Theme.getColor(Theme.key_wallet_loadingBackgroundColor));

        addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    public void setNeedDrawLoadingBg (boolean drawLoading) {
        this.drawLoading = drawLoading;
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        rectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        updatePath();
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        loadingPaint.setAlpha((int) ((1f - imageView.getAlpha()) * 64));
        canvas.save();
        canvas.clipPath(path);
        if (drawLoading) {
            canvas.drawRect(rectF, loadingPaint);
        }
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private void setBitmap (Bitmap bitmap) {
        setBitmap(bitmap, true);
    }

    public void setUrl (String url) {
        setBitmap(bitmapReceiver.receive(url, this::setBitmap), false);
    }

    public void setBitmap (Bitmap bitmap, boolean animated) {
        imageView.animate().cancel();
        if (bitmap == null) {
            imageView.setVisibility(GONE);
            imageView.setAlpha(0f);
            invalidate();
            return;
        }

        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(VISIBLE);
        if (animated) {
            imageView.animate().alpha(1f).setUpdateListener(valueAnimator -> {
                invalidate();
            }).setDuration(200L).start();
        } else {
            imageView.setAlpha(1f);
        }
        invalidate();
    }

    public void setRadius (float radius) {
        this.radius = radius;
        updatePath();
        invalidate();
    }

    private void updatePath () {
        path.reset();
        path.addRoundRect(rectF, radius, radius, Path.Direction.CW);
        path.close();
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
        setBitmap(bitmapReceiver.receive(bitmapReceiver.getCurrentUrl(), this::setBitmap), false);
    }

    @Override
    protected void onDetachedFromWindow () {
        super.onDetachedFromWindow();
        bitmapReceiver.stop();
    }
}
