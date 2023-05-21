package org.UI.Sheets.Jetton;

import android.content.Context;
import android.view.Gravity;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.UI.Cells.AddressEditTextCell;
import org.UI.Components.Buttons.DefaultButtonView;
import org.telegram.ui.Components.LayoutHelper;
import org.UI.Sheets.Templates.BottomSheetScrollable;
import org.telegram.ui.Components.UndoView;
import org.Utils.Callbacks;

public class TokenAddSheet extends BottomSheetScrollable {
    private final UndoView incorrectDomainWarningView;
    private final AddressEditTextCell addressEditTextCell;
    private final DefaultButtonView addTokenButton;
    private final Callbacks.RootJettonContractCallback callback;

    public TokenAddSheet (BaseFragment parent, Callbacks.RootJettonContractCallback callback) {
        super(parent);
        setFocusable(true);
        this.callback = callback;

        Context context = parent.getParentActivity();

        addTokenButton = new DefaultButtonView(context);
        addressEditTextCell = new AddressEditTextCell(context);
        incorrectDomainWarningView = new UndoView(getContext());

        init(parent.getParentActivity());
    }

    private void init (Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("WalletAddToken", R.string.WalletAddToken));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.getBackButton().getAlpha() > 0f) {
                        dismiss();
                    }
                }
            }
        });


        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(LocaleController.getString("WalletRootTokenAddress", R.string.WalletRootTokenAddress));
        linearLayout.addView(headerCell);

        addressEditTextCell.setTextAndHint("", LocaleController.getString("WalletEnterRootTokenAddress", R.string.WalletEnterRootTokenAddress), false);
        linearLayout.addView(addressEditTextCell);

        TextInfoPrivacyCell textInfoPrivacyCell = new TextInfoPrivacyCell(context);
        textInfoPrivacyCell.setText(LocaleController.getString("WalletSendPasteDescription", R.string.WalletSendPasteDescription));
        linearLayout.addView(textInfoPrivacyCell);

        addTokenButton.setText(LocaleController.getString("WalletAddToken", R.string.WalletAddToken));
        addTokenButton.setCollapseIcon(R.drawable.ic_done);
        addTokenButton.setOnClickListener(this::onAddButtonClick);
        containerView.addView(addTokenButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 0, 16, 16));

        containerView.addView(incorrectDomainWarningView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));
    }

    private void onAddButtonClick (View v) {
        final String address = addressEditTextCell.getText();
        final boolean isDomain = Utilities.isValidTonDomain(address);
        final boolean isAddress = !isDomain && Utilities.isValidWalletAddress(address);
        if (!isDomain && !isAddress) {
            incorrectDomainWarningView.showWithAction(UndoView.ACTION_WRONG_ADDRESS);
            AndroidUtilities.shakeAndVibrateView(addressEditTextCell, 2, 0);
            return;
        }

        addTokenButton.setEnabled(false);
        addTokenButton.showProgress(true, true);
        if (isDomain) {
            parentFragment.getTonController().resolveDnsWallet(address, s -> {
                if (s != null) {
                    onAddressInputSuccess(s);
                } else {
                    incorrectDomainWarningView.showWithAction(UndoView.ACTION_WRONG_ADDRESS);
                    AndroidUtilities.shakeAndVibrateView(addressEditTextCell, 2, 0);
                    addTokenButton.setEnabled(true);
                    addTokenButton.showProgress(false, true);
                }
            });
            return;
        }
        onAddressInputSuccess(address);
    }

    private void onAddressInputSuccess (String address) {
        getTonController().fetchJettonToken(address, contract -> {
            getTonController().getJettonsCache().save(contract);
            addTokenButton.showProgress(false, true);
            addTokenButton.collapse();
            callback.run(contract);
        }, (text, error) -> {
            incorrectDomainWarningView.showWithAction(UndoView.ACTION_JETTON_PARSE_ERROR);
            AndroidUtilities.shakeAndVibrateView(addressEditTextCell, 2, 0);
            addTokenButton.setEnabled(true);
            addTokenButton.showProgress(false, true);
        });
    }

    @Override
    public int getScrollPaddingBottom() {
        return AndroidUtilities.dp(80);
    }

    @Override
    public int getPopupPaddingTop() {
        return ActionBar.getCurrentActionBarHeight();
    }

    @Override
    public int getPopupPaddingBottom() {
        return AndroidUtilities.dp(80);
    }
}
