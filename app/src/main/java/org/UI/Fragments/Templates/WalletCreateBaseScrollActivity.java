package org.UI.Fragments.Templates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.core.ColorUtils;

public class WalletCreateBaseScrollActivity extends WalletCreateBaseActivity {
    private BoolAnimator actionBarAnimator = new BoolAnimator(0, this::onFactorChanged, AnimatorUtils.DECELERATE_INTERPOLATOR, 220);

    protected ScrollView scrollView;
    protected LinearLayout topLayout;
    protected LinearLayout scrollViewLinearLayout;
    private View actionBarBackground;

    @Override
    public View createView (Context context) {
        onCreateFragment(context);

        FrameLayout container = new FrameLayout(context) {
            @Override
            protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                FrameLayout.LayoutParams layoutParams = (LayoutParams) actionBarBackground.getLayoutParams();
                layoutParams.topMargin = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;
                scrollViewLinearLayout.setPadding(0, ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight, 0, 0);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };

        scrollView = new ScrollView(context) {
            private final int[] location = new int[2];
            private final Rect tempRect = new Rect();
            private boolean isLayoutDirty = true;
            private int scrollingUp;

            @Override
            protected void onScrollChanged (int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);

                if (titleTextView == null) {
                    return;
                }
                titleTextView.getLocationOnScreen(location);
                boolean show = location[1] + titleTextView.getMeasuredHeight() < actionBar.getBottom();
                boolean visible = titleTextView.getTag() == null;
                if (show != visible) {
                    titleTextView.setTag(show ? null : 1);
                    actionBarAnimator.setValue(show, true);
                }
            }

            @Override
            public void scrollToDescendant (View child) {
                child.getDrawingRect(tempRect);
                offsetDescendantRectToMyCoords(child, tempRect);
                tempRect.bottom += AndroidUtilities.dp(10);
                int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(tempRect);
                if (scrollDelta < 0) {
                    scrollDelta -= (scrollingUp = (getMeasuredHeight() - child.getMeasuredHeight()) / 2);
                } else {
                    scrollingUp = 0;
                }
                if (scrollDelta != 0) {
                    smoothScrollBy(0, scrollDelta);
                }
            }

            @Override
            public void requestChildFocus (View child, View focused) {
                if (Build.VERSION.SDK_INT < 29) {
                    if (focused != null && !isLayoutDirty) {
                        scrollToDescendant(focused);
                    }
                }
                super.requestChildFocus(child, focused);
            }

            @Override
            public boolean requestChildRectangleOnScreen (View child, Rect rectangle, boolean immediate) {
                if (Build.VERSION.SDK_INT < 23) {
                    rectangle.bottom += AndroidUtilities.dp(16);
                    if (scrollingUp != 0) {
                        rectangle.top -= scrollingUp;
                        rectangle.bottom -= scrollingUp;
                        scrollingUp = 0;
                    }
                }
                return super.requestChildRectangleOnScreen(child, rectangle, immediate);
            }

            @Override
            public void requestLayout () {
                isLayoutDirty = true;
                super.requestLayout();
            }

            @Override
            protected void onLayout (boolean changed, int l, int t, int r, int b) {
                isLayoutDirty = false;
                super.onLayout(changed, l, t, r, b);
            }
        };
        scrollView.setVerticalScrollBarEnabled(false);
        container.addView(scrollView);

        scrollViewLinearLayout = new LinearLayout(context);
        scrollViewLinearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(walletFragmentView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP));

        walletFragmentView.addView(scrollViewLinearLayout);

        topLayout = new LinearLayout(context);
        topLayout.setOrientation(LinearLayout.VERTICAL);
        topLayout.addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 100, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
        topLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
        topLayout.addView(descriptionText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        LinearLayout bottomLayout = new LinearLayout(context);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.addView(buttonTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL));
        if (secondButton != null) {
            bottomLayout.addView(secondButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));
        }

        actionBarBackground = new View(context) {
            private final Paint paint = new Paint();
            @Override
            protected void onDraw(Canvas canvas) {
                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                int h = getMeasuredHeight() - AndroidUtilities.dp(3);
                canvas.drawRect(0, 0, getMeasuredWidth(), h, paint);
                parentLayout.drawHeaderShadow(canvas, h);
            }
        };
        actionBarBackground.setAlpha(0.0f);
        container.addView(actionBarBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3));
        container.addView(actionBar);

        scrollViewLinearLayout.addView(topLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, getTopLayoutOffset(), 0, 0));
        scrollViewLinearLayout.addView(bottomLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, secondButton != null ? 44 : 100));
        fragmentView = container;
        actionBar.getTitleTextView().setAlpha(0.0f);

        return fragmentView;
    }

    private void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        actionBar.getTitleTextView().setAlpha(factor);
        actionBarBackground.setAlpha(factor);
        actionBar.setBackgroundColor(ColorUtils.fromToArgb(ColorUtils.alphaColor(0, Theme.getColor(Theme.key_windowBackgroundWhite)), Theme.getColor(Theme.key_windowBackgroundWhite), factor));
    }

    protected int getTopLayoutOffset () {
        return 0; // override
    }

}
