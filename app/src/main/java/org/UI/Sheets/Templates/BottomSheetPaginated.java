package org.UI.Sheets.Templates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.TonController.AccountsStateManager;
import org.TonController.TonController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Components.LayoutHelper;

import me.vkryl.core.MathUtils;

public abstract class BottomSheetPaginated extends BottomSheet {
    protected ViewPager viewPager;
    protected int currentPage = 0;
    protected float currentPageOffset = 0f;
    protected Page[] pages;
    protected boolean inLayout;
    protected int containerHeight;

    public BottomSheetPaginated (BaseFragment parent) {
        super(parent, false);
        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        Context context = parent.getParentActivity();

        pages = new Page[getPagesCount()];
        containerView = new FrameLayout(context) {
            private boolean ignoreLayout;

            @Override
            protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                ignoreLayout = true;
                setPadding(backgroundPaddingLeft, AndroidUtilities.statusBarHeight, backgroundPaddingLeft, 0);
                ignoreLayout = false;
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }

            @Override
            protected void onLayout (boolean changed, int l, int t, int r, int b) {
                inLayout = true;
                super.onLayout(changed, l, t, r, b);
                inLayout = false;
                updateLayout();
            }

            @Override
            public void requestLayout () {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            public boolean onInterceptTouchEvent (MotionEvent ev) {
                float top = Math.max(getMeasuredHeight() - containerHeight, AndroidUtilities.statusBarHeight);
                return (ev.getY() < top) || super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent (MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && canDismissWithTouchOutside()) {
                    float top = Math.max(getMeasuredHeight() - containerHeight, AndroidUtilities.statusBarHeight);
                    if (ev.getY() + backgroundPaddingTop < top) {
                        dismiss();
                        return true;
                    }
                }

                return !isDismissed() && super.onTouchEvent(ev);
            }

            @Override
            protected void dispatchDraw (Canvas canvas) {
                int top = Math.max(getMeasuredHeight() - containerHeight, AndroidUtilities.statusBarHeight);
                shadowDrawable.setBounds(0, top - backgroundPaddingTop, getMeasuredWidth(), getMeasuredHeight());
                shadowDrawable.draw(canvas);

                canvas.save();
                canvas.clipRect(backgroundPaddingLeft, top, getMeasuredWidth() - backgroundPaddingLeft, getMeasuredHeight());
                super.dispatchDraw(canvas);
                canvas.restore();
            }
        };

        viewPager = new ViewPager(context) {
            @Override
            public boolean onInterceptTouchEvent (MotionEvent ev) {
                return false;
            }

            @Override
            public boolean onTouchEvent (MotionEvent ev) {
                return false;
            }
        };
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
                currentPage = position;
                currentPageOffset = positionOffset;
                BottomSheetPaginated.this.onPageScrolled();
            }

            @Override
            public void onPageSelected (int position) {
                if (pages[position] != null) {
                    pages[position].onPrepareToShow();
                }
            }

            @Override
            public void onPageScrollStateChanged (int state) {

            }
        });
        viewPager.setAdapter(new SheetPagerAdapter(this));

        containerView.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    protected abstract int getPagesCount ();

    protected abstract Page createPageAtPosition (int position);

    protected boolean useNonPagerMode () {
        return false;
    }

    @CallSuper
    protected void onPageScrolled () {
        updateHeight();
    }

    @CallSuper
    protected void updateLayout () {
        updateHeight();
    }

    protected final void updateHeight () {
        Page p1 = pages[currentPage];
        Page p2 = (currentPage + 1 < pages.length) ? (pages[currentPage + 1]) : null;

        int height1 = (p1 != null ? p1.getHeight() : viewPager.getMeasuredHeight());
        int height2 = (p2 != null ? p2.getHeight() : viewPager.getMeasuredHeight());
        containerHeight = (int) MathUtils.fromTo(height1, height2, currentPageOffset);

        if (useNonPagerMode()) {
            if (currentPageOffset != 0f) {
                if (p1 != null) {
                    p1.containerView.setTranslationX(viewPager.getMeasuredWidth() * currentPageOffset);
                    p1.containerView.setAlpha(1f - currentPageOffset);
                }
                if (p2 != null) {
                    p2.containerView.setTranslationX(viewPager.getMeasuredWidth() * (currentPageOffset - 1f));
                    p2.containerView.setAlpha(currentPageOffset);
                }
            } else {
                for (Page page : pages) {
                    if (page == null) continue;
                    page.containerView.setTranslationX(0);
                    page.containerView.setAlpha(1);
                }
            }
        }

        containerView.invalidate();
    }

    private static class SheetPagerAdapter extends PagerAdapter {
        private final BottomSheetPaginated sheet;

        public SheetPagerAdapter (BottomSheetPaginated sheet) {
            this.sheet = sheet;
        }

        @SuppressLint("NotifyDataSetChanged")
        @NonNull
        @Override
        public Object instantiateItem (@NonNull ViewGroup container, int position) {
            if (sheet.pages[position] == null) {
                sheet.pages[position] = sheet.createPageAtPosition(position);
                sheet.pages[position].setParent(sheet);
                sheet.pages[position].create(container.getContext());
            } else {
                sheet.pages[position].onPrepareToShow();
            }
            container.addView(sheet.pages[position].containerView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            return sheet.pages[position].containerView;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount () {
            return sheet.getPagesCount();
        }

        @Override
        public boolean isViewFromObject (@NonNull View view, @NonNull Object object) {
            return view.equals(object);
        }
    }

    public static abstract class Page {
        private BottomSheetPaginated parent;
        protected ViewGroup containerView;

        private void setParent (BottomSheetPaginated parent) {
            this.parent = parent;
        }

        private void create (Context context) {
            containerView = createView(context);
        }

        protected void invalidateParent () {
            parent.containerView.invalidate();
        }

        protected BaseFragment getParentFragment () {
            return parent.getParentFragment();
        }

        protected int getParentMeasuredHeight () {
            return parent.viewPager.getMeasuredHeight();
        }

        protected abstract ViewGroup createView (Context context);

        protected void onPrepareToShow () {

        }

        protected int getHeight () {
            return getParentMeasuredHeight();
        }

        protected void showPrevPage () {
            parent.viewPager.setCurrentItem(parent.currentPage - 1);
        }

        protected void showNextPage () {
            parent.viewPager.setCurrentItem(parent.currentPage + 1);
        }

        protected void dismiss () {
            parent.dismiss();
        }

        public TonController getTonController () {
            return parent.getTonController();
        }

        public AccountsStateManager getAccountsStateManager () {
            return parent.getTonController().getAccountsStateManager();
        }
    }

    @Override
    protected boolean canDismissWithSwipe () {
        return false;
    }
}
