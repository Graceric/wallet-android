package org.UI.Fragments.Settings;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.Data.Jettons.RootJettonContract;
import org.UI.Sheets.Jetton.TokenInfoSheet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsTextCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.UI.Cells.TripleTextWithIconCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.UI.Sheets.Jetton.TokenAddSheet;
import org.UI.Fragments.Templates.WalletBaseActivity;

import java.util.ArrayList;

public class SettingsTokensListActivity extends WalletBaseActivity {
    private static final int CELL_TYPE_EMPTY = 1;
    private static final int CELL_TYPE_HEADER = 2;
    private static final int CELL_TYPE_TOKEN = 3;
    private static final int CELL_TYPE_TOKEN_ADD = 4;

    private RecyclerListView listView;
    private ArrayList<RootJettonContract> tokensList;

    private int rowCount;

    private int tokensHeaderRow;
    private int tokensListStartRow;
    private int tokensEmptyRow;
    private int tokensAddButtonRow;

    public SettingsTokensListActivity () {
        super();
        updateList();
    }

    public void updateList () {
        ArrayList<String> tokens = getTonController().getJettonsCache().getTokens();
        tokensList = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            RootJettonContract contract = getTonController().getJettonsCache().get(token);
            if (contract != null) {
                tokensList.add(contract);
            }
        }
    }

    @Override
    public View createView (Context context) {
        FrameLayout frameLayout = createFragmentView(context);
        updateRows();

        actionBar.setTitle(LocaleController.getString("WalletListOfTokens", R.string.WalletListOfTokens));
        fragmentView = frameLayout;

        listView = createRecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setGlowColor(Theme.getColor(Theme.key_wallet_blackBackground));
        listView.setAdapter(new Adapter(context));
        listView.setOnItemClickListener((view, position) -> {
            if (position >= tokensListStartRow && position < tokensListStartRow + tokensList.size()) {
                RootJettonContract contract = tokensList.get(position - tokensListStartRow);
                TokenInfoSheet s = new TokenInfoSheet(this, contract, () -> {
                    updateList();
                    updateRows();
                    listView.getAdapter().notifyDataSetChanged();
                });
                showDialog(s);
            } else {
                position -= tokensList.size();
                if (position == tokensAddButtonRow) {
                    TokenAddSheet s = new TokenAddSheet(this, contract -> {
                        updateList();
                        updateRows();
                        listView.getAdapter().notifyDataSetChanged();
                    });
                    showDialog(s);
                }
            }
        });

        frameLayout.addView(listView);

        return fragmentView;
    }

    private void updateRows () {
        tokensEmptyRow = -1;

        rowCount = 0;
        tokensHeaderRow = rowCount++;
        tokensListStartRow = rowCount;
        if (!tokensList.isEmpty()) {
            tokensEmptyRow = rowCount++;
        }
        tokensAddButtonRow = rowCount++;
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public Adapter (Context c) {
            context = c;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case CELL_TYPE_HEADER: {
                    view = new HeaderCell(context);
                    break;
                }
                case CELL_TYPE_TOKEN: {
                    TripleTextWithIconCell cell = new TripleTextWithIconCell(context);
                    cell.setRadius(AndroidUtilities.dp(28));
                    view = cell;
                    break;
                }
                case CELL_TYPE_TOKEN_ADD: {
                    view = new SettingsTextCell(context);
                    break;
                }
                case CELL_TYPE_EMPTY:
                default: {
                    view = new ShadowSectionCell(context);
                    break;
                }
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case CELL_TYPE_EMPTY: {
                    Drawable drawable = Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                    CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                    combinedDrawable.setFullsize(true);
                    holder.itemView.setBackgroundDrawable(combinedDrawable);
                    break;
                }
                case CELL_TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == tokensHeaderRow) {
                        cell.setText(LocaleController.getString("WalletTokens", R.string.WalletTokens));
                    }
                    break;
                }
                case CELL_TYPE_TOKEN: {
                    TripleTextWithIconCell cell = (TripleTextWithIconCell) holder.itemView;
                    int index = position - tokensListStartRow;
                    cell.setJetton(tokensList.get(index), true);
                    break;
                }
                case CELL_TYPE_TOKEN_ADD: {
                    SettingsTextCell cell = (SettingsTextCell) holder.itemView;
                    cell.setText("Add", false);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType (int position) {
            if (position == tokensHeaderRow) {
                return CELL_TYPE_HEADER;
            } else if (position < tokensListStartRow + tokensList.size()) {
                return CELL_TYPE_TOKEN;
            }
            position -= tokensList.size();
            if (position == tokensEmptyRow) {
                return CELL_TYPE_EMPTY;
            } else if (position == tokensAddButtonRow) {
                return CELL_TYPE_TOKEN_ADD;
            }
            return -1;
        }

        @Override
        public boolean isEnabled (RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == CELL_TYPE_TOKEN || type == CELL_TYPE_TOKEN_ADD;
        }

        @Override
        public int getItemCount () {
            return rowCount + tokensList.size();
        }
    }
}
