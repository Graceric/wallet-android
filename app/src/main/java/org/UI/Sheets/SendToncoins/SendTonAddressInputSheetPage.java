package org.UI.Sheets.SendToncoins;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Storage.RecentRecipientsStorage;
import org.UI.Cells.RecentRecipientCell;
import org.UI.Components.Buttons.DefaultButtonView;
import org.UI.Sheets.Templates.BottomSheetPageScrollable;
import org.UI.Utils.Typefaces;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.UI.Cells.AddressEditTextCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SmallTextButton;
import org.telegram.ui.Components.UndoView;
import org.Utils.Callbacks;
import org.TonController.Parsers.UriParser;

import java.util.ArrayList;

public class SendTonAddressInputSheetPage extends BottomSheetPageScrollable {
    private final @Nullable RootJettonContract rootJettonContract;
    private ArrayList<RecentRecipientsStorage.RecentRecipient> recentRecipients;

    public SendTonAddressInputSheetPage (@Nullable RootJettonContract rootJettonContract) {
        this.rootJettonContract = rootJettonContract;
    }

    private AddressEditTextCell addressEditTextCell;
    private DefaultButtonView nextButton;
    private UndoView incorrectDomainWarningView;

    private String inputFieldRecipientString = "";
    private Delegate delegate;
    private Callbacks.SendInfoGetters getters;

    public interface Delegate {
        void onAddressInputSuccess (String address, String domain);
        void onParseQrCode (UriParser.Result result);
    }

    public void setSendInfoGetters (Callbacks.SendInfoGetters getters, Delegate delegate) {
        this.getters = getters;
        this.delegate = delegate;
    }

    @Override
    protected ViewGroup createView (Context context) {
        recentRecipients = getTonController().getAccountsStateManager().getRecentRecipientsStorage().getRecipients();

        ViewGroup viewGroup = super.createView(context);

        actionBar.setTitle(LocaleController.formatString("WalletSendX", R.string.WalletSendX,
            rootJettonContract != null ? rootJettonContract.content.getSymbolOrName(): "TON"));

        HeaderCell headerCell = new HeaderCell(context, 20, 0, false);
        headerCell.setText(LocaleController.getString("WalletSendRecipient", R.string.WalletSendRecipient));
        linearLayout.addView(headerCell);

        addressEditTextCell = new AddressEditTextCell(context);
        addressEditTextCell.setPasteDelegate(r -> delegate.onParseQrCode(r));
        addressEditTextCell.setEditDelegate(s -> inputFieldRecipientString = s);
        // addressEditTextCell.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        updateEditField();
        linearLayout.addView(addressEditTextCell);

        TextInfoPrivacyCell textInfoPrivacyCell = new TextInfoPrivacyCell(context, 20, 12, 4);
        textInfoPrivacyCell.setText(LocaleController.getString("WalletSendPasteDescription", R.string.WalletSendPasteDescription));
        linearLayout.addView(textInfoPrivacyCell);

        InputHelpersButtons cell = new InputHelpersButtons(context);
        cell.setDelegate(inputHelpersOnClickListener);
        linearLayout.addView(cell);

        nextButton = new DefaultButtonView(context);
        nextButton.setText(LocaleController.getString("WalletContinue", R.string.WalletContinue));
        nextButton.setOnClickListener(this::onAddressInputContinueButtonClick);
        viewGroup.addView(nextButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT, 16, 0, 16, 16));

        incorrectDomainWarningView = new UndoView(context);
        viewGroup.addView(incorrectDomainWarningView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        if (!recentRecipients.isEmpty()) {
            HeaderCell recipientsHeader = new HeaderCell(context, 20, 24, false);
            recipientsHeader.setText("Recent");
            linearLayout.addView(recipientsHeader);
            for (int a = 0; a < recentRecipients.size(); a++) {
                RecentRecipientsStorage.RecentRecipient recipient = recentRecipients.get(a);
                RecentRecipientCell recipientCell = new RecentRecipientCell(context);
                recipientCell.setBackground(Theme.getSelectorDrawable(false));
                recipientCell.setRecipient(recipient, a != recentRecipients.size() - 1);
                recipientCell.setOnClickListener(v -> {
                    addressEditTextCell.setText(recipient.address, false);
                });
                linearLayout.addView(recipientCell);
            }
        }

        return viewGroup;
    }

    private void onAddressInputContinueButtonClick (View v) {
        final boolean isDomain = Utilities.isValidTonDomain(inputFieldRecipientString);
        final boolean isAddress = !isDomain && Utilities.isValidWalletAddress(inputFieldRecipientString);
        if (!isDomain && !isAddress) {
            incorrectDomainWarningView.showWithAction(UndoView.ACTION_WRONG_ADDRESS);
            AndroidUtilities.shakeAndVibrateView(addressEditTextCell, 2, 0);
            return;
        }

        if (isDomain) {
            nextButton.showProgress(true, true);
            getTonController().resolveDnsWallet(inputFieldRecipientString, s -> {
                nextButton.showProgress(false, true);
                if (s != null) {
                    delegate.onAddressInputSuccess(s, inputFieldRecipientString);
                } else {
                    incorrectDomainWarningView.showWithAction(UndoView.ACTION_WRONG_DOMAIN);
                    AndroidUtilities.shakeAndVibrateView(addressEditTextCell, 2, 0);
                }
            });

            return;
        }

        delegate.onAddressInputSuccess(inputFieldRecipientString, null);
    }

    private void updateEditField () {
        addressEditTextCell.setTextAndHint(!TextUtils.isEmpty(inputFieldRecipientString) ? inputFieldRecipientString : getters.getRecipientAddressOrDomain(), LocaleController.getString("WalletEnterWalletAddress", R.string.WalletEnterWalletAddress), false);

    }

    public void setInputFieldRecipientString (String inputFieldRecipientString) {
        this.inputFieldRecipientString = inputFieldRecipientString;
        updateEditField();
    }

    @Override
    protected void onPrepareToShow () {
       updateEditField();
    }

    @Override
    protected int getHeight () {
        return linearLayout.getMeasuredHeight() + getPopupPaddingTop() + getPopupPaddingBottom() + getScrollPaddingBottom();
    }

    @Override
    public int getScrollPaddingBottom () {
        return AndroidUtilities.dp(64);
    }

    @Override
    public int getPopupPaddingTop () {
        return ActionBar.getCurrentActionBarHeight();
    }

    @Override
    public int getPopupPaddingBottom () {
        return AndroidUtilities.dp(80);
    }

    @Override
    public boolean usePadding () {
        return true;
    }

    public static class InputHelpersButtons extends LinearLayout {
        private OnClickListener delegate;

        interface OnClickListener {
            void onPaste ();

            void onScan ();
        }

        public InputHelpersButtons (Context context) {
            super(context);
            setOrientation(HORIZONTAL);

            SmallTextButton pasteButton = new SmallTextButton(context);
            pasteButton.setTypeface(Typefaces.INTER_MEDIUM);
            pasteButton.setText(LocaleController.getString("WalletPaste", R.string.WalletPaste));
            pasteButton.setDrawable(R.drawable.baseline_paste_20);
            pasteButton.setOnClickListener(v -> {
                if (delegate != null) {
                    delegate.onPaste();
                }
            });

            SmallTextButton scanButton = new SmallTextButton(context);
            scanButton.setText(LocaleController.getString("WalletScan", R.string.WalletScan));
            scanButton.setTypeface(Typefaces.INTER_MEDIUM);
            scanButton.setDrawable(R.drawable.baseline_qr_scan_send_20);
            scanButton.setOnClickListener(v -> {
                if (delegate != null) {
                    delegate.onScan();
                }
            });

            addView(pasteButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 12, 0, 8, 0));
            addView(scanButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }

        public void setDelegate (OnClickListener delegate) {
            this.delegate = delegate;
        }

        @Override
        protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(36), MeasureSpec.EXACTLY));
        }
    }

    private final InputHelpersButtons.OnClickListener inputHelpersOnClickListener = new InputHelpersButtons.OnClickListener() {
        @Override
        public void onPaste () {
            AndroidUtilities.pasteFromClipboard(addressEditTextCell.getTextView());
        }

        @Override
        public void onScan () {
            getParentFragment().openQrCodeReader(r -> {
                UriParser.Result result = UriParser.parse(r);
                if (result != null) {
                    delegate.onParseQrCode(result);
                } else {
                    AlertsCreator.createSimpleAlert(getParentFragment().getParentActivity(), LocaleController.getString("WalletQRCode", R.string.WalletQRCode), LocaleController.getString("WalletScanImageNotFound", R.string.WalletScanImageNotFound)).show();
                }
                if (result instanceof UriParser.ResultTonLink) {
                    setInputFieldRecipientString(((UriParser.ResultTonLink) result).address);
                } else if (result instanceof UriParser.ResultTonDomain) {
                    setInputFieldRecipientString(((UriParser.ResultTonDomain) result).domain);
                }
            });
        }
    };
}
