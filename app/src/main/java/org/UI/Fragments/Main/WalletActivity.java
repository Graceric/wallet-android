package org.UI.Fragments.Main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.Data.Jettons.JettonContract;
import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.TonAmount;
import org.TonController.Data.WalletTransaction;
import org.TonController.Data.WalletTransactionMessage;
import org.TonController.Parsers.UriParser;
import org.TonController.TonConnect.Requests.Request;
import org.TonController.TonConnect.Requests.RequestSendTransaction;
import org.UI.Cells.WalletActivityButtonsCell;
import org.UI.Cells.WalletDateCell;
import org.UI.Cells.WalletTransactionCell;
import org.UI.Fragments.Create.WalletCreateReadyActivity;
import org.UI.Fragments.Create.WalletCreateStartActivity;
import org.UI.Fragments.Settings.SettingsMainActivity;
import org.UI.Sheets.ReceiveTonSheet;
import org.UI.Sheets.SendToncoins.SendTonSheet;
import org.UI.Sheets.TonConnect.TonConnectInfoSheet;
import org.UI.Sheets.TonConnect.TonConnectInitialSheet;
import org.UI.Sheets.TonConnect.TonConnectSendSheet;
import org.UI.Sheets.TonConnect.TonConnectSwitchAddressSheet;
import org.UI.Sheets.TransactionInfoSheet;
import org.Utils.Callbacks;
import org.Utils.Settings;
import org.Utils.network.CoingeckoApi;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;

import java.util.ArrayList;

import drinkless.org.ton.TonApi;
import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import me.vkryl.core.MathUtils;

public class WalletActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, FactorAnimator.Target {

    private static final int menu_settings = 1;
    private static final int menu_qr_code = 2;

    /* Animators */

    private static final int WALLET_PRESENT_ANIMATOR_ID = 14;
    private final BoolAnimator walletPresentVisibility = new BoolAnimator(WALLET_PRESENT_ANIMATOR_ID, this, AnimatorUtils.LINEAR_INTERPOLATOR, 380L);

    /* Callbacks */

    private final Callbacks.AccountStateCallbackExtended onAccountLastTransactionUpdate = this::onAccountLastTransactionUpdateListener;
    private final Callbacks.JettonBalanceUpdateCallback onUpdateJettonBalanceRunnable = this::onAccountUpdateJettonBalance;

    /* Views */

    private WalletActivityContainerView customContainerView;
    private PullRecyclerView listView;
    private LinearLayoutManager layoutManager;
    private WalletActivityListAdapter adapter;
    private WalletActivityInfoLayoutView walletInfoLayoutView;
    private UndoView undoView;

    private @Nullable ActionBarPopupWindow tokenSelectorPopupWindow;
    private @Nullable WalletActivityTokenSelectorLayout tokenSelectorLayout;
    private @Nullable WalletPresentView walletPresentView;

    // * * //

    private String currentTonWalletAddress;
    private final TonAmount currentTonWalletAccountBalance = new TonAmount(9);
    private TonApi.RawFullAccountState currentTonWalletAccountState;

    private @Nullable RootJettonContract currentJettonRootContract;
    private @Nullable JettonContract currentJettonWalletContract;
    private String jettonWalletAddress;
    private boolean accountStateLoaded;
    private long lastUpdateTime;
    private boolean loadingTransactions;
    private boolean loadingAccountState;

    private boolean hasNetworkConnection = true;
    private long startArchivePullingTime;
    private boolean canShowHiddenPull;
    private boolean wasPulled;
    private final int presentType;

    public WalletActivity () {
        this(WalletCreateReadyActivity.TYPE_READY_NONE);
    }

    public WalletActivity (int presentType) {
        this.presentType = presentType;

    }

    @Override
    public View createView (Context context) {
        WalletActivityContainerView frameLayout = new WalletActivityContainerView(context);
        frameLayout.setBottomCellPositionGetter(this::getBalanceCellBottom);
        frameLayout.setOnUpdateLayoutRunnable(this::updateLayout);
        fragmentView = customContainerView = frameLayout;

        Drawable pinnedHeaderShadowDrawable = context.getResources().getDrawable(R.drawable.photos_header_shadow);
        pinnedHeaderShadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundGrayShadow), PorterDuff.Mode.MULTIPLY));

        listView = new PullRecyclerView(context);

        adapter = new WalletActivityListAdapter(context);
        adapter.setButtonsListeners(this::onReceivePressed, this::onSendPressed);
        adapter.setDrawables(listView.getPullForegroundDrawable(), frameLayout.getBackgroundDrawable());

        listView.setSectionsType(2);
        listView.setPinnedHeaderShadowDrawable(pinnedHeaderShadowDrawable);
        listView.setLayoutManager(layoutManager = new PullLinearLayoutManager(context));
        listView.setAdapter(adapter);
        listView.setGlowColor(Theme.getColor(Theme.key_wallet_blackBackground));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addItemDecoration(new WalletActivityListDecorator());
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled (@NonNull RecyclerView recyclerView, int dx, int dy) {
                fragmentView.invalidate();
                if (!loadingTransactions && !adapter.isTransactionsEndReached()) {
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int visibleItemCount = lastVisibleItem == RecyclerView.NO_POSITION ? 0 : lastVisibleItem;
                    if (visibleItemCount > 0 && lastVisibleItem > adapter.getItemCount() - 4) {
                        loadTransactions(false);
                    }
                }
                updateScrollPaddings();
            }

            @Override
            public void onScrollStateChanged (@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                onUserScrollFinished();
            }
        });
        listView.setOnItemClickListener((view, position) -> {
            if (getParentActivity() == null) {
                return;
            }
            if (view instanceof WalletTransactionCell) {
                WalletTransactionCell cell = (WalletTransactionCell) view;
                if (cell.isEmpty()) {
                    return;
                }
                WalletTransactionMessage transaction = cell.getWalletTransactionMessage();
                TransactionInfoSheet s = new TransactionInfoSheet(this, transaction, cell.getWalletTransactionRepresentation(), cell.getRootJettonContract());
                s.setOnClickSendDelegate(address -> {
                    SendTonSheet s2 = new SendTonSheet(WalletActivity.this, getCurrentTonOrJettonWalletBalance(), currentJettonRootContract);
                    s2.onParseQrCode(UriParser.ResultTonLink.valueOf(address));
                    s2.show();
                });
                showDialog(s, s::onDismiss);
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        frameLayout.addView(actionBar);

        walletInfoLayoutView = new WalletActivityInfoLayoutView(context);
        walletInfoLayoutView.setBalanceBottomCellGetter(this::getBalanceCellBottom);
        walletInfoLayoutView.setActionBarIsFullVisible(true, false);
        walletInfoLayoutView.setOnBalanceClickListener(this::onBalanceViewClick);
        walletInfoLayoutView.setOnPositionsUpdateListener(() -> {
            if (tokenSelectorPopupWindow != null && tokenSelectorLayout != null) {
                tokenSelectorLayout.setBalanceViewX(walletInfoLayoutView.getMainBalanceViewTranslationX());
                tokenSelectorLayout.setBalanceViewY(walletInfoLayoutView.getMainBalanceViewTranslationY());
            }
        });
        frameLayout.addView(walletInfoLayoutView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        updateBalanceAndAddressView();
        updateActionBarStatus(false);
        checkWalletStatusCellsVisible(false);

        undoView = new UndoView(context);
        frameLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        if (presentType != WalletCreateReadyActivity.TYPE_READY_NONE) {
            walletPresentView = new WalletPresentView(context, presentType);
            walletPresentVisibility.setValue(true, false);
            customContainerView.setWalletPresentVisibilityFactor(1f);
            frameLayout.addView(walletPresentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        setTonWalletAddress(getTonController().getCurrentWalletAddress(), false, false);
        setJettonRootContract(getTonAccountStateManager().getJettonContractsStorage().get(
            getTonController().getCurrentJettonRootAddress()), false);

        return fragmentView;
    }

    public void openTransfer (UriParser.Result result) {
        if (getParentActivity() == null || currentTonWalletAddress == null || !getTonAccountStateManager().isLoadedAccount(currentTonWalletAddress)) {
            return;
        }
        SendTonSheet sheet = new SendTonSheet(WalletActivity.this, getCurrentTonOrJettonWalletBalance(), currentJettonRootContract);
        if (result != null) sheet.onParseQrCode(result, true);
        sheet.show();
    }

    private void forceLoadAccountState () {
        if (loadingAccountState) {
            return;
        }
        loadingAccountState = true;

        if (currentTonWalletAddress == null) {
            getTonController().guessAccountAddress(address -> {
                loadingAccountState = false;
                if (address == null) {
                    retryForceLoadAccountState();
                }
            });
        } else {
            getTonAccountStateManager().fetchRawFullAccountState(currentTonWalletAddress, state -> {
                loadingAccountState = false;
                loadTransactions(true);
            });
        }
    }

    private void retryForceLoadAccountState () {
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() != null) {
                forceLoadAccountState();
            }
        }, 1500);
    }

    private boolean fillTransactions (ArrayList<WalletTransaction> arrayList, boolean reload, boolean animated) {
        adapter.setMode(currentJettonRootContract != null ?
            WalletActivityListAdapter.TRANSACTIONS_MODE_JETTON :
            WalletActivityListAdapter.TRANSACTIONS_MODE_DEFAULT);
        adapter.setRootJettonContract(currentJettonRootContract);

        boolean result = adapter.fillTransactions(arrayList, getTonAccountStateManager().getPendingTransactions(currentTonWalletAddress), reload);
        checkWalletStatusCellsVisible(animated);
        listView.invalidateItemDecorations();

        return result;
    }

    private void loadTransactions (boolean reload) {
        String address = getCurrentTonOrJettonWalletAddress();
        loadTransactions(address, reload);
    }

    private void loadTransactions (String address, boolean reload) {
        if (getTonAccountStateManager().isLoadedAccountWithEmptyTransactions(address)) return;
        if (loadingTransactions || (!accountStateLoaded && currentJettonRootContract == null)) {
            return;
        }
        if (address == null) return;
        if (TextUtils.equals(address, getCurrentTonOrJettonWalletAddress())) {
            loadingTransactions = true;
        }

        getTonAccountStateManager().fetchRawTransactions(address, reload, (rawTransactions, parsedTransactions) ->
            onTransactionsLoad(address, reload, rawTransactions, parsedTransactions));
    }

    private void onTransactionsLoad (String address, boolean reload, TonApi.RawTransactions rawTransactions, ArrayList<WalletTransaction> transactions) {
        if (!TextUtils.equals(address, getCurrentTonOrJettonWalletAddress())) return;

        if (reload) onFinishGettingAccountState();
        loadingTransactions = false;
        currentTonWalletAccountState = getTonAccountStateManager().getCachedFullAccountState(currentTonWalletAddress);
        if (currentTonWalletAccountState != null) {
            currentTonWalletAccountBalance.setBalance(Math.max(currentTonWalletAccountState.balance, 0));
            currentTonWalletAccountBalance.invalidate();
        } else {
            currentTonWalletAccountBalance.clear();
        }
        if (!fillTransactions(transactions, reload, true) && !reload) {
            adapter.setTransactionsEndReached(true);
        }
    }

    private void onAccountLastTransactionUpdateListener (String address, TonApi.RawFullAccountState state, boolean needUpdateTransactions) {
        String currentTonOrJettonAddress = getCurrentTonOrJettonWalletAddress();
        boolean isTonWalletUpdate = TextUtils.equals(address, currentTonWalletAddress);
        boolean isCurrentWalletUpdate = TextUtils.equals(address, currentTonOrJettonAddress);

        if (isTonWalletUpdate) {
            currentTonWalletAccountState = state;
            currentTonWalletAccountBalance.setBalance(Math.max(currentTonWalletAccountState.balance, 0));
        }

        if (isCurrentWalletUpdate) {
            accountStateLoaded = true;
            updateBalanceAndAddressView();
            boolean realNeedUpdateTransactions = (needUpdateTransactions ||
                (currentTonWalletAccountState == null || adapter.isEmpty() || !accountStateLoaded) &&
                    !getTonAccountStateManager().isLoadedAccountWithEmptyTransactions(currentTonOrJettonAddress)
            );
            if (realNeedUpdateTransactions) {
                loadTransactions(true);
            } else {
                onFinishGettingAccountState();
            }
            checkWalletStatusCellsVisible(true);
        } else if (currentJettonRootContract != null && isTonWalletUpdate) {
            if (needUpdateTransactions) {
                loadTransactions(currentTonWalletAddress, true);
            }
        }
    }

    private void onFinishGettingAccountState () {    /// ????
        if (currentTonWalletAccountState == null) {
            return;
        }
        lastUpdateTime = Utilities.getLastSyncTime(currentTonWalletAccountState);
        updateActionBarStatus(true);
        loadingAccountState = false;
    }







    /* Wallets Switching */

    public @Nullable String getCurrentTonOrJettonWalletAddress () {
        return currentJettonRootContract != null ? jettonWalletAddress : currentTonWalletAddress;
    }

    public TonAmount getCurrentTonOrJettonWalletBalance () {
        if (currentJettonRootContract == null) {
            return currentTonWalletAccountBalance;
        } else if (currentJettonWalletContract != null) {
            return currentJettonWalletContract.balance;
        }
        return TonAmount.EMPTY;
    }

    public void onBalanceViewClick (View v) {
        ArrayList<RootJettonContract> tokens = getTonController().getJettonsCache().getTokensList();
        if (tokens.size() == 0) return;
        if (tokens.size() == 1) {
            getTonController().updateJettonRootTokenAddress(currentJettonRootContract == null ? tokens.get(0).address: null);
            return;
        }

        tokenSelectorLayout = new WalletActivityTokenSelectorLayout(v.getContext());
        tokenSelectorPopupWindow = new ActionBarPopupWindow(tokenSelectorLayout, LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT);
        tokenSelectorLayout.setWindow(tokenSelectorPopupWindow);
        tokenSelectorLayout.setTokensList(tokens,
            currentJettonRootContract != null ? currentJettonRootContract.address : null
        );
        tokenSelectorLayout.setBalanceViewX(walletInfoLayoutView.getMainBalanceViewTranslationX());
        tokenSelectorLayout.setBalanceViewY(walletInfoLayoutView.getMainBalanceViewTranslationY());
        tokenSelectorLayout.setTokenSelectCallback(tokenAddress ->
            getTonController().updateJettonRootTokenAddress(tokenAddress));
        tokenSelectorLayout.appear();

        tokenSelectorPopupWindow.setAnimationEnabled(false);
        tokenSelectorPopupWindow.setAnimationStyle(R.style.PopupAnimation);
        tokenSelectorPopupWindow.setClippingEnabled(true);
        tokenSelectorPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        tokenSelectorPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        tokenSelectorPopupWindow.getContentView().setFocusableInTouchMode(true);
        tokenSelectorPopupWindow.setFocusable(false);
        tokenSelectorPopupWindow.showAtLocation(walletInfoLayoutView, Gravity.LEFT | Gravity.TOP, 0, 0);
        tokenSelectorPopupWindow.setOnDismissListener(() -> {
            tokenSelectorPopupWindow = null;
            tokenSelectorLayout = null;
        });
    }

    private void setTonWalletAddress (String address, boolean animated, boolean force) {
        if (TextUtils.equals(address, currentTonWalletAddress) && !force) {
            setJettonRootContract(null, animated);
            return;
        }

        setJettonRootContract(null, animated);
        if (currentTonWalletAddress != null) {
            getTonAccountStateManager().unsubscribeFromRawAccountStateUpdates(currentTonWalletAddress, onAccountLastTransactionUpdate);
        }

        adapter.clear();
        loadingTransactions = false;
        loadingAccountState = false;
        accountStateLoaded = false;
        currentTonWalletAddress = address;

        if (currentTonWalletAddress != null) {
            currentTonWalletAccountState = getTonAccountStateManager().getCachedFullAccountState(currentTonWalletAddress);
            if (currentTonWalletAccountState != null) {
                currentTonWalletAccountBalance.setBalance(Math.max(currentTonWalletAccountState.balance, 0));
                currentTonWalletAccountBalance.invalidate();
            } else {
                currentTonWalletAccountBalance.clear();
            }
            fillTransactions(getTonAccountStateManager().getCachedTransactions(currentTonWalletAddress), true, animated);
            getTonAccountStateManager().subscribeToRawAccountStateUpdates(currentTonWalletAddress, onAccountLastTransactionUpdate);
        } else {
            currentTonWalletAccountState = null;
            currentTonWalletAccountBalance.clear();
        }

        updateBalanceAndAddressView();
        forceLoadAccountState();
    }

    private void setJettonRootContract (RootJettonContract contract, boolean animated) {
        if (jettonWalletAddress != null && contract != null && currentJettonRootContract != null
            && TextUtils.equals(currentJettonRootContract.address, contract.address)) return;
        if (currentJettonRootContract == null && contract == null) return;

        walletInfoLayoutView.setDisplayJettonToken(contract != null);

        if (jettonWalletAddress != null) {
            getTonAccountStateManager().subscribeToRawAccountStateUpdates(jettonWalletAddress, onAccountLastTransactionUpdate);
            getTonAccountStateManager().unsubscribeFromJettonBalanceUpdates(jettonWalletAddress, onUpdateJettonBalanceRunnable);
        }

        walletInfoLayoutView.setTokenIconUrl(contract != null ? contract.content.imageUrl : null, animated);
        currentJettonRootContract = contract;

        if (currentJettonRootContract == null) {
            jettonWalletAddress = null;
            currentJettonWalletContract = null;
            setTonWalletAddress(currentTonWalletAddress, animated, true);
            return;
        }

        adapter.clear();
        loadingTransactions = false;

        jettonWalletAddress = getTonAccountStateManager().getCachedJettonWalletAddress(currentTonWalletAddress, currentJettonRootContract.address);
        if (jettonWalletAddress == null) {
            currentJettonWalletContract = null;
            final String finalWalletAddress = currentTonWalletAddress;
            final String finalRootTokenAddress = currentJettonRootContract.address;
            getTonAccountStateManager().fetchJettonWalletAddress(currentTonWalletAddress, finalRootTokenAddress, addr -> {
                if (!TextUtils.equals(currentTonWalletAddress, finalWalletAddress)) return;
                if (finalRootTokenAddress == null || !TextUtils.equals(currentJettonRootContract.address, finalRootTokenAddress))
                    return;
                setJettonRootContract(contract, true);
            });
            updateBalanceAndAddressView();
            return;
        }

        currentJettonWalletContract = getTonAccountStateManager().getCachedJettonWallet(jettonWalletAddress);

        fillTransactions(getTonAccountStateManager().getCachedTransactions(jettonWalletAddress), true, animated);
        getTonAccountStateManager().subscribeToJettonBalanceUpdates(jettonWalletAddress, onUpdateJettonBalanceRunnable);
        getTonAccountStateManager().subscribeToRawAccountStateUpdates(jettonWalletAddress, onAccountLastTransactionUpdate);
        updateBalanceAndAddressView();
    }

    private void onAccountUpdateJettonBalance (String jettonWalletAddress, long balance) {
        updateBalanceAndAddressView();
    }



    /* Views update functions */

    private float getBalanceCellBottom () {
        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(1);
        return ((holder != null) ? holder.itemView.getBottom() : 0) + listView.getViewOffset();
    }

    private void updateLayout () {
        FrameLayout.LayoutParams layoutParams;

        layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.topMargin = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;

        layoutParams = (FrameLayout.LayoutParams) walletInfoLayoutView.getLayoutParams();
        layoutParams.topMargin = AndroidUtilities.statusBarHeight;
    }

    private void onUserScrollFinished () {
        int bottom = (int) getBalanceCellBottom();
        if (bottom < 3) return;
        if (bottom < AndroidUtilities.dp(40)) {
            listView.stopScroll();
            listView.smoothScrollBy(0, bottom);
        } else if (bottom < AndroidUtilities.dp(170)) {
            listView.stopScroll();
            listView.smoothScrollBy(0, bottom - AndroidUtilities.dp(72));
        } /*else if (bottom < AndroidUtilities.dp(WalletSendReceiveButtonsCell.HEIGHT)) {
            listView.stopScroll();
            listView.smoothScrollBy(0, bottom - AndroidUtilities.dp(WalletSendReceiveButtonsCell.HEIGHT));
        }*/
    }

    private void updateScrollPaddings () {
        float bottom = getBalanceCellBottom();
        float progress = Math.max(Math.min((bottom - AndroidUtilities.dp(80 - 48)) / AndroidUtilities.dp(WalletActivityButtonsCell.HEIGHT - 80), 1f), 0f);
        boolean needActionBarCompact = progress < 1f;

        walletInfoLayoutView.setActionBarIsFullVisible(!needActionBarCompact, true);
        walletInfoLayoutView.checkActionBarViewsPosition();
    }

    private void updateBalanceAndAddressView () {
        if (walletInfoLayoutView == null) return;

        walletInfoLayoutView.setAddress(getCurrentTonOrJettonWalletAddress(), true);
        walletInfoLayoutView.setBalance(getCurrentTonOrJettonWalletBalance().toString(), true);
        updateCurrencyPriceView();
    }

    @SuppressLint("DefaultLocale")
    private void updateCurrencyPriceView () {
        String currency = Settings.instance().optionShowedCurrency.get();
        if (currentTonWalletAccountState == null) {
            walletInfoLayoutView.setCurrencyPriceText(null);
        } else {
            double price = CoingeckoApi.getPrice(currency.toLowerCase()) * (((double) currentTonWalletAccountState.balance) / 1000000000f);
            if (currency.equals("USD")) {
                walletInfoLayoutView.setCurrencyPriceText(String.format("≈ $%,.2f", Math.abs(price)));
            } else if (currency.equals("EUR")) {
                walletInfoLayoutView.setCurrencyPriceText(String.format("≈ %,.2f€", Math.abs(price)));
            } else {
                walletInfoLayoutView.setCurrencyPriceText(String.format("≈ %f %s", Math.abs(price), currency));
            }
        }
    }

    String actionBarText;

    private void updateActionBarStatus (boolean animated) {
        String titleToSet = "";
        int titleIdToSet = 0;

        if (!hasNetworkConnection) {
            titleToSet = LocaleController.getString("WalletWaitingForNetwork", titleIdToSet = R.string.WalletWaitingForNetwork);
        } else if (lastUpdateTime == 0) {
            int progress = getTonController().getSyncProgress();
            if (progress != 0 && progress != 100) {
                titleToSet = LocaleController.formatString("WalletUpdatingProgress", R.string.WalletUpdatingProgress, progress);
            } else {
                titleToSet = LocaleController.getString("WalletUpdating", titleIdToSet = R.string.WalletUpdating);
            }
        }


        walletInfoLayoutView.setStatusBarVisible(!TextUtils.isEmpty(titleToSet), animated);
        if (TextUtils.equals(actionBarText, titleToSet)) return;
        actionBarText = titleToSet;
        actionBar.setTitleAnimated(titleToSet, true, 220L);
    }

    private void checkWalletStatusCellsVisible (boolean animated) {
        boolean isEmptyAndLoaded = getTonAccountStateManager().isLoadedAccountWithEmptyTransactions(getCurrentTonOrJettonWalletAddress());
        boolean isEmpty = adapter.isEmpty();
        if (walletInfoLayoutView != null) {
            walletInfoLayoutView.setWalletStatusCells(isEmptyAndLoaded, isEmpty && !isEmptyAndLoaded, animated);
        }
    }



    /* Buttons listener */

    protected void onReceivePressed () {
        if (getParentActivity() == null || currentTonWalletAddress == null) {
            return;
        }
        new ReceiveTonSheet(WalletActivity.this, currentTonWalletAddress).show();
    }

    protected void onSendPressed () {
        if (!getTonAccountStateManager().getPendingTransactions(currentTonWalletAddress).isEmpty()) {
            AlertsCreator.showSimpleAlert(WalletActivity.this, LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("WalletPendingWait", R.string.WalletPendingWait));
            return;
        }
        int syncProgress = getTonController().getSyncProgress();
        if (syncProgress != 0 && syncProgress != 100) {
            AlertsCreator.showSimpleAlert(WalletActivity.this, LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("WalletSendSyncInProgress", R.string.WalletSendSyncInProgress));
            return;
        }
        openTransfer(null);
    }





    /* Ton Connect Requests */

    public void onTonConnectRequest (Request request) {
        if (request instanceof RequestSendTransaction) {
            onTonConnectRequestSend((RequestSendTransaction) request);

        }
    }

    public void onTonConnectRequestSend (RequestSendTransaction request) {
        if (request.network != null && !TextUtils.equals(request.network, getTonController().getNetworkId())) {
            getTonConnectController().sendErrorToApplication(request, 300, "Wrong network id", null);
            return; // ignore ???
        }

        if (currentJettonRootContract != null || request.from != null && !TextUtils.equals(request.from, currentTonWalletAddress)) {
            TonConnectSwitchAddressSheet s = new TonConnectSwitchAddressSheet(this, request, currentJettonRootContract, currentJettonWalletContract);
            s.setOnSwitchDelegate(() -> {
                if (currentTonWalletAccountState == null || currentTonWalletAccountState.balance < request.totalAmount) {
                    undoView.showWithAction(UndoView.ACTION_INSUFFICIENT_FUNDS);
                    getTonConnectController().sendErrorToApplication(request, 300, "Insufficient funds", null);
                    return;
                }
                onTonConnectRequestSend(request);
            });
            showDialog(s, s::onDismiss);
            return;
        }

        TonConnectSendSheet s = new TonConnectSendSheet(this, request, currentTonWalletAccountBalance);
        showDialog(s);
    }



    /* Qr Scan */

    private void onQrCodeScanSuccess (String result) {
        UriParser.Result parsed = UriParser.parse(result);
        if (parsed != null) {
            onParseQrCode(parsed);
        } else {
            AlertsCreator.createSimpleAlert(getParentActivity(), LocaleController.getString("WalletQRCode", R.string.WalletQRCode), LocaleController.getString("WalletScanImageNotFound", R.string.WalletScanImageNotFound)).show();
        }
    }

    public void onParseQrCode (UriParser.Result result) {
        if (result instanceof UriParser.ResultTonDomain) {
            openTransfer(result);
        } else if (result instanceof UriParser.ResultTonLink) {
            openTransfer(result);
        } else if (result instanceof UriParser.ResultTonConnectLink) {
            TonConnectInitialSheet s = new TonConnectInitialSheet(this);
            getTonConnectController().initNewConnect((UriParser.ResultTonConnectLink) result, s::setLoadedApplication, this::defaultErrorCallback);
            showDialog(s);
        }
    }



    /* Life cycle */

    @Override
    public boolean onFragmentCreate () {
        getNotificationCenter().addObserver(this, NotificationCenter.walletPendingTransactionsChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.walletSyncProgressChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.walletChangeWalletVersion);
        getNotificationCenter().addObserver(this, NotificationCenter.walletChangeRootJettonAddress);
        getNotificationCenter().addObserver(this, NotificationCenter.coingeckoPricesUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.hasNetworkConnectionUpdated);
        getTonConnectController().setRequestCallback(this::onTonConnectRequest);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy () {
        super.onFragmentDestroy();
        getNotificationCenter().removeObserver(this, NotificationCenter.walletPendingTransactionsChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.walletSyncProgressChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.walletChangeWalletVersion);
        getNotificationCenter().removeObserver(this, NotificationCenter.walletChangeRootJettonAddress);
        getNotificationCenter().removeObserver(this, NotificationCenter.coingeckoPricesUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.hasNetworkConnectionUpdated);
        getTonConnectController().setRequestCallback(null);
    }

    @Override
    public void didReceivedNotification (int id, int account, Object... args) {
        if (id == NotificationCenter.walletPendingTransactionsChanged) {
            fillTransactions(null, false, true);
            updateBalanceAndAddressView();
        } else if (id == NotificationCenter.walletSyncProgressChanged) {
            updateActionBarStatus(true);
        } else if (id == NotificationCenter.walletChangeWalletVersion) {
            setTonWalletAddress(getTonController().getCurrentWalletAddress(), true, false);
        } else if (id == NotificationCenter.walletChangeRootJettonAddress) {
            String address = getTonController().getCurrentJettonRootAddress();
            setJettonRootContract(address != null ? getTonAccountStateManager().getJettonContractsStorage().get(address) : null, true);
        } else if (id == NotificationCenter.coingeckoPricesUpdated) {
            updateCurrencyPriceView();
        } else if (id == NotificationCenter.hasNetworkConnectionUpdated) {
            hasNetworkConnection = (boolean) args[0];
            updateActionBarStatus(true);
        }
    }

    @Override
    protected void onBecomeFullyVisible () {
        super.onBecomeFullyVisible();
        getTonController().isKeyStoreInvalidated((invalidated) -> {
            if (invalidated && getParentActivity() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Wallet", R.string.Wallet));
                builder.setMessage(LocaleController.getString("WalletKeystoreInvalidated", R.string.WalletKeystoreInvalidated));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                builder.setOnDismissListener(dialog -> {
                    getTonController().cleanup();
                    presentFragment(new WalletCreateStartActivity(), true);
                });
                builder.show();
            }
        });
    }

    @Override
    public void onResume () {
        super.onResume();
        updateBalanceAndAddressView();
    }

    @Override
    public boolean onBackPressed () {
        if (tokenSelectorPopupWindow != null) {
            tokenSelectorPopupWindow.dismiss();
            return false;
        }
        return super.onBackPressed();
    }

    @Override
    protected ActionBar createActionBar (Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackground));
        actionBar.setTitleColor(Theme.getColor(Theme.key_wallet_whiteText));
        actionBar.setItemsColor(Theme.getColor(Theme.key_wallet_whiteText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackgroundSelector), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick (int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == menu_settings) {
                    presentFragment(new SettingsMainActivity(WalletActivity.this));
                } else if (id == menu_qr_code) {
                    openQrCodeReader(WalletActivity.this::onQrCodeScanSuccess);
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(menu_qr_code, R.drawable.baseline_qr_scan_24);
        menu.addItem(menu_settings, R.drawable.notifications_settings);

        return actionBar;
    }

    @Override
    public boolean dismissDialogOnPause (Dialog dialog) {
        boolean needSafe =
            (dialog instanceof TonConnectSwitchAddressSheet) ||
                (dialog instanceof TonConnectSendSheet) ||
                (dialog instanceof TonConnectInfoSheet) ||
                (dialog instanceof TonConnectInitialSheet) ||
                (dialog instanceof TransactionInfoSheet) ||
                (dialog instanceof ReceiveTonSheet) ||
                (dialog instanceof SendTonSheet);

        return !needSafe && super.dismissDialogOnPause(dialog);
    }



    @Override
    public void onFactorChanged (int id, float factor, float fraction, FactorAnimator callee) {
        if (id == WALLET_PRESENT_ANIMATOR_ID) {
            checkWalletPresentVisibility();
        }
    }

    @Override
    public void onFactorChangeFinished (int id, float finalFactor, FactorAnimator callee) {
        if (id == WALLET_PRESENT_ANIMATOR_ID) {
            if (walletPresentView != null && !walletPresentVisibility.getValue()) {
                ((ViewGroup)fragmentView).removeView(walletPresentView);
            }
        }
    }

    private void checkWalletPresentVisibility () {
        final float factor = customContainerView.interpolator.getInterpolation(walletPresentVisibility.getFloatValue());

        listView.setScaleX(MathUtils.fromTo(1, 0.75f, factor));
        listView.setScaleY(MathUtils.fromTo(1, 0.75f, factor));
        listView.setAlpha(MathUtils.clamp(1f - factor));
        actionBar.setAlpha(MathUtils.clamp(1f - factor));
        walletInfoLayoutView.setPivotX(AndroidUtilities.dp(80));
        walletInfoLayoutView.setScaleX(listView.getScaleX());
        walletInfoLayoutView.setScaleY(listView.getScaleY());
        walletInfoLayoutView.setAlpha(MathUtils.clamp(1f - factor));
        customContainerView.setWalletPresentVisibilityFactor(walletPresentVisibility.getFloatValue());
        listView.invalidate();

        if (walletPresentView == null) return;
        walletPresentView.setAlpha(MathUtils.clamp(factor));
        walletPresentView.inner.setTranslationY(walletPresentView.inner.getMeasuredHeight() * (1f - factor) / 2.2f);
    }



    /*  */

    public class PullLinearLayoutManager extends LinearLayoutManager {
        public PullLinearLayoutManager (Context context) {
            super(context, LinearLayoutManager.VERTICAL, false);
        }

        @Override
        public boolean supportsPredictiveItemAnimations () {
            return false;
        }

        @Override
        public int scrollVerticallyBy (int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
            boolean isDragging = listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING;
            PullForegroundDrawable pullForegroundDrawable = listView.getPullForegroundDrawable();

            int measuredDy = dy;
            if (dy < 0) {
                listView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                int currentPosition = findFirstVisibleItemPosition();
                if (currentPosition == 0) {
                    View view = findViewByPosition(currentPosition);
                    if (view != null && view.getBottom() <= AndroidUtilities.dp(1)) {
                        currentPosition = 1;
                    }
                }
                if (!isDragging) {
                    View view = findViewByPosition(currentPosition);
                    int dialogHeight = AndroidUtilities.dp(72) + 1;
                    int canScrollDy = -view.getTop() + (currentPosition - 1) * dialogHeight;
                    int positiveDy = Math.abs(dy);
                    if (canScrollDy < positiveDy) {
                        measuredDy = -canScrollDy;
                    }
                } else if (currentPosition == 0) {
                    View v = findViewByPosition(currentPosition);
                    float k = 1f + (v.getTop() / (float) v.getMeasuredHeight());
                    if (k > 1f) {
                        k = 1f;
                    }
                    listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                    measuredDy *= PullForegroundDrawable.startPullParallax - PullForegroundDrawable.endPullParallax * k;
                    if (measuredDy > -1) {
                        measuredDy = -1;
                    }
                }
            }

            float viewOffset = listView.getViewOffset();
            if (viewOffset != 0 && dy > 0 && isDragging) {
                float ty = (int) viewOffset;
                ty -= dy;
                if (ty < 0) {
                    measuredDy = (int) ty;
                    ty = 0;
                } else {
                    measuredDy = 0;
                }
                listView.setViewsOffset(ty);
            }

            int usedDy = super.scrollVerticallyBy(measuredDy, recycler, state);
            if (pullForegroundDrawable != null) {
                pullForegroundDrawable.scrollDy = usedDy;
            }
            int currentPosition = findFirstVisibleItemPosition();
            View firstView = null;
            if (currentPosition == 0) {
                firstView = findViewByPosition(currentPosition);
            }
            if (currentPosition == 0 && firstView != null && firstView.getBottom() >= AndroidUtilities.dp(4)) {
                if (startArchivePullingTime == 0) {
                    startArchivePullingTime = System.currentTimeMillis();
                }
                if (pullForegroundDrawable != null) {
                    pullForegroundDrawable.showHidden();
                }
                float k = 1f + (firstView.getTop() / (float) firstView.getMeasuredHeight());
                if (k > 1f) {
                    k = 1f;
                }
                long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                boolean canShowInternal = k > PullForegroundDrawable.SNAP_HEIGHT && pullingTime > PullForegroundDrawable.minPullingTime + 20;
                if (canShowHiddenPull != canShowInternal) {
                    canShowHiddenPull = canShowInternal;
                    if (!wasPulled) {
                        listView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        if (pullForegroundDrawable != null) {
                            pullForegroundDrawable.colorize(canShowInternal);
                        }
                    }
                }
                if (measuredDy - usedDy != 0 && dy < 0 && isDragging) {
                    float ty;
                    float tk = (viewOffset / PullForegroundDrawable.getMaxOverscroll());
                    tk = 1f - tk;
                    ty = (viewOffset - dy * PullForegroundDrawable.startPullOverScroll * tk);
                    listView.setViewsOffset(ty);
                }
                if (pullForegroundDrawable != null) {
                    pullForegroundDrawable.pullProgress = k;
                    pullForegroundDrawable.setListView(listView);
                }
            } else {
                startArchivePullingTime = 0;
                canShowHiddenPull = false;
                if (pullForegroundDrawable != null) {
                    pullForegroundDrawable.resetText();
                    pullForegroundDrawable.pullProgress = 0f;
                    pullForegroundDrawable.setListView(listView);
                }
            }
            if (firstView != null) {
                firstView.invalidate();
            }
            return usedDy;
        }
    }

    public class PullRecyclerView extends RecyclerListView {
        private final PullForegroundDrawable pullForegroundDrawable;

        private float viewOffset = 0.0f;
        private boolean firstLayout = true;
        private boolean ignoreLayout;

        public PullRecyclerView (Context context) {
            super(context);

            pullForegroundDrawable = new PullForegroundDrawable(LocaleController.getString("WalletSwipeToRefresh", R.string.WalletSwipeToRefresh), LocaleController.getString("WalletReleaseToRefresh", R.string.WalletReleaseToRefresh)) {
                @Override
                protected float getViewOffset () {
                    return listView.getViewOffset();
                }
            };
            pullForegroundDrawable.setColors(Theme.key_wallet_pullBackground, Theme.key_wallet_releaseBackground);
            pullForegroundDrawable.showHidden();
            pullForegroundDrawable.setWillDraw(true);
        }

        public void setViewsOffset (float offset) {
            viewOffset = offset;
            int n = getChildCount();
            for (int i = 0; i < n; i++) {
                getChildAt(i).setTranslationY(viewOffset);
            }
            invalidate();
            updateScrollPaddings();
            fragmentView.invalidate();
        }

        public float getViewOffset () {
            return viewOffset;
        }

        public PullForegroundDrawable getPullForegroundDrawable () {
            return pullForegroundDrawable;
        }

        @Override
        protected void onMeasure (int widthSpec, int heightSpec) {
            if (firstLayout) {
                ignoreLayout = true;
                layoutManager.scrollToPositionWithOffset(1, 0);
                ignoreLayout = false;
                firstLayout = false;
            }
            super.onMeasure(widthSpec, heightSpec);
            invalidateItemDecorations();
        }

        @Override
        public void requestLayout () {
            if (ignoreLayout) {
                return;
            }
            super.requestLayout();
        }

        @Override
        public void setAdapter (RecyclerView.Adapter adapter) {
            super.setAdapter(adapter);
            firstLayout = true;
        }

        @Override
        public void addView (View child, int index, ViewGroup.LayoutParams params) {
            super.addView(child, index, params);
            child.setTranslationY(viewOffset);
        }

        @Override
        public void removeView (View view) {
            super.removeView(view);
            view.setTranslationY(0);
        }

        @Override
        public void onDraw (Canvas canvas) {
            if (pullForegroundDrawable != null && viewOffset != 0) {
                pullForegroundDrawable.drawOverScroll(canvas);
            }
            super.onDraw(canvas);
        }

        @Override
        public boolean onTouchEvent (MotionEvent e) {
            int action = e.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                setOverScrollMode(View.OVER_SCROLL_ALWAYS);
                if (wasPulled) {
                    wasPulled = false;
                    if (pullForegroundDrawable != null) {
                        pullForegroundDrawable.doNotShow();
                    }
                    canShowHiddenPull = false;
                }
            }
            boolean result = super.onTouchEvent(e);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                int currentPosition = layoutManager.findFirstVisibleItemPosition();
                if (currentPosition == 0) {
                    View view = layoutManager.findViewByPosition(currentPosition);
                    int height = (int) (AndroidUtilities.dp(72) * PullForegroundDrawable.SNAP_HEIGHT);
                    int diff = (view != null ? view.getTop() + view.getMeasuredHeight() : 0);
                    if (view != null) {
                        long pullingTime = System.currentTimeMillis() - startArchivePullingTime;
                        smoothScrollBy(0, diff, CubicBezierInterpolator.EASE_OUT_QUINT);
                        if (diff >= height && pullingTime >= PullForegroundDrawable.minPullingTime) {
                            wasPulled = true;
                            lastUpdateTime = 0;
                            forceLoadAccountState();
                            updateActionBarStatus(true);
                        }

                        if (viewOffset != 0) {
                            ValueAnimator valueAnimator = ValueAnimator.ofFloat(viewOffset, 0f);
                            valueAnimator.addUpdateListener(animation -> setViewsOffset((float) animation.getAnimatedValue()));

                            valueAnimator.setDuration((long) (350f - 120f * (viewOffset / PullForegroundDrawable.getMaxOverscroll())));
                            valueAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                            setScrollEnabled(false);
                            valueAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd (Animator animation) {
                                    super.onAnimationEnd(animation);
                                    setScrollEnabled(true);
                                }
                            });
                            valueAnimator.start();
                        }
                    }
                }
            }
            return result;
        }

        private final RectF clipRect = new RectF();
        private final Path clipPath = new Path();
        private final float[] radii = new float[8];

        @Override
        protected void onLayout (boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            radii[0] = AndroidUtilities.dp(12);
            radii[1] = AndroidUtilities.dp(12);
            radii[2] = AndroidUtilities.dp(12);
            radii[3] = AndroidUtilities.dp(12);
            updateScrollPaddings();
        }

        @Override
        protected void dispatchDraw (Canvas canvas) {
            int bottom = (int) getBalanceCellBottom();
            clipRect.set(0, bottom, getMeasuredWidth(), getMeasuredHeight());
            clipPath.reset();
            clipPath.addRoundRect(clipRect, radii, Path.Direction.CW);
            clipPath.close();
            super.dispatchDraw(canvas);
        }

        @Override
        public boolean drawChild (Canvas canvas, View child, long drawingTime) {
            float presentValue = walletPresentVisibility.getFloatValue();
            if (presentValue > 0 && (child instanceof WalletActivityButtonsCell)) {
                int bottom = (int) customContainerView.getInterpolatedBottom();
                bottom -= ActionBar.getCurrentActionBarHeight();
                bottom -= AndroidUtilities.statusBarHeight;
                canvas.save();
                canvas.clipRect(0, 0, getMeasuredWidth(), bottom);
                boolean r = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
                return r;
            } else if (child instanceof WalletTransactionCell || child instanceof WalletDateCell) {
                canvas.save();
                canvas.clipPath(clipPath);
                boolean r = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
                return r;
            }
            return super.drawChild(canvas, child, drawingTime);
        }

        @Nullable
        @Override
        public Path getClipHeaderPath () {
            return clipPath;
        }
    }

    public class WalletPresentView extends FrameLayout {
        private final View inner;

        public WalletPresentView (@NonNull Context context) {
            this(context, WalletCreateReadyActivity.TYPE_READY_CREATE);
        }

        public WalletPresentView (@NonNull Context context, int type) {
            super(context);

            BaseFragment p = new WalletCreateReadyActivity(type) {
                @Override
                protected void onMainButtonClick (View v) {
                    walletPresentVisibility.setValue(false, true);
                }
            };
            p.setParentFragment(WalletActivity.this);
            inner = p.getFragmentView();

            addView(inner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
            inner.setBackgroundColor(0);
            setBackgroundColor(0);
        }
    }
}
