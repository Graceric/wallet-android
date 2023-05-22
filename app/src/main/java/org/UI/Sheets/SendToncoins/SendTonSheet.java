package org.UI.Sheets.SendToncoins;

import androidx.annotation.Nullable;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.TonController.Parsers.UriParser;
import org.TonController.TonController;
import org.UI.Fragments.Main.WalletActivity;
import org.UI.Sheets.Templates.BottomSheetPaginated;
import org.Utils.Callbacks;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.AlertsCreator;

import javax.crypto.Cipher;

import drinkless.org.ton.TonApi;

public class SendTonSheet extends BottomSheetPaginated implements Callbacks.SendInfoGetters, SendTonAddressInputSheetPage.Delegate {

    private final WalletActivity parent;
    private final @Nullable RootJettonContract rootJettonContract;

    public SendTonSheet (WalletActivity fragment, TonAmount balance, @Nullable RootJettonContract rootJettonContract) {
        super(fragment);
        setFocusable(true);
        parent = fragment;
        currentUserBalance = balance;
        inputFieldSendAmountValue = new TonAmount(rootJettonContract != null ? rootJettonContract.content.decimals: 9);
        this.rootJettonContract = rootJettonContract;
    }

    private void trySend (TonController.UserAuthInfo auth) {
        trySend(auth.passcode, auth.cipher);
        viewPager.setCurrentItem(3);
    }

    private void trySend (String passcode, Cipher cipher) {
        final TonController.SendTonArgs args = new TonController.SendTonArgs(
            inputFieldRecipientAddressString,
            inputFieldRecipientDomainString,
            inputFieldSendAmountValue.getBalanceSafe().longValue(),
            inputFieldCommentString,
            inputFieldSendAllFlag ? 128 : 1,
            rootJettonContract
        );

        getTonController().sendTransaction(new TonController.UserAuthInfo(passcode, cipher, null), args, new TonController.SendTransactionCallbacks() {
            @Override
            public void onTransactionCreate () {
                viewPager.setCurrentItem(3);
            }

            @Override
            public void onTransactionSendSuccess () {
                viewPager.setCurrentItem(4);
            }

            @Override
            public void onTransactionSendError (String text, TonApi.Error error) {
                if (error != null && error.message.startsWith("NOT_ENOUGH_FUNDS")) {
                    AlertDialog.Builder builder = AlertsCreator.createSimpleAlert(getParentFragment().getParentActivity(), LocaleController.getString("WalletInsufficientGramsTitle", R.string.WalletInsufficientGramsTitle), LocaleController.getString("WalletInsufficientGramsText", R.string.WalletInsufficientGramsText));
                    getParentFragment().showDialog(builder.create(), dialog -> {
                    });
                } else {
                    AlertDialog.Builder builder = AlertsCreator.createSimpleAlert(getParentFragment().getParentActivity(), LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + (error != null ? error.message : text));
                    getParentFragment().showDialog(builder.create(), dialog -> {
                    });
                }
            }
        });
    }



    /* Qr Code */

    @Override
    public void onParseQrCode (UriParser.Result result) {
        onParseQrCode(result, false);
    }

    public void onParseQrCode (UriParser.Result result, boolean needUpdatePage) {
        if (result instanceof UriParser.ResultTonDomain) {
            onParseTonDomain((UriParser.ResultTonDomain) result);
        } else if (result instanceof UriParser.ResultTonLink) {
            onParseTonLink((UriParser.ResultTonLink) result, needUpdatePage);
        } else if (result != null) {
            parent.onParseQrCode(result);
            dismiss();
        }
    }

    private void onParseTonDomain (UriParser.ResultTonDomain domain) {
        inputFieldRecipientDomainString = domain.domain;
        if (pages[0] != null) ((SendTonAddressInputSheetPage) pages[0]).setInputFieldRecipientString(domain.domain);
    }

    private void onParseTonLink (UriParser.ResultTonLink link, boolean needUpdatePage) {
        inputFieldRecipientAddressString = link.address;
        inputFieldSendAmountValue.setBalance(link.amount);
        inputFieldCommentString = link.comment != null ? link.comment : "";

        if (pages[0] != null) ((SendTonAddressInputSheetPage) pages[0]).setInputFieldRecipientString(link.address);
        if (pages[1] != null) ((SendTonAmountInputSheetPage) pages[1]).setInputFieldSendAmountValue(link.amount);
        if (pages[2] != null) ((SendTonCommentInputSheetPage) pages[2]).setInputFieldCommentString(link.comment);

        if (needUpdatePage) {
          //  viewPager.setCurrentItem(link.amount != 0 ? 2: 1, false);
        }
    }



    /* Fields */

    private String inputFieldRecipientAddressString;
    private String inputFieldRecipientDomainString;
    private String inputFieldCommentString = "";
    private final TonAmount currentUserBalance;
    private final TonAmount inputFieldSendAmountValue;
    private boolean inputFieldSendAllFlag;



    /* Setters */

    @Override
    public void onAddressInputSuccess (String address, String domain) {
        inputFieldRecipientAddressString = address;
        inputFieldRecipientDomainString = domain;
        viewPager.setCurrentItem(1);
    }

    private void onAmountInputSuccess (long amount, boolean sendAllFlag) {
        inputFieldSendAmountValue.setBalance(amount);
        inputFieldSendAllFlag = sendAllFlag;
        viewPager.setCurrentItem(2);
    }

    private void onCommentInputSuccess (String comment) {
        this.inputFieldCommentString = comment;
        AndroidUtilities.hideKeyboard(getCurrentFocus());
        getParentFragment().requestPasscodeOrBiometricAuth(LocaleController.getString("WalletSendConfirmCredentials", R.string.WalletSendConfirmCredentials), this::trySend);
    }



    /* Getters */

    @Override
    public String getInputFieldRecipientAddressString () {
        return inputFieldRecipientAddressString;
    }

    @Override
    public String getInputFieldRecipientDomainString () {
        return inputFieldRecipientDomainString;
    }

    @Override
    public String getInputFieldCommentString () {
        return inputFieldCommentString;
    }

    @Override
    public TonAmount getInputFieldSendAmountValue () {
        return inputFieldSendAmountValue;
    }

    @Override
    public TonAmount getCurrentWalletBalance () {
        return currentUserBalance;
    }



    /* Pages */

    @Override
    public Page createPageAtPosition (int position) {
        if (position == 0) {
            SendTonAddressInputSheetPage p = new SendTonAddressInputSheetPage(rootJettonContract);
            p.setSendInfoGetters(this, this);
            return p;
        } else if (position == 1) {
            SendTonAmountInputSheetPage p = new SendTonAmountInputSheetPage(rootJettonContract);
            p.setSendInfoGetters(this, this::onAmountInputSuccess);
            return p;
        } else if (position == 2) {
            SendTonCommentInputSheetPage p = new SendTonCommentInputSheetPage(rootJettonContract);
            p.setSendInfoGetters(this, this::onCommentInputSuccess);
            return p;
        } else if (position == 3 || position == 4) {
            SendTonWaitingSendSheetPage p = new SendTonWaitingSendSheetPage(rootJettonContract, position == 3 ? SendTonWaitingSendSheetPage.MODE_WAITING : SendTonWaitingSendSheetPage.MODE_FINISHED);
            p.setSendInfoGetters(this);
            return p;
        }
        return null;
    }

    @Override
    public int getPagesCount () {
        return 5;
    }
}
