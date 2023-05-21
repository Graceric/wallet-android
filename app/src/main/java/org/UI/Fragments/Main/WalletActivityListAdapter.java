package org.UI.Fragments.Main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.Data.Jettons.RootJettonContract;
import org.TonController.Data.WalletTransaction;
import org.TonController.Data.WalletTransactionMessage;
import org.TonController.Parsers.PayloadParser;
import org.UI.Cells.WalletDateCell;
import org.UI.Cells.WalletTransactionCell;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Cells.WalletActivityButtonsCell;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class WalletActivityListAdapter extends RecyclerListView.SectionsAdapter {
    public final static String PENDING_KEY = "pending";

    private static final int CELL_TYPE_WALLET_BALANCE = 0;
    private static final int CELL_TYPE_WALLET_TRANSACTION = 1;
    private static final int CELL_TYPE_WALLET_TRANSACTION_LOADING = 2;
    private static final int CELL_TYPE_WALLET_TRANSACTION_DATE = 3;
    private static final int CELL_TYPE_WALLET_PULL_FOREGROUND = 4;

    public static final int TRANSACTIONS_MODE_DEFAULT = 0;
    public static final int TRANSACTIONS_MODE_JETTON = 1;

    private final Context context;
    private Runnable onReceiveButtonPressed;
    private Runnable onSendButtonPressed;
    private PullForegroundDrawable pullForegroundDrawable;
    private Drawable backgroundDrawable;

    private final HashSet<Long> transactionsDict = new HashSet<>();
    private final HashMap<String, ArrayList<WalletTransactionMessage>> sectionMessages = new HashMap<>();
    private final ArrayList<String> sections = new ArrayList<>();
    private int mode = TRANSACTIONS_MODE_DEFAULT;
    private boolean transactionsEndReached;
    private boolean showTransactionsLoading;
    private @Nullable RootJettonContract rootJettonContract;

    public WalletActivityListAdapter (Context context) {
        this.context = context;
    }

    public void setButtonsListeners (Runnable onReceiveButtonPressed, Runnable onSendButtonPressed) {
        this.onReceiveButtonPressed = onReceiveButtonPressed;
        this.onSendButtonPressed = onSendButtonPressed;
    }

    public void setDrawables (PullForegroundDrawable pullForegroundDrawable, Drawable backgroundDrawable) {
        this.pullForegroundDrawable = pullForegroundDrawable;
        this.backgroundDrawable = backgroundDrawable;
    }

    public void setRootJettonContract (@Nullable RootJettonContract rootJettonContract) {
        this.rootJettonContract = rootJettonContract;
    }

    public void setMode (int mode) {
        this.mode = mode;
    }

    @Override
    public boolean isEnabled (int section, int row) {
        return section != 0 && row != 0;
    }

    @Override
    public Object getItem (int section, int position) {
        return null;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case CELL_TYPE_WALLET_BALANCE: {
                WalletActivityButtonsCell cell = new WalletActivityButtonsCell(context);
                cell.setListeners(onReceiveButtonPressed, onSendButtonPressed);
                view = cell;
                break;
            }
            case CELL_TYPE_WALLET_TRANSACTION: {
                view = new WalletTransactionCell(context);
                break;
            }
            case CELL_TYPE_WALLET_TRANSACTION_DATE: {
                view = new WalletDateCell(context);
                break;
            }
            case CELL_TYPE_WALLET_PULL_FOREGROUND: {
                view = new View(context) {
                    @Override
                    protected void onDraw (Canvas canvas) {
                        pullForegroundDrawable.draw(canvas);
                    }

                    @Override
                    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(AndroidUtilities.dp(72)), MeasureSpec.EXACTLY));
                    }
                };
                pullForegroundDrawable.setCell(view);
                break;
            }
            case CELL_TYPE_WALLET_TRANSACTION_LOADING: {
                RadialProgressView progressView = new RadialProgressView(context) {
                    @Override
                    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(
                            MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY));
                    }
                };
                progressView.setSize(AndroidUtilities.dp(32));
                progressView.setStrokeWidth(4f);
                progressView.setProgressColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));
                view = progressView;
                break;
            }
            default: {
                view = new View(context);
                break;
            }
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder (int section, int position, RecyclerView.ViewHolder holder) {
        switch (holder.getItemViewType()) {
            case CELL_TYPE_WALLET_TRANSACTION: {
                WalletTransactionCell transactionCell = (WalletTransactionCell) holder.itemView;
                section -= 1;
                String key = sections.get(section);
                ArrayList<WalletTransactionMessage> arrayList = sectionMessages.get(key);
                transactionCell.setRootJettonContract(rootJettonContract);
                transactionCell.setTransaction(arrayList.get(position - 1), mode, position != arrayList.size() || (section + 1 != sectionMessages.size()));
                transactionCell.setNeedTopPadding(position > 1);
                break;
            }
            case CELL_TYPE_WALLET_TRANSACTION_DATE: {
                WalletDateCell dateCell = (WalletDateCell) holder.itemView;
                section -= 1;
                String key = sections.get(section);
                if (PENDING_KEY.equals(key)) {
                    dateCell.setText(LocaleController.getString("WalletPendingTransactions", R.string.WalletPendingTransactions));
                } else {
                    ArrayList<WalletTransactionMessage> arrayList = sectionMessages.get(key);
                    dateCell.setDate(arrayList.get(0).transaction.utime);
                }
                break;
            }
        }
    }

    @Override
    public int getItemViewType (int section, int position) {
        if (section == 0) {
            if (position == 0) {
                return CELL_TYPE_WALLET_PULL_FOREGROUND;
            } else {
                return CELL_TYPE_WALLET_BALANCE;
            }
        } else if (section - 1 < sections.size()) {
            return position == 0 ? CELL_TYPE_WALLET_TRANSACTION_DATE : CELL_TYPE_WALLET_TRANSACTION;
        } else {
            return CELL_TYPE_WALLET_TRANSACTION_LOADING;
        }
    }

    @Override
    public int getSectionCount () {
        return sections.size() + (showTransactionsLoading ? 2: 1);
    }

    @Override
    public int getCountForSection (int section) {
        if (section == 0) {
            return 2;
        } else if (section - 1 < sections.size()) {
            return sectionMessages.get(sections.get(section - 1)).size() + 1;
        } else {
            return 1;
        }
    }

    @Override
    public View getSectionHeaderView (int section, View view) {
        if (view == null) {
            view = new WalletDateCell(context) {
                @Override
                protected void onDraw (Canvas canvas) {
                    backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    backgroundDrawable.draw(canvas);
                    super.onDraw(canvas);
                }
            };
            view.setWillNotDraw(false);
        }
        WalletDateCell dateCell = (WalletDateCell) view;
        if (section == 0) {
            dateCell.setAlpha(0.0f);
        } else {
            section -= 1;
            if (section < sections.size()) {
                view.setAlpha(1.0f);
                String key = sections.get(section);
                if (PENDING_KEY.equals(key)) {
                    dateCell.setText(LocaleController.getString("WalletPendingTransactions", R.string.WalletPendingTransactions));
                } else {
                    ArrayList<WalletTransactionMessage> arrayList = sectionMessages.get(key);
                    dateCell.setDate(arrayList.get(0).transaction.utime);
                }
            }
        }
        return view;
    }

    private int getSectionStartIndex (String section) {
        return getSectionStartIndex(sections.indexOf(section));
    }

    private int getSectionStartIndex (int sectionIndex) {
        int result = 2;
        for (int a = 0; a < sectionIndex; a++) {
            result += sectionMessages.get(sections.get(a)).size() + 1;
        }
        return result;
    }

    public boolean fillTransactions (ArrayList<WalletTransaction> arrayList, ArrayList<WalletTransaction> pendingTransactions, boolean reload) {
        boolean cleared = sections.isEmpty();
        if (arrayList != null && !arrayList.isEmpty() && reload) {
            WalletTransaction transaction = arrayList.get(arrayList.size() - 1);
            for (int b = 0, N2 = sections.size(); b < N2; b++) {
                if (PENDING_KEY.equals(sections.get(b))) {
                    continue;
                }
                String key = sections.get(b);
                ArrayList<WalletTransactionMessage> existingTransactions = sectionMessages.get(key);
                if (existingTransactions.get(0).transaction.utime < transaction.rawTransaction.utime) {
                    clear();
                    cleared = true;
                } else {
                    Collections.reverse(arrayList);
                }
                break;
            }
        } else if (arrayList != null && arrayList.isEmpty() && reload) {
            clear();
            cleared = true;
        }

        ArrayList<WalletTransactionMessage> pendingMessages = WalletTransaction.from(pendingTransactions);
        if (pendingMessages.isEmpty()) {
            List<?> list = sectionMessages.remove(PENDING_KEY);
            if (list != null) {
                sections.remove(0);
                notifyItemRangeRemoved(2, list.size() + 1);
            }
        } else {
            if (!sectionMessages.containsKey(PENDING_KEY)) {
                sections.add(0, PENDING_KEY);
                sectionMessages.put(PENDING_KEY, pendingMessages);
                notifyItemRangeInserted(2, pendingMessages.size() + 1);
            }
        }
        if (arrayList == null) {
            setShowTransactionsLoading(!isEmpty() && !isTransactionsEndReached());
            return false;
        }

        boolean added = false;
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            WalletTransaction transaction = arrayList.get(a);
            if (transactionsDict.contains(transaction.rawTransaction.transactionId.lt)) continue;

            String dateKey = Utilities.getDateKey(transaction.rawTransaction.utime * 1000);
            ArrayList<WalletTransactionMessage> transactions = sectionMessages.get(dateKey);

            if (transactions == null) {
                int addToIndex = sections.size();
                for (int b = 0, N2 = sections.size(); b < N2; b++) {
                    if (PENDING_KEY.equals(sections.get(b))) continue;
                    String key = sections.get(b);
                    ArrayList<WalletTransactionMessage> existingTransactions = sectionMessages.get(key);
                    if (existingTransactions.get(0).transaction.utime < transaction.rawTransaction.utime) {
                        addToIndex = b;
                        break;
                    }
                }
                transactions = new ArrayList<>();
                sections.add(addToIndex, dateKey);
                sectionMessages.put(dateKey, transactions);
                notifyItemRangeInserted(getSectionStartIndex(addToIndex), 1);
            }
            added = true;
            int oldCount = transactions.size();

            final boolean isPending = transaction.isPending();
            final boolean isInternalTransferReceive = mode == TRANSACTIONS_MODE_JETTON && transaction.isInternalTokenReceive();
            final boolean isInternalTransferSend = mode == TRANSACTIONS_MODE_JETTON && transaction.isInternalTokenSend();
            final boolean isPendingJettonMode = mode == TRANSACTIONS_MODE_JETTON && isPending;

            if (reload && !cleared) {
                if (transaction.outMsgs.length > 0 && !isInternalTransferReceive) {
                    for (int c = transaction.outMsgs.length - 1; c >= 0; c--) {
                        if (!isInternalTransferSend || transaction.outMsgs[c].parsedPayload instanceof PayloadParser.ResultJettonInternalTransfer) {
                            transactions.add(0, transaction.outMsgs[c]);
                        }
                    }
                }
                if (transaction.inMsg != null && (!isInternalTransferSend || isPendingJettonMode) && (isInternalTransferReceive || transaction.outMsgs.length == 0 || mode == TRANSACTIONS_MODE_JETTON)) {
                    transactions.add(0, transaction.inMsg);
                }
                int startIndex = getSectionStartIndex(dateKey) + 1;
                notifyItemRangeInserted(startIndex, transactions.size() - oldCount);
                if (oldCount > 0) {
                    notifyItemChanged(startIndex + transactions.size() - oldCount);
                }
            } else {
                if (transaction.outMsgs.length > 0 && !isInternalTransferReceive) {
                    for (int c = transaction.outMsgs.length - 1; c >= 0; c--) {
                        if (!isInternalTransferSend || transaction.outMsgs[c].parsedPayload instanceof PayloadParser.ResultJettonInternalTransfer) {
                            transactions.add(transaction.outMsgs[c]);
                        }
                    }
                }
                if (transaction.inMsg != null && (!isInternalTransferSend || isPendingJettonMode) && (isInternalTransferReceive || transaction.outMsgs.length == 0 || mode == TRANSACTIONS_MODE_JETTON)) {
                    transactions.add(transaction.inMsg);
                }
                int startIndex = getSectionStartIndex(dateKey) + 1;
                notifyItemRangeInserted(startIndex + oldCount, transactions.size() - oldCount);
                if (oldCount > 0) {
                    notifyItemChanged(startIndex + transactions.size());
                }
            }
            transactionsDict.add(transaction.rawTransaction.transactionId.lt);
        }

        setShowTransactionsLoading(!isEmpty() && !isTransactionsEndReached());
        return added;
    }

    public void setTransactionsEndReached (boolean isEnd) {
        if (transactionsEndReached == isEnd) return;
        transactionsEndReached = isEnd;
        if (isEnd) {
            setShowTransactionsLoading(false);
        }
    }

    private void setShowTransactionsLoading (boolean show) {
        if (showTransactionsLoading == show) return;
        showTransactionsLoading = show;

        int itemCount = getItemCount();
        if (show) {
            notifyItemRangeRemoved(itemCount, 1);
        } else {
            notifyItemRangeInserted(itemCount, 1);
        }
    }

    public boolean isEmpty () {
        return sections.isEmpty();
    }

    public boolean isTransactionsEndReached () {
        return transactionsEndReached;
    }

    public void clear () {
        setShowTransactionsLoading(false);
        setTransactionsEndReached(false);
        int itemsSize = getItemCount();
        sections.clear();
        sectionMessages.clear();
        transactionsDict.clear();
        if (itemsSize > 2) {
            notifyItemRangeRemoved(2, itemsSize - 2);
        }
    }
}
