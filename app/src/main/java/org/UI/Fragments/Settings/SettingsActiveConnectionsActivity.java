package org.UI.Fragments.Settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.TonConnect.TonConnectController;
import org.UI.Sheets.TonConnect.TonConnectInfoSheet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.UI.Cells.TripleTextWithIconCell;
import org.telegram.ui.Components.RecyclerListView;
import org.UI.Fragments.Templates.WalletBaseActivity;

import java.util.ArrayList;

public class SettingsActiveConnectionsActivity extends WalletBaseActivity {
    private RecyclerListView listView;
    private Adapter adapter;
    private final ArrayList<TonConnectController.ConnectedApplication> connections;

    public SettingsActiveConnectionsActivity () {
        super();
        ArrayList<String> appClientsId = getTonConnectController().getConnectedClientsId();
        connections = new ArrayList<>(appClientsId.size());
        for (String clientId : appClientsId) {
            connections.add(getTonConnectController().getConnectedApplication(clientId));
        }
    }

    @Override
    public View createView (Context context) {
        FrameLayout frameLayout = createFragmentView(context);

        actionBar.setTitle(LocaleController.getString("TonConnectActiveConnections", R.string.TonConnectActiveConnections));
        fragmentView = frameLayout;

        listView = createRecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setGlowColor(Theme.getColor(Theme.key_wallet_blackBackground));
        listView.setAdapter(adapter = new Adapter(context));
        listView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        listView.setClipToPadding(false);
        listView.setOnItemClickListener((view, position) -> showDialog(new TonConnectInfoSheet(this, connections.get(position), () -> {
            connections.remove(position);
            if (!listView.isComputingLayout()) {
                adapter.notifyDataSetChanged();
            }
            listView.invalidate();
        })));

        frameLayout.addView(listView);

        return fragmentView;
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public Adapter (Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = new TripleTextWithIconCell(context);
            } else {
                view = new View(context);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                TripleTextWithIconCell cell = (TripleTextWithIconCell) holder.itemView;
                cell.setApp(connections.get(position), position != getItemCount() - 1);
            }
        }

        @Override
        public int getItemViewType (int position) {
            return 0;
        }

        @Override
        public boolean isEnabled (RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount () {
            return connections.size();
        }
    }
}
