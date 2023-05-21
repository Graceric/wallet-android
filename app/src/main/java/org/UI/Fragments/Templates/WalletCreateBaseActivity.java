package org.UI.Fragments.Templates;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.UI.Components.Buttons.DefaultButtonView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RLottieImageView;

public class WalletCreateBaseActivity extends BaseFragment {

    protected WalletFragmentView walletFragmentView;
    protected RLottieImageView imageView;
    protected TextView titleTextView;
    protected TextView descriptionText;
    protected DefaultButtonView buttonTextView;
    protected DefaultButtonView secondButton;

    protected String[] secretWords;

    protected Runnable cancelOnDestroyRunnable;
    public BaseFragment fragmentToRemove;
    protected long showTime;

    public WalletCreateBaseActivity () {
        super();
        showTime = SystemClock.uptimeMillis();
    }

    protected void onCreateFragment (Context context) {
        swipeBackEnabled = canGoBack();
        if (swipeBackEnabled) {
            if (showBackButton()) {
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            }
        }
        actionBar.setBackgroundDrawable(null);
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText2), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                WalletCreateBaseActivity.this.onActionBarItemClick(id);
            }
        });
        if (!AndroidUtilities.isTablet()) {
            actionBar.showActionModeTop();
        }

        imageView = new RLottieImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setAutoRepeat(needImageAutoRepeat());
        imageView.setAnimation(getImageAnimation(), 100, 100);
        imageView.playAnimation();

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);

        descriptionText = new TextView(context);
        descriptionText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText2));
        descriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText.setLineSpacing(AndroidUtilities.dp(5), 1);
        descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        descriptionText.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);


        buttonTextView = new DefaultButtonView(context);
        buttonTextView.setForceWidth(200);
        buttonTextView.setOnClickListener((v) -> {
            if (getParentActivity() == null) {
                return;
            }
            this.onMainButtonClick(v);
        });

        String secondButtonText = getSecondButtonText();
        if (secondButtonText != null) {
            secondButton = new DefaultButtonView(context);
            secondButton.setType(DefaultButtonView.TYPE_NO_BG);
            secondButton.setForceWidth(200);
            secondButton.setText(secondButtonText);
            secondButton.setOnClickListener((v) -> {
                if (getParentActivity() == null) {
                    return;
                }
                this.onSecondButtonClick(v);
            });
        }

        walletFragmentView = new WalletFragmentView(context);
    }




    protected void onActionBarItemClick (int id) {
        if (id == -1) {
            finishFragment();
        }
    }

    protected boolean needImageAutoRepeat () {
        return true;
    }

    protected int getImageAnimation () {
        return 0;
    }

    protected boolean showBackButton () {
        return canGoBack();
    }

    protected boolean canGoBack () {
        return true;
    }

    protected boolean needSetFlagSecure () {
        return false;
    }

    protected String getSecondButtonText () {
        return null; // override
    }

    protected void onMainButtonClick (View v) {
        // override
    }

    protected void onSecondButtonClick (View v) {
        // override
    }

    @Override
    public void onResume () {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 23 && needSetFlagSecure() && AndroidUtilities.allowScreenCapture()) {
            AndroidUtilities.setFlagSecure(this, true);
        }
    }

    @Override
    public void onPause () {
        super.onPause();
        if (Build.VERSION.SDK_INT >= 23 && needSetFlagSecure() && AndroidUtilities.allowScreenCapture()) {
            AndroidUtilities.setFlagSecure(this, false);
        }
        if (getParentActivity() != null) {
            AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
        }
    }

    @Override
    public boolean onBackPressed () {
        return canGoBack();
    }

    @Override
    public void onFragmentDestroy () {
        if (cancelOnDestroyRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(cancelOnDestroyRunnable);
            cancelOnDestroyRunnable = null;
        }
        super.onFragmentDestroy();
    }

    public static class WalletFragmentView extends FrameLayout {
        public WalletFragmentView (@NonNull Context context) {
            super(context);
        }

        @Override
        protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int containerWidth = Math.min(AndroidUtilities.dp(360), width);

            super.onMeasure(MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        }

        @Override
        protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int p = width - getMeasuredWidth();
            super.onLayout(changed, left + p, top, right - p, bottom);
        }
    }
}
