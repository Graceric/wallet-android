package org.UI.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.TonController.Data.TonAmount;
import org.UI.Components.IconUrlView;
import org.UI.Utils.BalanceCounter;
import org.UI.Utils.Drawables;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.AtomicAnimator;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.ColorAnimator;
import me.vkryl.android.animator.FactorAnimator;

public class AmountEditTextCell extends FrameLayout {
    private final BoolAnimator insufficientFundsVisible = new BoolAnimator(0, this::onFactorChanged, AnimatorUtils.DECELERATE_INTERPOLATOR, 180L);
    private final BalanceCounter balanceCounter = new BalanceCounter(this::onCounterAppearanceChanged);
    private final ColorAnimator colorAnimator = new ColorAnimator(this::onAtomicValueUpdate, AnimatorUtils.DECELERATE_INTERPOLATOR, 180L, () -> Theme.getColor(Theme.key_wallet_blackText));
    private final ColorAnimator.FutureColor defaultColor = () -> Theme.getColor(Theme.key_wallet_blackText);
    private final ColorAnimator.FutureColor hintColor = () -> Theme.getColor(Theme.key_windowBackgroundWhiteHintText);
    private final ColorAnimator.FutureColor errorColor = () -> Theme.getColor(Theme.key_wallet_errorAmountInputTextColor);
    private final IconUrlView iconUrlView;
    private final RLottieDrawable gemDrawable;
    private final EditTextBoldCursor textView;
    private final TextView textView2;

    private TonAmount inputAmount;
    private int decimals = 9;
    
    private boolean needDrawGem = true;
    private OnUpdateAmountValue delegate;

    public interface OnUpdateAmountValue {
        void onUpdate (BigInteger i, String value);
        TonAmount getCurrentWalletBalance ();
    }

    public AmountEditTextCell (@NonNull Context context) {
        super(context);
        setDecimals(9);

        textView = new EditTextBoldCursor(context) {
            @Override
            public boolean onTouchEvent (MotionEvent event) {
                return false;
            }
        };
        textView.setVisibility(INVISIBLE);
        textView.addTextChangedListener(new TextWatcher() {
            private boolean ignoreTextChange;
            private boolean adding;

            @Override
            public void beforeTextChanged (CharSequence s, int start, int count, int after) {
                adding = count == 0 && after == 1;
            }

            @Override
            public void onTextChanged (CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged (Editable s) {
                if (ignoreTextChange || textView.getTag() != null) {
                    return;
                }
                ignoreTextChange = true;
                int dotsCount = 0;
                for (int a = 0; a < s.length(); a++) {
                    char c = s.charAt(a);
                    if (c == ',' || c == '#' || c == '*') {
                        s.replace(a, a + 1, ".");
                        c = '.';
                    }
                    if (c == '.' && dotsCount == 0) {
                        dotsCount++;
                    } else if (c < '0' || c > '9') {
                        s.delete(a, a + 1);
                        a--;
                    }
                }
                if (s.length() > 0 && s.charAt(0) == '.') {
                    s.insert(0, "0");
                }
                if (adding && s.length() == 1 && s.charAt(0) == '0') {
                    s.replace(0, s.length(), "0.");
                }
                int index = TextUtils.indexOf(s, '.');
                if (index >= 0) {
                    if (s.length() - index > (decimals + 1)) {
                        s.delete(index + (decimals + 1), s.length());
                    }
                    if (index > 9) {
                        int countToDelete = index - 9;
                        s.delete(9, 9 + countToDelete);
                        index -= countToDelete;
                    }
                    String start = s.subSequence(0, index).toString();
                    String end = s.subSequence(index + 1, s.length()).toString();
                    String fixedEnd = end.length() != decimals ? end + Utils.repeatString("0", decimals - end.length()): end;

                    inputAmount.setBalance(start + fixedEnd);
                } else {
                    if (s.length() > 9) {
                        s.delete(9, s.length());
                    }
                    inputAmount.setBalance(s + Utils.repeatString("0", decimals));
                }

                ignoreTextChange = false;

                BigInteger balance = inputAmount.getBalanceSafe();
                boolean isError = balance.compareTo(delegate.getCurrentWalletBalance().getBalanceSafe()) > 0;
                String textToSet = TextUtils.isEmpty(s) ? "0" : s.toString();
                balanceCounter.setBalance(textToSet, true);
                colorAnimator.setValue(isError ? errorColor : (TextUtils.isEmpty(s) ? hintColor : defaultColor), true);
                insufficientFundsVisible.setValue(isError, true);
                if (delegate != null && inputAmount.getBalance() != null) {
                    delegate.onUpdate(inputAmount.getBalance(), s.toString());
                }
            }
        });
        textView.setFocusable(false);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 0, 0, 0, 0));

        textView2 = new TextView(getContext());
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView2.setGravity(Gravity.CENTER);
        textView2.setTranslationY(AndroidUtilities.dp(35));
        textView2.setTextColor(Theme.getColor(Theme.key_wallet_errorAmountInputTextColor));
        textView2.setText(LocaleController.getString("WalletSendTransactionInsufficientFunds", R.string.WalletSendTransactionInsufficientFunds));
        textView2.setAlpha(0);
        addView(textView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        gemDrawable = new RLottieDrawable(R.raw.wallet_main, "" + R.raw.wallet_main, AndroidUtilities.dp(44), AndroidUtilities.dp(44), false);
        gemDrawable.setAutoRepeat(1);
        gemDrawable.setAllowDecodeSingleFrame(true);
        gemDrawable.addParentView(textView);
        gemDrawable.setCallback(textView);
        gemDrawable.start();

        iconUrlView = new IconUrlView(context);
        iconUrlView.setRadius(AndroidUtilities.dp(22));
        iconUrlView.setVisibility(GONE);
        addView(iconUrlView, LayoutHelper.createFrame(44, 44, Gravity.CENTER_VERTICAL));

        insufficientFundsVisible.setValue(false, false);
        balanceCounter.setBalance("0", true);
        colorAnimator.setValue(hintColor, true);
    }

    public void setDecimals (int decimals) {
        inputAmount = new TonAmount(decimals);
        this.decimals = decimals;
    }

    public void setCurrencyUrl (String url) {
        iconUrlView.setUrl(url);
        iconUrlView.setVisibility(VISIBLE);
        needDrawGem = false;
    }

    public EditTextBoldCursor getTextView () {
        return textView;
    }

    public void setText (CharSequence text) {
        textView.setText(text);
        if (!TextUtils.isEmpty(text)) {
            textView.setSelection(textView.length());
        }
    }

    private void onCounterAppearanceChanged (BalanceCounter counter, boolean sizeChanged) {
        invalidate();
    }

    private void onAtomicValueUpdate (AtomicAnimator<ColorAnimator.FutureColor> animator, float newValue) {
        invalidate();
    }

    private void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        textView2.setAlpha(insufficientFundsVisible.getFloatValue());
        invalidate();
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        super.dispatchDraw(canvas);
        int counterStartX = (getMeasuredWidth() - balanceCounter.getWidth() + AndroidUtilities.dp(48)) / 2;
        balanceCounter.draw(canvas, counterStartX, getMeasuredHeight() / 2, 1f);
        balanceCounter.setTextColor(colorAnimator.getIntValue());
        if (needDrawGem) {
            Drawables.draw(canvas, gemDrawable, counterStartX - AndroidUtilities.dp(48), getMeasuredHeight() / 2f - AndroidUtilities.dp(22), null);
        } else {
            iconUrlView.setTranslationX(counterStartX - AndroidUtilities.dp(48));
        }
    }

    public void setDelegate (OnUpdateAmountValue delegate) {
        this.delegate = delegate;
    }
}
