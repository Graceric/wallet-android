package org.UI.Sheets.TonConnect;

import org.TonController.Data.TonAmount;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.TonController.TonController;
import org.UI.Sheets.SendToncoins.SendTonWaitingSendSheetPage;
import org.UI.Sheets.Templates.BottomSheetPaginated;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.Utils.Callbacks;

import drinkless.org.ton.TonApi;

public class TonConnectSendSheet extends BottomSheetPaginated {
    private final RequestSendTransaction request;
    private Runnable goToLoadingRunnable;
    private final TonAmount walletBalance;

    public TonConnectSendSheet (BaseFragment parent, RequestSendTransaction request, TonAmount walletBalance) {
        super(parent);
        this.request = request;
        this.walletBalance = walletBalance;
    }

    @Override
    protected int getPagesCount () {
        return 3;
    }

    @Override
    protected Page createPageAtPosition (int position) {
        if (position == 0) {
            TonConnectSendPage p = new TonConnectSendPage(request, walletBalance);
            p.setSendCallbacks(new TonController.SendTransactionCallbacks() {
                @Override
                public void onTransactionCreate () {
                    goToLoadingRunnable = () -> {
                        viewPager.setCurrentItem(1);
                        goToLoadingRunnable = null;
                    };
                    AndroidUtilities.runOnUIThread(goToLoadingRunnable, 750);
                }

                @Override
                public void onTransactionSendSuccess () {
                    if (goToLoadingRunnable != null) {
                        AndroidUtilities.cancelRunOnUIThread(goToLoadingRunnable);
                    }
                    viewPager.setCurrentItem(2);
                }

                @Override
                public void onTransactionSendError (String text, TonApi.Error error) {
                    getParentFragment().defaultErrorCallback(text, error);
                }
            });
            return p;
        } else {
            int mode = position == 1 ? SendTonWaitingSendSheetPage.MODE_WAITING: SendTonWaitingSendSheetPage.MODE_FINISHED;
            SendTonWaitingSendSheetPage p = new SendTonWaitingSendSheetPage(null, mode);
            p.setSendInfoGetters(new Callbacks.SendInfoGetters() {
                @Override
                public TonAmount getCurrentWalletBalance () {
                    return TonAmount.EMPTY;
                }

                @Override
                public String getInputFieldRecipientAddressString () {
                    return request.messages[0].address;
                }

                @Override
                public String getInputFieldCommentString () {
                    return null;
                }

                @Override
                public String getInputFieldRecipientDomainString () {
                    return null;
                }

                @Override
                public TonAmount getInputFieldSendAmountValue () {
                    return TonAmount.valueOf(request.totalAmount);
                }
            });
            return p;
        }

    }
}
