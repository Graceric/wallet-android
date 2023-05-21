package org.UI.Components.Text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.TextView;

import org.TonController.Data.TonAmount;
import org.UI.Utils.Drawables;
import org.telegram.messenger.AndroidUtilities;

public class BalanceValueTextView extends TextView {
    private float textSize;
    private int iconPadding;
    private Drawable icon;

    public BalanceValueTextView (Context context) {
        super(context);
        setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        setTextSize(18);
    }

    @Override
    public void setTextSize (float textSizeDp) {
        textSize = textSizeDp;
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        checkPaddings();
        invalidate();
    }

    public void setCurrencyDrawable (int iconRes, int paddingDp) {
        setCurrencyDrawable(iconRes != 0 ? Drawables.get(iconRes) : null, iconRes != 0 ? paddingDp : 0);
    }

    public void setCurrencyDrawable (Drawable drawable, int paddingDp) {
        if (icon != null) {
            icon.setCallback(null);
        }
        icon = drawable;
        if (icon != null) {
            icon.setCallback(this);
        }
        iconPadding = paddingDp;
        checkPaddings();
        invalidate();
    }

    private void checkPaddings () {
        setPadding(icon != null ? AndroidUtilities.dp(textSize + iconPadding) : 0, 0, 0, 0);
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        if (icon != null) {
            int x = (AndroidUtilities.dp(textSize) - icon.getMinimumWidth()) / 2;
            int y = (getMeasuredHeight() - icon.getMinimumHeight()) / 2;
            Drawables.draw(canvas, icon, x, y, null);
        }
        super.dispatchDraw(canvas);
    }

    public void setValue (TonAmount value) {
        setText(value.toSpannableString(true));
    }
}
