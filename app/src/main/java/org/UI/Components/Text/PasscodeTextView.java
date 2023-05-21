package org.UI.Components.Text;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.Keep;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.Theme;

public class PasscodeTextView extends View {
    private final Paint dotFillingPaint;
    private final Paint dotStrokePaint;

    private final StringBuilder stringBuilder;
    private int maxLength;

    private ObjectAnimator targetLengthAnimator;
    private ObjectAnimator lengthAnimator;
    private float targetLengthValue;
    private float lengthValue;

    public PasscodeTextView (Context context) {
        this(context, 4);
    }

    public PasscodeTextView (Context context, int max) {
        super(context);
        setMaxLength(max, false);
        stringBuilder = new StringBuilder(4);

        dotFillingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(AndroidUtilities.dp(1));

        setColors(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
    }

    public void setMaxLength (int maxLength, boolean animated) {
        if (this.maxLength == maxLength) return;
        if (targetLengthAnimator != null) {
            targetLengthAnimator.cancel();
        }

        if (animated) {
            targetLengthAnimator = ObjectAnimator.ofFloat(this, "targetLengthValue", maxLength);
            targetLengthAnimator.setDuration(180L);
            targetLengthAnimator.start();
        } else {
            targetLengthValue = maxLength;
        }

        this.maxLength = maxLength;
        invalidate();
    }

    public void setColors (int fillingColor, int strokeColor) {
        dotFillingPaint.setColor(fillingColor);
        dotStrokePaint.setColor(strokeColor);
    }

    private void checkLength () {
        if (lengthAnimator != null) {
            lengthAnimator.cancel();
        }
        lengthAnimator = ObjectAnimator.ofFloat(this, "lengthValue", length());
        lengthAnimator.setDuration(180L);
        lengthAnimator.start();
    }

    public void appendCharacter (String c) {
        if (stringBuilder.length() == maxLength) {
            return;
        }
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception e) {
            FileLog.e(e);
        }
        stringBuilder.append(c);
        checkLength();
    }

    public String getString () {
        return stringBuilder.toString();
    }

    public int length () {
        return stringBuilder.length();
    }

    public void eraseLastCharacter () {
        if (stringBuilder.length() == 0) {
            return;
        }
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception e) {
            FileLog.e(e);
        }

        int deletingPos = stringBuilder.length() - 1;
        if (deletingPos != 0) {
            stringBuilder.deleteCharAt(deletingPos);
        }

        if (deletingPos == 0) {
            stringBuilder.deleteCharAt(deletingPos);
        }
        checkLength();
    }

    public void eraseAllCharacters () {
        if (stringBuilder.length() == 0) {
            return;
        }
        stringBuilder.delete(0, stringBuilder.length());
        checkLength();
    }

    @Override
    protected void onDraw (Canvas canvas) {
        int rad = AndroidUtilities.dp(8);
        int d = AndroidUtilities.dp(11);
        int width = (int) (d * (targetLengthValue - 1) + rad * 2 * targetLengthValue);
        int x = (getMeasuredWidth() - width) / 2;
        int y = (getMeasuredHeight()) / 2;

        for (int a = 0; a < Math.ceil(targetLengthValue); a++) {
            float progressShow = Math.min(Math.max(targetLengthValue - a, 0), 1);
            float radA = AndroidUtilities.dp(8) * progressShow;

            float progress = Math.min(Math.max(lengthValue - a, 0), 1);
            float radius = radA * progress;
            dotStrokePaint.setAlpha((int) (255 * (1f - progress)));
            canvas.drawCircle(x + rad, y, radA, dotStrokePaint);
            canvas.drawCircle(x + rad, y, radius, dotFillingPaint);
            x += rad * 2 + d;
        }
    }

    @Keep
    public void setTargetLengthValue (float lengthValue) {
        this.targetLengthValue = lengthValue;
        invalidate();
    }

    @Keep
    public float getTargetLengthValue () {
        return targetLengthValue;
    }

    @Keep
    public void setLengthValue (float lengthValue) {
        this.lengthValue = lengthValue;
        invalidate();
    }

    @Keep
    public float getLengthValue () {
        return lengthValue;
    }
}
