package org.UI.Fragments.Main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.Data.Jettons.RootJettonContract;
import org.UI.Components.IconUrlView;
import org.Utils.Callbacks;
import org.UI.Utils.Drawables;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

public class WalletActivityTokenSelectorLayout extends FrameLayout implements FactorAnimator.Target {
    private final BoolAnimator appearAnimator = new BoolAnimator(0, this, AnimatorUtils.DECELERATE_INTERPOLATOR, 250L);

    private Callbacks.StringCallback tokenSelectCallback;

    public final FrameLayout wrapperView;
    public final RecyclerView recyclerGridView;
    public final Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TokenView iconUrlView;
    public final int radius = 28;

    private ArrayList<RootJettonContract> tokensList;
    private int currentTokenIndex;

    private final GridLayoutManager gridLayoutManager;
    private ActionBarPopupWindow window;

    public WalletActivityTokenSelectorLayout (@NonNull Context context) {
        super(context);

        whitePaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_wallet_whiteBackground), 48));
        Drawable backgroundDrawable = getResources().getDrawable(R.drawable.popup_fixed_alert2).mutate();
        backgroundDrawable.setColorFilter(new PorterDuffColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY));
        backgroundDrawable.setAlpha(64);
        setBackgroundColor(0xA0000000);

        iconUrlView = new TokenView(context);
        iconUrlView.setTokenIcon(null, true);

        addView(iconUrlView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));

        wrapperView = new FrameLayout(context);
        addView(wrapperView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        int spanCount = (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(32)) / AndroidUtilities.dp(72);
        gridLayoutManager = new GridLayoutManager(context, Math.min(spanCount, 5), RecyclerView.VERTICAL, false);
        recyclerGridView = new RecyclerView(context);
        recyclerGridView.setBackgroundColor(0x4000FF00);
        recyclerGridView.setLayoutManager(gridLayoutManager);
        recyclerGridView.setBackground(backgroundDrawable);
        recyclerGridView.setOverScrollMode(OVER_SCROLL_NEVER);

        wrapperView.addView(recyclerGridView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 16, 0, 16, 16));
        setAppearFactor(0);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        appearAnimator.setValue(false, true);
        return super.onTouchEvent(event);
    }

    public void setTokenSelectCallback (Callbacks.StringCallback tokenSelectCallback) {
        this.tokenSelectCallback = tokenSelectCallback;
    }

    public void setTokensList (ArrayList<RootJettonContract> tokensList, String currentRootToken) {
        int spanCount = (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(32)) / AndroidUtilities.dp(72);


        this.tokensList = tokensList;
        currentTokenIndex = -1;
        for (int a = 0; a < tokensList.size(); a++) {
            RootJettonContract token = tokensList.get(a);
            if (TextUtils.equals(token.address, currentRootToken)) {
                iconUrlView.setTokenIcon(token.content.imageUrl, false);
                currentTokenIndex = a;
                break;
            }
        }
        recyclerGridView.setAdapter(new Adapter(getContext()));
        gridLayoutManager.setSpanCount(Math.min(spanCount, Math.min(5, tokensList.size())));
    }

    public void setBalanceViewX (float x) {
        float currencyIconCenterX = x + AndroidUtilities.dp(22);
        wrapperView.setPivotX(currencyIconCenterX);
        iconUrlView.setTranslationX(currencyIconCenterX - AndroidUtilities.dp(radius + 8));
    }

    public void setBalanceViewY (float y) {
        LayoutParams layoutParams = (LayoutParams) wrapperView.getLayoutParams();
        layoutParams.topMargin = (int) y + AndroidUtilities.dp(56 + 8);
        float currencyIconCenterY = y + AndroidUtilities.dp(28);
        wrapperView.setPivotY(-AndroidUtilities.dp(8 + 28));
        iconUrlView.setTranslationY(currencyIconCenterY - AndroidUtilities.dp(radius + 8));
    }

    public void appear () {
        appearAnimator.setValue(true, true);
    }

    public void setAppearFactor (float factor) {
        iconUrlView.setScaleX(factor);
        iconUrlView.setScaleY(factor);
        wrapperView.setScaleX(factor);
        wrapperView.setScaleY(factor);
        wrapperView.setAlpha(0.5f + factor * 0.5f);
    }

    @Override
    public void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        setAppearFactor(factor);
    }

    @Override
    public void onFactorChangeFinished (int id, float finalFactor, FactorAnimator callee) {
        if (appearAnimator.getValue()) return;
        window.dismiss();
    }

    public void setWindow (ActionBarPopupWindow window) {
        this.window = window;
    }

    private static class TokenView extends FrameLayout {
        private final IconUrlView iconUrlView;
        private RLottieDrawable gemDrawable;
        private boolean needDrawGemDrawable;

        public TokenView (@NonNull Context context) {
            super(context);

            iconUrlView = new IconUrlView(context);
            iconUrlView.setRadius(AndroidUtilities.dp(28));

            addView(iconUrlView, LayoutHelper.createFrame(56, 56, Gravity.CENTER, 8, 8, 8, 8));
        }

        public void setTokenIcon (String url, boolean isGemDrawable) {
            iconUrlView.setVisibility(isGemDrawable ? INVISIBLE : VISIBLE);
            needDrawGemDrawable = isGemDrawable;
            if (isGemDrawable) {
                getGemDrawable().start();
            } else {
                iconUrlView.setUrl(url);
                if (gemDrawable != null) {
                    gemDrawable.stop();
                }
            }
            invalidate();
        }

        private RLottieDrawable getGemDrawable () {
            if (gemDrawable == null) {
                gemDrawable = new RLottieDrawable(R.raw.wallet_main, "" + R.raw.wallet_main, AndroidUtilities.dp(56), AndroidUtilities.dp(56), false);
                gemDrawable.setAutoRepeat(1);
                gemDrawable.setAllowDecodeSingleFrame(true);
                gemDrawable.addParentView(this);
                gemDrawable.setCallback(this);
            }

            return gemDrawable;
        }

        @Override
        protected void dispatchDraw (Canvas canvas) {
            super.dispatchDraw(canvas);
            if (needDrawGemDrawable) {
                Drawables.drawCentered(canvas, gemDrawable, getMeasuredWidth() / 2f, getMeasuredWidth() / 2f, null);
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;

        public Adapter (Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
            TokenView t = new TokenView(context);
            return new RecyclerListView.Holder(t);
        }

        @Override
        public void onBindViewHolder (@NonNull RecyclerView.ViewHolder holder, int position) {
            TokenView t = (TokenView) holder.itemView;
            if (currentTokenIndex != -1 && position == 0) {
                t.setTokenIcon(null, true);
                t.setOnClickListener(v -> {
                    tokenSelectCallback.run(null);
                    appearAnimator.setValue(false, true);
                });
                return;
            }
            if (position <= currentTokenIndex && currentTokenIndex != -1) {
                position -= 1;
            }
            final RootJettonContract contract = tokensList.get(position);
            t.setTokenIcon(contract.content.imageUrl, false);
            t.setOnClickListener(v -> {
                tokenSelectCallback.run(contract.address);
                appearAnimator.setValue(false, true);
            });
        }

        @Override
        public int getItemCount () {
            return tokensList.size();
        }
    }
}
