package org.UI.Cells;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Components.Buttons.DefaultButtonView;
import org.telegram.ui.Components.LayoutHelper;
import org.UI.Utils.Drawables;

public class WalletActivityButtonsCell extends FrameLayout {
    public static final int HEIGHT = 216;

    private Runnable onReceivePressed;
    private Runnable onSendPressed;

    private FrameLayout receiveButton;
    private FrameLayout sendButton;

    public WalletActivityButtonsCell (Context context) {
        super(context);

        Drawable receiveDrawable = Drawables.get(R.drawable.baseline_transaction_receive_18).mutate();
        receiveDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_wallet_buttonText), PorterDuff.Mode.MULTIPLY));
        Drawable sendDrawable = Drawables.get(R.drawable.baseline_transaction_send_18).mutate();
        sendDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_wallet_buttonText), PorterDuff.Mode.MULTIPLY));

        for (int a = 0; a < 2; a++) {
            DefaultButtonView blueButton = new DefaultButtonView(context);
            addView(blueButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM, 0, 0, 0, 16));
            blueButton.setOnClickListener(v -> {
                if (v == receiveButton && onReceivePressed != null) {
                    onReceivePressed.run();
                } else if (v == sendButton && onSendPressed != null) {
                    onSendPressed.run();
                }
            });

            blueButton.getTextView().setDrawablePadding(AndroidUtilities.dp(6));
            if (a == 0) {
                blueButton.setText(LocaleController.getString("WalletReceive", R.string.WalletReceive));
                blueButton.getTextView().setLeftDrawable(receiveDrawable);
                receiveButton = blueButton;
            } else {
                blueButton.setText(LocaleController.getString("WalletSend", R.string.WalletSend));
                blueButton.getTextView().setLeftDrawable(sendDrawable);
                sendButton = blueButton;
            }
        }
    }

    public void setListeners (Runnable onReceivePressed, Runnable onSendPressed) {
        this.onReceivePressed = onReceivePressed;
        this.onSendPressed = onSendPressed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int buttonWidth = (width - AndroidUtilities.dp(52)) / 2;

        LayoutParams layoutParams = (LayoutParams) receiveButton.getLayoutParams();
        layoutParams.width = buttonWidth;
        layoutParams.leftMargin = AndroidUtilities.dp(20);

        layoutParams = (LayoutParams) sendButton.getLayoutParams();
        layoutParams.width = buttonWidth;
        layoutParams.leftMargin = AndroidUtilities.dp(32) + buttonWidth;

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(HEIGHT), MeasureSpec.EXACTLY));
    }
}
