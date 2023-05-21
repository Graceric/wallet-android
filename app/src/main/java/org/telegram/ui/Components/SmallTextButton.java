package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class SmallTextButton extends FrameLayout {
    private final TextView textView;
    private Drawable drawable;


    public SmallTextButton(@NonNull Context context) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), 0xFFFFFFFF, 0xFFBBBBBB));
        setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
    }

    public void setTypeface (Typeface typeface) {
        textView.setTypeface(typeface);
    }

    public void setText (String text) {
        textView.setText(text);
    }

    public void setDrawable (int resId) {
        drawable = getContext().getResources().getDrawable(resId).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_wallet_defaultTonBlue), PorterDuff.Mode.MULTIPLY));
        setPadding(AndroidUtilities.dp(32), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (drawable != null) {
            drawable.setBounds(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(28));
            drawable.draw(canvas);
        }
    }
}
