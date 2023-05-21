package org.UI.Sheets.SendToncoins;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.UI.Sheets.Templates.BottomSheetPageSimple;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;

public class SendTonWaitingSendSheetPage extends BottomSheetPageSimple {
    public static final int MODE_WAITING = 0;
    public static final int MODE_FINISHED = 1;

    private final @Nullable RootJettonContract rootJettonContract;
    private final int mode;

    private TextView textView;
    private TextView addressView;

    public SendTonWaitingSendSheetPage (@Nullable RootJettonContract rootJettonContract, int mode) {
        this.rootJettonContract = rootJettonContract;
        this.mode = mode;
    }

    private Callbacks.SendInfoGetters getters;

    public void setSendInfoGetters (Callbacks.SendInfoGetters sendInfoGetters) {
        this.getters = sendInfoGetters;
    }

    @Override
    protected ViewGroup createView (Context context) {
        containerView = super.createView(context);

        actionBar.setBackButtonImage(R.drawable.baseline_close_24);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            public void onItemClick (int id) {
                dismiss();
            }
        });
        nextButton.setText(LocaleController.getString("WalletView", R.string.WalletView));
        nextButton.setOnClickListener(v -> {
            dismiss();
        });

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        RLottieImageView imageView = new RLottieImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setAutoRepeat(true);
        linearLayout.addView(imageView, LayoutHelper.createLinear(100, 100, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        TextView titleTextView = new TextView(context);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setGravity(Gravity.CENTER);
        titleTextView.setTextColor(Theme.getColor(Theme.key_wallet_headerBlackText));

        linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        textView = new TextView(context);
        textView.setMaxWidth(AndroidUtilities.dp(280));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));

        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        if (mode == MODE_WAITING) {
            imageView.setAnimation(R.raw.wallet_waiting_ton, 100, 100);
            titleTextView.setText(LocaleController.formatString(
                "WalletSendingX",
                R.string.WalletSendingX,
                rootJettonContract != null ? rootJettonContract.content.getSymbolOrName(): "TON"
            ));
            textView.setText(LocaleController.getString("WalletSendingGramsInfo", R.string.WalletSendingGramsInfo));
        } else {
            imageView.setAnimation(R.raw.wallet_success, 100, 100);
            titleTextView.setText(LocaleController.getString("WalletSendingGrams", R.string.WalletSendDone));
            addressView = new TextView(context);
            addressView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            addressView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
            addressView.setTextColor(Theme.getColor(Theme.key_wallet_blackText));
            linearLayout.addView(addressView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 24, 0, 0));
        }
        imageView.playAnimation();

        containerView.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 68));
        return containerView;
    }

    @Override
    protected void onPrepareToShow () {
        if (mode == MODE_FINISHED) {
            textView.setText(LocaleController.formatString("WalletSendXDoneText", R.string.WalletSendXDoneText,
                getters.getInputFieldSendAmountValue().toString(), rootJettonContract != null ? rootJettonContract.content.name: "Toncoin"));
            String address = getters.getInputFieldRecipientAddressString();
            StringBuilder stringBuilder = new StringBuilder(address != null ? address: "");
            stringBuilder.insert(stringBuilder.length() / 2, '\n');
            addressView.setText(stringBuilder);
        }
    }
}
