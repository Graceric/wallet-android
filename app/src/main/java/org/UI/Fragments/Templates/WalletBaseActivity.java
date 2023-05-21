package org.UI.Fragments.Templates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;

public class WalletBaseActivity extends BaseFragment {
    private final Paint blackPaint = new Paint();
    private final GradientDrawable backgroundDrawable;

    public WalletBaseActivity () {
        int r = AndroidUtilities.dp(12);
        backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        backgroundDrawable.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        backgroundDrawable.setColor(Theme.getColor(Theme.key_wallet_whiteBackground));
        blackPaint.setColor(Theme.getColor(Theme.key_wallet_blackBackground));
    }

    @Override
    protected ActionBar createActionBar (Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackground));
        actionBar.setTitleColor(Theme.getColor(Theme.key_wallet_whiteText));
        actionBar.setItemsColor(Theme.getColor(Theme.key_wallet_whiteText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackgroundSelector), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        return actionBar;
    }

    protected FrameLayout createFragmentView (Context context) {
        FrameLayout frameLayout = new FrameLayout(context) {
            @Override
            protected void onDraw (Canvas canvas) {
                canvas.drawRect(0, 0, getMeasuredWidth(), AndroidUtilities.dp(12), blackPaint);
                backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), AndroidUtilities.dp(12));
                backgroundDrawable.draw(canvas);
            }
        };
        frameLayout.setWillNotDraw(false);
        return frameLayout;
    }

    protected RecyclerListView createRecyclerView (Context context) {
        RecyclerListView listView = new RecyclerListView(context) {
            private final Paint paint = new Paint();

            @Override
            public void onDraw (Canvas c) {
                RecyclerView.Adapter<?> adapter = getAdapter();
                ViewHolder holder;
                if (adapter != null) {
                    holder = findViewHolderForAdapterPosition(adapter.getItemCount() - 1);
                } else {
                    holder = null;
                }
                int bottom;
                int height = getMeasuredHeight();
                if (holder != null) {
                    bottom = (int) (holder.itemView.getY() + holder.itemView.getMeasuredHeight());
                    if (holder.itemView.getBottom() >= height) {
                        bottom = height;
                    }
                } else {
                    bottom = height;
                }
                bottom += getPaddingBottom();

                paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                if (bottom < height) {
                    paint.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
                    c.drawRect(0, bottom, getMeasuredWidth(), height, paint);
                }
            }
        };

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator() {
            protected void onMoveAnimationUpdate (RecyclerView.ViewHolder holder) {
                listView.invalidate();
            }
        };
        itemAnimator.setDelayAnimations(false);
        listView.setItemAnimator(itemAnimator);

        return listView;
    }
}
