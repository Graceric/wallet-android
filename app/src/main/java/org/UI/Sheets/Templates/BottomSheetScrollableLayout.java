package org.UI.Sheets.Templates;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public abstract class BottomSheetScrollableLayout extends FrameLayout {
    protected final NestedScrollView scrollView;
    protected final LinearLayout linearLayout;
    protected final ActionBar actionBar;
    protected final View actionBarShadow;
    protected final View bottomViewShadow;

    protected int scrollOffsetY;
    protected boolean inLayout;

    private AnimatorSet topShadowAnimation;
    private AnimatorSet bottomShadowAnimation;

    public BottomSheetScrollableLayout (@NonNull Context context) {
        super(context);

        scrollView = new NestedScrollView(context);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipToPadding(false);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, a, b, c, d) -> updateLayout(!inLayout));

        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));

        addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, getPopupPaddingTop() / AndroidUtilities.density, 0, getPopupPaddingBottom() / AndroidUtilities.density));

        actionBar = new ActionBar(context);
        actionBar.setInterceptTouches(false);
        actionBar.setOccupyStatusBar(false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_dialogTextBlack), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_dialogButtonSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_dialogTextBlack));
        addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionBarShadow = new View(context);
        actionBarShadow.setAlpha(0.0f);
        actionBarShadow.setBackgroundColor(Theme.getColor(Theme.key_dialogShadowLine));
        addView(actionBarShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1));

        bottomViewShadow = new View(context);
        bottomViewShadow.setAlpha(0.0f);
        bottomViewShadow.setBackgroundColor(Theme.getColor(Theme.key_dialogShadowLine));
        addView(bottomViewShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 0, 0, 0, getPopupPaddingBottom() / AndroidUtilities.density));

    }

    private boolean ignoreLayout;

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        int totalHeight = MeasureSpec.getSize(heightMeasureSpec);

        ignoreLayout = true;

        updatePadding();
        int availableHeight = totalHeight - getPaddingTop();

        LayoutParams layoutParams = (LayoutParams) actionBarShadow.getLayoutParams();
        layoutParams.topMargin = ActionBar.getCurrentActionBarHeight();
        layoutParams = (LayoutParams) scrollView.getLayoutParams();
        layoutParams.topMargin = getPopupPaddingTop();
        layoutParams.bottomMargin = getPopupPaddingBottom();

        if (usePadding()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int contentSize = getPopupPaddingBottom() + getPopupPaddingTop() + getScrollPaddingBottom();
            contentSize += linearLayout.getMeasuredHeight();

            int padding = Math.max(availableHeight - contentSize, 0);
            if (useMinimalPadding()) {
                padding = Math.max(padding, availableHeight / 4);
            }
            if (scrollView.getPaddingTop() != padding) {
                scrollView.setPadding(0, padding, 0, getScrollPaddingBottom());
            }
        }

        ignoreLayout = false;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public boolean useMinimalPadding () {
        return true;
    }

    public boolean usePadding () {
        return true;
    }

    @Override
    protected void onLayout (boolean changed, int l, int t, int r, int b) {
        inLayout = true;
        super.onLayout(changed, l, t, r, b);
        inLayout = false;
        updateLayout(false);
    }

    @Override
    public void requestLayout () {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    private void updateLayout (boolean animated) {
        View child = scrollView.getChildAt(0);
        int top = child.getTop() - scrollView.getScrollY();
        int newOffset = 0;
        if (top >= 0) {
            newOffset = top;
        }
        boolean show = top < 0;
        if (show && actionBar.getTag() == null || !show && actionBar.getTag() != null) {
            actionBar.setTag(show ? 1 : null);
            onUpdateShowTopShadow(show, animated);
            if (topShadowAnimation != null) {
                topShadowAnimation.cancel();
                topShadowAnimation = null;
            }
            if (animated) {
                topShadowAnimation = new AnimatorSet();
                topShadowAnimation.setDuration(180);
                topShadowAnimation.playTogether(
                    ObjectAnimator.ofFloat(actionBarShadow, View.ALPHA, show ? 1.0f : 0.0f)
                );
                topShadowAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd (Animator animation) {
                        topShadowAnimation = null;
                    }
                });
                topShadowAnimation.start();
            } else {
                actionBarShadow.setAlpha(show ? 1.0f : 0.0f);
            }
        }
        if (scrollOffsetY != newOffset) {
            scrollOffsetY = newOffset;
            invalidate();
            actionBar.setTranslationY(scrollOffsetY);
            actionBarShadow.setTranslationY(scrollOffsetY);
        }

        show = child.getBottom() - scrollView.getScrollY() > scrollView.getMeasuredHeight();
        if (show && bottomViewShadow.getTag() == null || !show && bottomViewShadow.getTag() != null) {
            bottomViewShadow.setTag(show ? 1 : null);
            if (bottomShadowAnimation != null) {
                bottomShadowAnimation.cancel();
                bottomShadowAnimation = null;
            }
            if (animated) {
                bottomShadowAnimation = new AnimatorSet();
                bottomShadowAnimation.setDuration(180);
                bottomShadowAnimation.playTogether(ObjectAnimator.ofFloat(bottomViewShadow, View.ALPHA, show ? 1.0f : 0.0f));
                bottomShadowAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd (Animator animation) {
                        bottomShadowAnimation = null;
                    }
                });
                bottomShadowAnimation.start();
            } else {
                bottomViewShadow.setAlpha(show ? 1.0f : 0.0f);
            }
        }
    }

    protected void updatePadding () {

    }

    protected void onUpdateShowTopShadow (boolean show, boolean animated) {

    }

    public abstract int getPopupPaddingTop ();

    public abstract int getPopupPaddingBottom ();

    public int getScrollPaddingBottom () {
        return 0;
    }
}
