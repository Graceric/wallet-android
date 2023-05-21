package org.UI.Components.Buttons;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.LayoutHelper;

public class DefaultButtonsPairView extends LinearLayout {
    public final DefaultButtonView confirmButton;
    public final DefaultButtonView cancelButton;

    public DefaultButtonsPairView (Context context) {
        super(context);
        setOrientation(HORIZONTAL);

        confirmButton = new DefaultButtonView(context);
        cancelButton = new DefaultButtonView(context);

        addView(cancelButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 0, 0, 4, 0));
        addView(confirmButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 4, 0, 0, 0));
    }

    public void collapseButtons (boolean isConfirm) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (isConfirm) {
            animatorSet.playTogether(
                ObjectAnimator.ofFloat(cancelButton, View.ALPHA, 1.0f, 0.0f),
                ObjectAnimator.ofFloat(cancelButton, View.TRANSLATION_X, 0.0f, AndroidUtilities.dp(56)),
                ObjectAnimator.ofFloat(confirmButton, View.TRANSLATION_X, 0.0f, -confirmButton.getMeasuredWidth() / 2f - AndroidUtilities.dp(4))
            );
        } else {
            animatorSet.playTogether(
                ObjectAnimator.ofFloat(confirmButton, View.ALPHA, 1.0f, 0.0f),
                ObjectAnimator.ofFloat(confirmButton, View.TRANSLATION_X, 0.0f, -AndroidUtilities.dp(56)),
                ObjectAnimator.ofFloat(cancelButton, View.TRANSLATION_X, 0.0f, confirmButton.getMeasuredWidth() / 2f + AndroidUtilities.dp(4))
            );
        }
        animatorSet.setDuration(180L);
        animatorSet.start();
        confirmButton.setEnabled(false);
        cancelButton.setEnabled(false);
        cancelButton.collapse();
        confirmButton.collapse();
    }

    public void setProgress (boolean isConfirm, boolean progress, boolean animated) {
        (isConfirm ? confirmButton : cancelButton).showProgress(progress, animated);
        confirmButton.setEnabled(!progress);
        cancelButton.setEnabled(!progress);
    }
}
