package org.UI.Utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.CounterAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.android.animator.ListAnimator;
import me.vkryl.core.MathUtils;

public class BalanceCounter implements FactorAnimator.Target, CounterAnimator.Callback<BalanceCounter.Text> {
    private static final int ANIMATOR_COUNTER_IS_VISIBLE = 0;
    private static final int ANIMATOR_ALL_SYMBOLS_COUNT = 1;
    private static final int ANIMATOR_WHOLE_PART_SYMBOLS_COUNT = 2;

    private final FactorAnimator allSymbolsCount = new FactorAnimator(ANIMATOR_ALL_SYMBOLS_COUNT, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final FactorAnimator wholePartSymbolsCount = new FactorAnimator(ANIMATOR_WHOLE_PART_SYMBOLS_COUNT, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 220L);
    private final CounterAnimator<Text> counter = new CounterAnimator<>(this);
    private final BoolAnimator isVisible = new BoolAnimator(ANIMATOR_COUNTER_IS_VISIBLE, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 200L);

    private final Callback callback;
    private final TextPaint textPaint;
    private float textSizeDp;
    private boolean needScale;
    private float lastWidth;
    private long fakeCounter = 1;

    public BalanceCounter (Callback callback) {
        this.callback = callback;
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        setParams(44, true, Typefaces.GOOGLE_SANS_MEDIUM);
    }

    public void setParams (float textSizeDp, boolean needScale, Typeface typeface) {
        this.textPaint.setTypeface(typeface);
        this.textPaint.setTextSize(AndroidUtilities.dp(textSizeDp));
        this.textSizeDp = textSizeDp;
        this.needScale = needScale;
    }

    public void setTextColor (int color) {
        textPaint.setColor(color);
        invalidate(false);
    }

    public void invalidate (boolean sizeChanged) {
        if (callback != null)
            callback.onCounterAppearanceChanged(this, sizeChanged);
    }

    public void setBalance (@NonNull String balanceRepr, boolean animated) {
        boolean setIsVisible = !TextUtils.isEmpty(balanceRepr);
        isVisible.setValue(setIsVisible, animated);

        if (setIsVisible) {
            int textLength = balanceRepr.length();
            int index = TextUtils.indexOf(balanceRepr, '.');
            allSymbolsCount.animateTo(textLength);
            int wholePartSize = index >= 0 ? index + 1: textLength;
            wholePartSymbolsCount.animateTo(wholePartSize);
            counter.setCounter(fakeCounter++, balanceRepr, animated);
        } else {
            counter.hideCounter(animated);
            allSymbolsCount.animateTo(0);
            wholePartSymbolsCount.animateTo(0);
        }
    }

    public void draw (Canvas c, int startX, int centerY, float alpha) {
        final float allSymbols = allSymbolsCount.getFactor();
        final float wholePart = wholePartSymbolsCount.getFactor();

        float offset = 0;

        for (ListAnimator.Entry<CounterAnimator.Part<Text>> entry : counter) {
            float scale = Math.min(MathUtils.clamp((allSymbols - entry.getPosition())),
                (needScale ? MathUtils.clamp((wholePart - entry.getPosition()), 0.78f, 1): 1f));
            float vertPosition = entry.item.getVerticalPosition() * MathUtils.clamp((allSymbols - entry.getPosition())) * 0.33f;

            float width = entry.item.text.getFloatWidth() * scale;
            int height = (int) (entry.item.getHeight() * scale);
            int textStartX = Math.round(startX + entry.getRectF().left + offset);

            offset -= (entry.item.text.getFloatWidth() - width) * entry.getVisibility();

            int startY = Math.round(centerY - entry.item.getHeight() / 2f + entry.item.getHeight() * .8f * vertPosition) + entry.item.getHeight() - height;
            entry.item.text.draw(c, textStartX, startY + height - AndroidUtilities.dp(6.5f / 44f * textSizeDp), alpha * entry.getVisibility() * (1f - Math.abs(entry.item.getVerticalPosition())), scale);
        }
    }

    public int getWidth () {
        final float allSymbols = allSymbolsCount.getFactor();
        final float wholePart = wholePartSymbolsCount.getFactor();

        float offset = 0;
        for (ListAnimator.Entry<CounterAnimator.Part<Text>> entry : counter) {
            float scale = (float) Math.min(MathUtils.clamp((allSymbols - entry.getPosition())),
                MathUtils.clamp((wholePart - entry.getPosition()), 0.73, 1));
            float width = (int) (entry.item.text.getFloatWidth() * scale);
            offset -= (entry.item.text.getFloatWidth() - width) * entry.getVisibility();
        }

        return (int) (counter.getWidth() + offset);
    }

    @Override
    public void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        invalidate(id == ANIMATOR_COUNTER_IS_VISIBLE);
    }

    @Override
    public void onItemsChanged (CounterAnimator<?> animator) {
        float width = getWidth();
        boolean changed = lastWidth != width;
        lastWidth = width;
        invalidate(changed);
    }

    @Override
    public Text onCreateTextDrawable (String text) {
        return new Text(text, textPaint);
    }

    public interface Callback {
        void onCounterAppearanceChanged (BalanceCounter counter, boolean sizeChanged);
    }

    public static class Text implements CounterAnimator.TextDrawable {
        private final String text;
        private final float width;
        private final TextPaint textPaint;

        public Text (String text, TextPaint textPaint) {
            this.text = text;
            this.textPaint = textPaint;
            this.width = textPaint.measureText("x" + text + "x") - textPaint.measureText("xx");
        }

        @Override
        public int getWidth () {
            return (int) getFloatWidth();
        }

        @Override
        public int getHeight () {
            return (int) textPaint.getTextSize();
        }

        public float getFloatWidth () {
            return width;
        }

        @Override
        public String getText () {
            return text;
        }

        public void draw (Canvas c, int startX, int endY, @FloatRange(from = 0f, to = 1f) float alpha, @FloatRange(from = 0f, to = 1f) float textScale) {

            textPaint.setAlpha((int) (255 * alpha));
            c.save();
            c.scale(textScale, textScale, startX, endY);
            c.drawText(text, startX, endY, textPaint);
            c.restore();
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        @Override
        public boolean equals (@Nullable Object obj) {
            return obj instanceof Text && ((Text) obj).text.equals(this.text);
        }
    }
}
