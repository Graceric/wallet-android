package org.UI.Sheets.Templates;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.widget.NestedScrollView;

import org.telegram.ui.ActionBar.ActionBar;

public class BottomSheetPageScrollable extends BottomSheetPaginated.Page {

    protected NestedScrollView scrollView;
    protected LinearLayout linearLayout;
    protected ActionBar actionBar;
    protected View actionBarShadow;
    protected View bottomViewShadow;

    @Override
    protected ViewGroup createView (Context context) {
        BottomSheetScrollableLayout layout = new BottomSheetScrollableLayout(context) {
            @Override
            public int getScrollPaddingBottom () {
                return BottomSheetPageScrollable.this.getScrollPaddingBottom();
            }

            @Override
            public int getPopupPaddingTop () {
                return BottomSheetPageScrollable.this.getPopupPaddingTop();
            }

            @Override
            public int getPopupPaddingBottom () {
                return BottomSheetPageScrollable.this.getPopupPaddingBottom();
            }

            @Override
            protected void onUpdateShowTopShadow (boolean show, boolean animated) {
                BottomSheetPageScrollable.this.onUpdateShowTopShadow(show, animated);
            }

            @Override
            public boolean useMinimalPadding () {
                return false;
            }

            @Override
            public boolean usePadding () {
                return BottomSheetPageScrollable.this.usePadding();
            }
        };

        scrollView = layout.scrollView;
        linearLayout = layout.linearLayout;
        actionBar = layout.actionBar;
        actionBarShadow = layout.actionBarShadow;
        bottomViewShadow = layout.bottomViewShadow;

        return layout;
    }

    protected void onUpdateShowTopShadow (boolean show, boolean animated) {

    }

    public int getPopupPaddingTop () {
        return 0;
    }

    public int getPopupPaddingBottom () {
        return 0;
    }

    public int getScrollPaddingBottom () {
        return 0;
    }

    public boolean usePadding () {
        return false;
    }
}
