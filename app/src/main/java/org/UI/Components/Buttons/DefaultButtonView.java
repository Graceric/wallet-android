package org.UI.Components.Buttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Keep;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

import me.vkryl.core.MathUtils;

public class DefaultButtonView extends FrameLayout {
    public static final int TYPE_PRIMARY = 0;
    public static final int TYPE_DEFAULT = 1;
    public static final int TYPE_NO_BG = 2;

    private final SimpleTextView textView;
    private final RadialProgressView progressView;
    private final ImageView collapseIcon;
    private final Typeface defaultTypeface;
    private ObjectAnimator progressAnimator;

    private RectF collapseRect;
    private Path collapsePath;
    private float collapseRadius;
    private final Paint backgroundPaint;
    private int textColor;
    private int forceWidthDp = -1;


    public DefaultButtonView (Context context) {
        super(context);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textView = new SimpleTextView(context);
        textView.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(15);
        defaultTypeface = textView.getTextPaint().getTypeface();
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        progressView = new RadialProgressView(context);
        progressView.setSize(AndroidUtilities.dp(17));
        progressView.setStrokeWidth(2f);
        progressView.setVisibility(GONE);
        addView(progressView, LayoutHelper.createFrame(20, 20, Gravity.RIGHT, 0, 14, 14, 14));

        collapseIcon = new ImageView(context);
        collapseIcon.setVisibility(GONE);
        addView(collapseIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        setEnabled(true);
        setType(TYPE_PRIMARY);
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        if (forceWidthDp == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        textView.measure(MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.EXACTLY), heightMeasureSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(forceWidthDp), textView.getTextWidth() + AndroidUtilities.dp(50)), MeasureSpec.EXACTLY), heightMeasureSpec);
    }

    public void setForceWidth (int forceWidthDp) {
        this.forceWidthDp = forceWidthDp;
    }

    public void setType (int type) {
        if (type == TYPE_PRIMARY) {
            setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_wallet_defaultTonBlue), 0x30000000));
            backgroundPaint.setColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
            textView.setTextColor(textColor = Theme.getColor(Theme.key_wallet_whiteText));
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            progressView.setProgressColor(Theme.getColor(Theme.key_wallet_whiteText));
        } else if (type == TYPE_DEFAULT) {
            setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_wallet_defaultTonBlue), 25), 0x30000000));
            backgroundPaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_wallet_defaultTonBlue), 25));
            textView.setTextColor(textColor = Theme.getColor(Theme.key_wallet_defaultTonBlue));
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            progressView.setProgressColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        } else if (type == TYPE_NO_BG) {
            setBackground(Theme.getRoundRectSelectorDrawable(Theme.getColor(Theme.key_wallet_defaultTonBlue), 8));
            backgroundPaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_wallet_defaultTonBlue), 25));
            textView.setTextColor(textColor = Theme.getColor(Theme.key_wallet_defaultTonBlue));
            textView.setTypeface(defaultTypeface);
            progressView.setProgressColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        }
        collapseIcon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN));
    }

    public SimpleTextView getTextView () {
        return textView;
    }

    public void setText (String text) {
        textView.setText(text);
    }

    public void showProgress (boolean visibility, boolean animated) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (!animated) {
            progressView.setVisibility(visibility ? VISIBLE : GONE);
            progressView.setAlpha(visibility ? 1f : 0f);
        } else {
            progressView.setVisibility(VISIBLE);
            progressView.setAlpha(visibility ? 0f : 1f);
            progressAnimator = ObjectAnimator.ofFloat(progressView, View.ALPHA, visibility ? 1.0f : 0.0f);
            progressAnimator.setDuration(250L);
            progressAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd (Animator animation) {
                    super.onAnimationEnd(animation);
                    if (progressView.getAlpha() == 0f) {
                        progressView.setVisibility(GONE);
                    }
                }
            });
            progressAnimator.start();
        }
    }

    public void setCollapseIcon (@DrawableRes int iconId) {
        collapseIcon.setImageResource(iconId);
        collapseIcon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN));
    }

    public void collapse () {
        collapseIcon.setVisibility(VISIBLE);
        setCollapseFactor(0);
        setBackground(null);
        invalidate();
        ObjectAnimator.ofFloat(this, "collapseFactor", 1).setDuration(180).start();
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        boolean needCollapse = collapseRect != null;
        if (needCollapse) {
            canvas.save();
            canvas.drawRoundRect(collapseRect, collapseRadius, collapseRadius, backgroundPaint);
        }
        super.dispatchDraw(canvas);
        if (needCollapse) {
            canvas.restore();
        }
    }

    private float collapseFactor = -1;

    @Keep
    public void setCollapseFactor (float value) {
        if (collapseFactor == value) {
            return;
        }
        collapseFactor = value;
        if (collapseRect == null) {
            collapseRect = new RectF();
            collapsePath = new Path();
        }
        collapseRect.set(
            MathUtils.fromTo(0, getMeasuredWidth() / 2f - AndroidUtilities.dp(17), collapseFactor),
            MathUtils.fromTo(0, getMeasuredHeight() / 2f - AndroidUtilities.dp(17), collapseFactor),
            MathUtils.fromTo(getMeasuredWidth(), getMeasuredWidth() / 2f + AndroidUtilities.dp(17), collapseFactor),
            MathUtils.fromTo(getMeasuredHeight(), getMeasuredHeight() / 2f + AndroidUtilities.dp(17), collapseFactor)
        );
        collapseRadius = AndroidUtilities.dp(MathUtils.fromTo(8, 17, collapseFactor));
        collapsePath.reset();
        collapsePath.addRoundRect(collapseRect, collapseRadius, collapseRadius, Path.Direction.CW);
        textView.setAlpha(1f - value);
        collapseIcon.setAlpha(value);
        invalidate();
    }

    @Keep
    public float getCollapseFactor () {
        return collapseFactor;
    }
}
