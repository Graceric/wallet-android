package org.UI.Sheets.Templates;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;

public abstract class BottomSheetScrollable extends BottomSheet {

    protected final NestedScrollView scrollView;
    protected final LinearLayout linearLayout;
    protected final ActionBar actionBar;
    protected final View actionBarShadow;
    protected final View bottomViewShadow;

    public BottomSheetScrollable (BaseFragment parent) {
        super(parent, false);
        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        Context context = parent.getParentActivity();

        BottomSheetScrollableLayout layout = new BottomSheetScrollableLayout(context) {
            @Override
            public int getScrollPaddingBottom () {
                return BottomSheetScrollable.this.getScrollPaddingBottom();
            }

            @Override
            public int getPopupPaddingTop () {
                return BottomSheetScrollable.this.getPopupPaddingTop();
            }

            @Override
            public int getPopupPaddingBottom () {
                return BottomSheetScrollable.this.getPopupPaddingBottom();
            }

            @Override
            protected void onUpdateShowTopShadow (boolean show, boolean animated) {
                BottomSheetScrollable.this.onUpdateShowTopShadow(show, animated);
            }

            @Override
            protected void updatePadding () {
                setPadding(backgroundPaddingLeft, AndroidUtilities.statusBarHeight, backgroundPaddingLeft, 0);
            }

            @Override
            public boolean onInterceptTouchEvent (MotionEvent ev) {
                float top = scrollOffsetY - backgroundPaddingTop + AndroidUtilities.statusBarHeight;
                return (ev.getY() < top) || super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent (MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && canDismissWithTouchOutside()) {
                    float top = scrollOffsetY - backgroundPaddingTop + AndroidUtilities.statusBarHeight;
                    if (ev.getY() < top) {
                        dismiss();
                        return true;
                    }
                }

                return super.onTouchEvent(ev);
            }

            @Override
            protected void dispatchDraw (Canvas canvas) {
                int top = scrollOffsetY - backgroundPaddingTop;
                if (Build.VERSION.SDK_INT >= 21) {
                    top += AndroidUtilities.statusBarHeight;
                }

                shadowDrawable.setBounds(0, top, getMeasuredWidth(), getMeasuredHeight());
                shadowDrawable.draw(canvas);
                super.dispatchDraw(canvas);
            }
        };

        scrollView = layout.scrollView;
        linearLayout = layout.linearLayout;
        actionBar = layout.actionBar;
        actionBarShadow = layout.actionBarShadow;
        bottomViewShadow = layout.bottomViewShadow;

        layout.setPadding(getBackgroundPaddingLeft(), 0, getBackgroundPaddingLeft(), 0);
        containerView = layout;
    }

    protected void onUpdateShowTopShadow (boolean show, boolean animated) {

    }

    public abstract int getPopupPaddingTop ();

    public abstract int getPopupPaddingBottom ();

    public int getScrollPaddingBottom () {
        return 0;
    }

    @Override
    protected boolean canDismissWithSwipe () {
        return false;
    }
}
