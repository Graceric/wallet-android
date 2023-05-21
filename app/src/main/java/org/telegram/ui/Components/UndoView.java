package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class UndoView extends FrameLayout {
    public final static int ACTION_WRONG_DOMAIN = 0;
    public final static int ACTION_WRONG_ADDRESS = 1;
    public final static int ACTION_JETTON_PARSE_ERROR = 2;
    public final static int ACTION_INSUFFICIENT_FUNDS = 3;

    private final TextView infoTextView;
    private final TextView subinfoTextView;
    private final ImageView leftImageView;

    private final int undoViewHeight = AndroidUtilities.dp(56);
    private final int enterOffsetMargin = AndroidUtilities.dp(8);

    private int currentAction = -1;
    private Runnable currentActionRunnable;
    private Runnable currentCancelRunnable;
    private Runnable hideRunnable;

    private boolean isShown;

    Drawable backgroundDrawable;

    public UndoView(Context context) {
        super(context);

        infoTextView = new TextView(context);
        infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        infoTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        infoTextView.setLinkTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        infoTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        infoTextView.setMaxLines(1);
        addView(infoTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 52, 10, 0, 0));

        subinfoTextView = new TextView(context);
        subinfoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subinfoTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        subinfoTextView.setLinkTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        subinfoTextView.setHighlightColor(0);
        subinfoTextView.setSingleLine(true);
        subinfoTextView.setEllipsize(TextUtils.TruncateAt.END);
        subinfoTextView.setMaxLines(1);
        addView(subinfoTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 52, 28, 8, 0));

        leftImageView = new ImageView(context);
        leftImageView.setScaleType(ImageView.ScaleType.CENTER);
        addView(leftImageView, LayoutHelper.createFrame(32, 32, Gravity.TOP | Gravity.LEFT, 10, 12, 0, 0));

        setWillNotDraw(false);
        backgroundDrawable = Theme.createRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_tooltip_backgroundColor));

        setOnClickListener(view -> hide(false, true));

        setVisibility(INVISIBLE);
    }

    public void hide(boolean apply, boolean animated) {
        if (getVisibility() != VISIBLE || !isShown) {
            return;
        }
        isShown = false;
        hideRunnable = null;
        if (currentActionRunnable != null) {
            if (apply) {
                currentActionRunnable.run();
            }
            currentActionRunnable = null;
        }
        if (currentCancelRunnable != null) {
            if (!apply) {
                currentCancelRunnable.run();
            }
            currentCancelRunnable = null;
        }
        if (animated) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(ObjectAnimator.ofFloat(this, "enterOffset", 1.0f * (enterOffsetMargin + undoViewHeight)));
            animatorSet.setDuration(250);
            animatorSet.setInterpolator(new DecelerateInterpolator());
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(INVISIBLE);
                    setScaleX(1.0f);
                    setScaleY(1.0f);
                    setAlpha(1.0f);
                }
            });
            animatorSet.start();
        } else {
            setEnterOffset((1.0f) * (enterOffsetMargin + undoViewHeight));
            setVisibility(INVISIBLE);
        }
    }

    public void showWithAction(int action) {
        showWithAction(action, null);
    }

    public void showWithAction(int action, Runnable actionRunnable) {
        showWithAction(action, actionRunnable, null);
    }

    public void showWithAction(int action, Runnable actionRunnable, Runnable cancelRunnable) {
        if (currentActionRunnable != null) {
            currentActionRunnable.run();
        }
        isShown = true;

        currentActionRunnable = actionRunnable;
        currentCancelRunnable = cancelRunnable;
        currentAction = action;

        if (hideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(hideRunnable);
        }
        hideRunnable = () -> hide(false, true);

        if (currentAction == ACTION_WRONG_ADDRESS) {
            infoTextView.setText(LocaleController.getString("WalletInvalidAddress", R.string.WalletInvalidAddress));
            subinfoTextView.setText(LocaleController.getString("WalletInvalidAddressInfo", R.string.WalletInvalidAddressInfo));
            leftImageView.setImageResource(R.drawable.baseline_warning_32);
        } else if (currentAction == ACTION_WRONG_DOMAIN) {
            infoTextView.setText(LocaleController.getString("WalletInvalidDomain", R.string.WalletInvalidDomain));
            subinfoTextView.setText(LocaleController.getString("WalletInvalidDomainInfo", R.string.WalletInvalidDomainInfo));
            leftImageView.setImageResource(R.drawable.baseline_warning_32);
        } else if (currentAction == ACTION_JETTON_PARSE_ERROR) {
            infoTextView.setText("Error");
            subinfoTextView.setText("Failed to parse token info");
            leftImageView.setImageResource(R.drawable.baseline_warning_32);
        } else if (currentAction == ACTION_INSUFFICIENT_FUNDS) {
            infoTextView.setText("Transaction rejected");
            subinfoTextView.setText("Insufficient funds");
            leftImageView.setImageResource(R.drawable.baseline_warning_32);
        }

        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
            setEnterOffset((1.0f) * (enterOffsetMargin + undoViewHeight));
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(ObjectAnimator.ofFloat(this, "enterOffset", enterOffsetMargin + undoViewHeight, -1.0f));
            animatorSet.setInterpolator(new DecelerateInterpolator());
            animatorSet.setDuration(180);
            animatorSet.start();
        }

        AndroidUtilities.runOnUIThread(hideRunnable, 5000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(undoViewHeight, MeasureSpec.EXACTLY));
        backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        backgroundDrawable.draw(canvas);
    }

    private float enterOffset;

    @Keep
    public float getEnterOffset() {
        return enterOffset;
    }

    @Keep
    public void setEnterOffset(float enterOffset) {
        if (this.enterOffset != enterOffset) {
            this.enterOffset = enterOffset;
            updatePosition();
        }
    }

    private void updatePosition() {
        setTranslationY(enterOffset - enterOffsetMargin + AndroidUtilities.dp(8));
        invalidate();
    }

    @Override
    public Drawable getBackground() {
        return backgroundDrawable;
    }
}
