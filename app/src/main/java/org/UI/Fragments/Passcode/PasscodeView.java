package org.UI.Fragments.Passcode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.TonController.TonController;
import org.UI.Components.Text.PasscodeTextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CustomPhoneKeyboardView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

class PasscodeView extends FrameLayout {
    private final FrameLayout passwordFrameLayout;
    private final PasscodeTextView passwordEditText;
    private final TextView passcodeTextView;
    private final TextView retryTextView;

    private TonController tonController;

    public interface PasscodeDelegate {
        void onPasscodeEnter (String passcode);
    }

    private PasscodeDelegate passcodeDelegate;

    public PasscodeView (final Context context) {
        super(context);

        setBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackground));
        setOnTouchListener((v, event) -> true);

        passwordFrameLayout = new FrameLayout(context);
        addView(passwordFrameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 250, Gravity.TOP | Gravity.LEFT));

        RLottieImageView lottieImageView = new RLottieImageView(context);
        lottieImageView.setAutoRepeat(false);
        lottieImageView.setScaleType(ImageView.ScaleType.CENTER);
        lottieImageView.setAnimation(R.raw.wallet_password, 100, 100);
        passwordFrameLayout.addView(lottieImageView, LayoutHelper.createFrame(100, 100, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));
        lottieImageView.getDrawable().setCustomEndFrame(49);
        lottieImageView.playAnimation();

        passcodeTextView = new TextView(context);
        passcodeTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        passcodeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        passcodeTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        passcodeTextView.setText(LocaleController.formatString("WalletEnterPasscodeDigit", R.string.WalletEnterPasscodeDigit, TonController.getInstance(Utilities.selectedAccount).getPasscodeLength()));

        passwordFrameLayout.addView(passcodeTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 120, 0, 0));

        retryTextView = new TextView(context);
        retryTextView.setTextColor(Theme.getColor(Theme.key_wallet_whiteText));
        retryTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        retryTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        retryTextView.setVisibility(INVISIBLE);
        addView(retryTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        passwordEditText = new PasscodeTextView(context);
        passwordEditText.setColors(Theme.getColor(Theme.key_wallet_whiteText), Theme.getColor(Theme.key_dialogTextGray3));
        passwordFrameLayout.addView(passwordEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 40, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 168, 0, 0));

        CustomPhoneKeyboardView keyboardView = new CustomPhoneKeyboardView(context);
        keyboardView.setUseDarkTheme(true);
        keyboardView.setEditCallback(new CustomPhoneKeyboardView.EditCallback() {
            @Override
            public void onNewCharacter (String symbol) {
                passwordEditText.appendCharacter(symbol);
                if (passwordEditText.length() == TonController.getInstance(Utilities.selectedAccount).getPasscodeLength()) {
                    processDone();
                }
            }

            @Override
            public void onRemoveCharacter () {
                passwordEditText.eraseLastCharacter();
            }
        });
        addView(keyboardView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, CustomPhoneKeyboardView.KEYBOARD_HEIGHT_DP, Gravity.BOTTOM));
    }

    public void setTonController (TonController tonController) {
        this.tonController = tonController;
    }

    public void setPasscodeDelegate (PasscodeDelegate passcodeDelegate) {
        this.passcodeDelegate = passcodeDelegate;
    }

    public void setMaxCharacters (int maxCharacters, boolean animated) {
        passwordEditText.setMaxLength(maxCharacters, animated);
    }

    public void onWrongPasscode () {
        if (tonController.getPasscodeRepeatAttemptBanTime() > 0) {
            checkRetryTextView();
        }
        passwordEditText.eraseAllCharacters();
        onPasscodeError();
    }

    public void onGoodPasscode (boolean hide) {
        if (hide) {
            // swipeBackEnabled = false;
            // actionBar.setBackButtonDrawable(null);
            AnimatorSet AnimatorSet = new AnimatorSet();
            AnimatorSet.setDuration(200);
            AnimatorSet.playTogether(
                ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, AndroidUtilities.dp(20)),
                ObjectAnimator.ofFloat(this, View.ALPHA, AndroidUtilities.dp(0.0f)));
            AnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd (Animator animation) {
                    setVisibility(View.GONE);
                }
            });
            AnimatorSet.start();
            setOnTouchListener(null);
        }
    }

    private void processDone () {
        if (tonController.getPasscodeRepeatAttemptBanTime() > 0) {
            return;
        }
        String password = passwordEditText.getString();
        if (password.length() == 0) {
            onPasscodeError();
            return;
        }
        if (passcodeDelegate != null) {
            passcodeDelegate.onPasscodeEnter(password);
        }
    }

    private void shakeTextView (final float x, final int num) {
        if (num == 6) {
            return;
        }
        AnimatorSet AnimatorSet = new AnimatorSet();
        AnimatorSet.playTogether(ObjectAnimator.ofFloat(passcodeTextView, View.TRANSLATION_X, AndroidUtilities.dp(x)));
        AnimatorSet.setDuration(50);
        AnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd (Animator animation) {
                shakeTextView(num == 5 ? 0 : -x, num + 1);
            }
        });
        AnimatorSet.start();
    }

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run () {
            checkRetryTextView();
            AndroidUtilities.runOnUIThread(checkRunnable, 100);
        }
    };
    private int lastValue;

    private void checkRetryTextView () {
        long banTime = tonController.getPasscodeRepeatAttemptBanTime();
        if (banTime > 0) {
            int value = Math.max(1, (int) Math.ceil(banTime / 1000.0));
            if (value != lastValue) {
                retryTextView.setText(LocaleController.formatString("TooManyTries", R.string.TooManyTries, LocaleController.formatPluralString("Seconds", value)));
                lastValue = value;
            }
            if (retryTextView.getVisibility() != VISIBLE) {
                retryTextView.setVisibility(VISIBLE);
                passwordFrameLayout.setVisibility(INVISIBLE);
                AndroidUtilities.cancelRunOnUIThread(checkRunnable);
                AndroidUtilities.runOnUIThread(checkRunnable, 100);
            }
        } else {
            AndroidUtilities.cancelRunOnUIThread(checkRunnable);
            if (passwordFrameLayout.getVisibility() != VISIBLE) {
                retryTextView.setVisibility(INVISIBLE);
                passwordFrameLayout.setVisibility(VISIBLE);
            }
        }
    }

    public void onResume () {
        checkRetryTextView();
    }

    public void onPause () {
        AndroidUtilities.cancelRunOnUIThread(checkRunnable);
    }

    public void onShow () {
        checkRetryTextView();
        passwordEditText.eraseAllCharacters();
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int statusBarHeight = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
        int height = AndroidUtilities.displaySize.y - statusBarHeight;

        LayoutParams layoutParams;

        if (!AndroidUtilities.isTablet() && getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams = (LayoutParams) passwordFrameLayout.getLayoutParams();
            layoutParams.width = width / 2;
            layoutParams.topMargin = (height - layoutParams.height) / 2 + statusBarHeight;
        } else {
            int top = 0;
            int left = 0;
            if (AndroidUtilities.isTablet()) {
                if (width > AndroidUtilities.dp(498)) {
                    left = (width - AndroidUtilities.dp(498)) / 2;
                    width = AndroidUtilities.dp(498);
                }
                if (height > AndroidUtilities.dp(528)) {
                    top = (height - AndroidUtilities.dp(528)) / 2;
                    height = AndroidUtilities.dp(528);
                }
            } else {
                top = ActionBar.getCurrentActionBarHeight() + statusBarHeight + AndroidUtilities.dp(38);
            }
            layoutParams = (LayoutParams) passwordFrameLayout.getLayoutParams();
            layoutParams.width = width;
            layoutParams.topMargin = top;
            layoutParams.leftMargin = left;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void onPasscodeError () {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(200);
        }
        shakeTextView(2, 0);
    }
}
