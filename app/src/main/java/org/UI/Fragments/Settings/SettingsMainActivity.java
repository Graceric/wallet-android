package org.UI.Fragments.Settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.TonController.Data.Wallets;
import org.TonController.TonController;
import org.UI.Fragments.Templates.WalletBaseActivity;
import org.UI.Fragments.Create.WalletCreateSeedShowActivity;
import org.UI.Fragments.Create.WalletCreateSetPasscodeActivity;
import org.UI.Fragments.Create.WalletCreateStartActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsSwitchCell;
import org.telegram.ui.Cells.SettingsTextCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BiometricPromtHelper;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.Utils.Settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SettingsMainActivity extends WalletBaseActivity {

    private Adapter adapter;
    private RecyclerListView listView;
    private final BaseFragment parentFragment;

    private int generalRow;
    private int notificationsRow;
    private int walletTypeRow;
    private int currencyRow;
    private int listOfTokensRow;

    private int tonConnectHeaderRow;
    private int tonConnectActiveConnectionsRow;
    private int tonConnectEmptyRow;

    private int headerRow;
    private int exportRow;
    private int serverSettingsRow;
    private int changePasscodeRow;
    private int biometricAuthRow;
    private int walletSectionRow;
    private int deleteRow;
    private int sendLogsRow;
    private int clearLogsRow;
    private int rowCount;

    public SettingsMainActivity (BaseFragment parent) {
        super();

        parentFragment = parent;

        updateRows();
    }

    private void updateRows () {
        rowCount = 0;

        changePasscodeRow = -1;
        biometricAuthRow = -1;
        deleteRow = -1;
        serverSettingsRow = -1;
        sendLogsRow = -1;
        clearLogsRow = -1;
        tonConnectHeaderRow = -1;
        tonConnectActiveConnectionsRow = -1;
        tonConnectEmptyRow = -1;

        generalRow = rowCount++;
        notificationsRow = rowCount++;
        walletTypeRow = rowCount++;
        currencyRow = rowCount++;
        listOfTokensRow = rowCount++;
        walletSectionRow = rowCount++;

        if (getTonConnectController().getConnectedClientsId().size() > 0) {
            tonConnectHeaderRow = rowCount++;
            tonConnectActiveConnectionsRow = rowCount++;
            tonConnectEmptyRow = rowCount++;
        }

        headerRow = rowCount++;
        if (BuildVars.DEBUG_VERSION) {
            clearLogsRow = rowCount++;
            sendLogsRow = rowCount++;
        }
        exportRow = rowCount++;
        if (BuildVars.DEBUG_VERSION) {
            serverSettingsRow = rowCount++;
        }
        changePasscodeRow = rowCount++;
        if (BiometricPromtHelper.canAddBiometric()) {
            biometricAuthRow = rowCount++;
        }
        deleteRow = rowCount++;
    }

    @Override
    public View createView (Context context) {
        actionBar.setTitle(LocaleController.getString("WalletSettings", R.string.WalletSettings));

        FrameLayout frameLayout = createFragmentView(context);

        fragmentView = frameLayout;

        listView = createRecyclerView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(adapter = new Adapter(context));
        listView.setGlowColor(Theme.getColor(Theme.key_wallet_blackBackground));

        frameLayout.addView(listView);
        listView.setOnItemClickListener((view, position) -> {
            if (position == exportRow) {
                requestPasscodeOrBiometricAuth(LocaleController.getString("WalletExportConfirmCredentials", R.string.WalletExportConfirmCredentials), this::doExport);
            } else if (position == clearLogsRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle("Clear Logs");
                builder.setMessage("Are you sure you want to clear logs?");
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.setPositiveButton("Clear", (dialog, which) -> FileLog.cleanupLogs());
                showDialog(builder.create());
            } else if (position == sendLogsRow) {
                sendLogs();
            } else if (position == deleteRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("WalletDeleteTitle", R.string.WalletDeleteTitle));
                builder.setMessage(LocaleController.getString("WalletDeleteInfo", R.string.WalletDeleteInfo));
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                    doLogout();
                    if (parentFragment != null) {
                        parentFragment.removeSelfFromStack();
                    }
                    presentFragment(new WalletCreateStartActivity(), true);
                });
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
            } else if (position == changePasscodeRow) {
                requestPasscodeActivity(passcode -> getTonController().prepareForPasscodeChange(passcode, () -> {
                    WalletCreateSetPasscodeActivity fragment = new WalletCreateSetPasscodeActivity();
                    fragment.setChangingPasscode();
                    presentFragment(fragment, true);
                }, this::defaultErrorCallback), false);
            } else if (position == listOfTokensRow) {
                presentFragment(new SettingsTokensListActivity());
            } else if (position == walletTypeRow) {
                showDialog(AlertsCreator.createWalletTypeSelectDialog(getParentActivity(), currentAccount, () -> adapter.notifyItemChanged(walletTypeRow)));
            } else if (position == notificationsRow) {
                getTonController().setNotificationsEnabled(!getTonController().isNotificationsEnabled());
                SettingsSwitchCell cell = (SettingsSwitchCell) view;
                cell.setChecked(!cell.isChecked());
            } else if (position == currencyRow) {
                showDialog(AlertsCreator.createCurrencySelectDialog(getParentActivity(), () -> adapter.notifyItemChanged(currencyRow)));
            } else if (position == biometricAuthRow) {
                if (getTonController().getKeyProtectionType() == TonController.KEY_PROTECTION_TYPE_NONE) {
                    if (BiometricPromtHelper.askForBiometricIfNoBiometricEnrolled(this)) {
                        requestPasscodeActivity(passcode -> getTonController().setupBiometricAuth(passcode, () -> adapter.notifyItemChanged(biometricAuthRow), this::defaultErrorCallback), true);
                    }
                } else {
                    requestPasscodeActivity(passcode -> getBiometricPromtHelper().promtWithCipher(getTonController().getCipherForDecrypt(), LocaleController.getString("WalletExportConfirmCredentials", R.string.WalletExportConfirmCredentials), cipher -> getTonController().disableBiometricAuth(cipher, passcode, () -> adapter.notifyItemChanged(biometricAuthRow), this::defaultErrorCallback)), true);
                }
            } else if (position == tonConnectActiveConnectionsRow) {
                presentFragment(new SettingsActiveConnectionsActivity());
            }
        });

        return fragmentView;
    }


    @Override
    public void onResume () {
        super.onResume();
        updateRows();
        if (adapter != null && !listView.isComputingLayout()) {
            if (biometricAuthRow >= 0) {
                adapter.notifyItemChanged(biometricAuthRow);
            }
        }
    }

    @Override
    public void removeSelfFromStack () {
        super.removeSelfFromStack();
        if (parentFragment != null) {
            parentFragment.removeSelfFromStack();
        }
    }

    private void sendLogs () {
        if (getParentActivity() == null) {
            return;
        }
        showProgressDialog();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
                File dir = new File(sdCard.getAbsolutePath() + "/logs");

                File zipFile = new File(dir, "wallet_logs.zip");
                if (zipFile.exists()) {
                    zipFile.delete();
                }

                File[] files = dir.listFiles();

                boolean[] finished = new boolean[1];

                BufferedInputStream origin = null;
                ZipOutputStream out = null;
                try {
                    FileOutputStream dest = new FileOutputStream(zipFile);
                    out = new ZipOutputStream(new BufferedOutputStream(dest));
                    byte[] data = new byte[1024 * 64];

                    for (File file : files) {
                        FileInputStream fi = new FileInputStream(file);
                        origin = new BufferedInputStream(fi, data.length);

                        ZipEntry entry = new ZipEntry(file.getName());
                        out.putNextEntry(entry);
                        int count;
                        while ((count = origin.read(data, 0, data.length)) != -1) {
                            out.write(data, 0, count);
                        }
                        origin.close();
                        origin = null;
                    }
                    finished[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (origin != null) {
                        origin.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    hideProgressDialog();
                    if (finished[0]) {
                        Uri uri;
                        if (Build.VERSION.SDK_INT >= 24) {
                            uri = FileProvider.getUriForFile(getParentActivity(), BuildConfig.APPLICATION_ID + ".provider", zipFile);
                        } else {
                            uri = Uri.fromFile(zipFile);
                        }

                        Intent i = new Intent(Intent.ACTION_SEND);
                        if (Build.VERSION.SDK_INT >= 24) {
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        i.setType("message/rfc822");
                        i.putExtra(Intent.EXTRA_EMAIL, "");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Logs from " + LocaleController.getInstance().formatterStats.format(System.currentTimeMillis()));
                        i.putExtra(Intent.EXTRA_STREAM, uri);
                        if (getParentActivity() != null) {
                            getParentActivity().startActivityForResult(Intent.createChooser(i, "Select email application."), 500);
                        }
                    } else {
                        Toast.makeText(getParentActivity(), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void doLogout () {
        getTonController().cleanup();
    }

    private void doExport (TonController.UserAuthInfo auth) {
        if (getParentActivity() == null) {
            return;
        }
        showProgressDialog();
        getTonController().getSecretWords(auth.passcode, auth.cipher, (words) -> {
            hideProgressDialog();
            WalletCreateSeedShowActivity fragment = new WalletCreateSeedShowActivity();
            fragment.setSecretWords(words, true);
            presentFragment(fragment);
        }, (text, error) -> {
            hideProgressDialog();
            AlertsCreator.showSimpleAlert(this, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + (error != null ? error.message : text));
        });
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
                case 0: {
                    view = new HeaderCell(context);
                    break;
                }
                case 1: {
                    view = new SettingsTextCell(context);
                    break;
                }
                case 2: {
                    view = new ShadowSectionCell(context);
                    break;
                }
                case 3:
                    view = new SettingsSwitchCell(context);
                    break;
                default: {
                    view = new View(context);
                    break;
                }
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    if (position == generalRow) {
                        cell.setText(LocaleController.getString("WalletGeneral", R.string.WalletGeneral));
                    } else if (position == headerRow) {
                        cell.setText(LocaleController.getString("Wallet", R.string.Wallet));
                    } else if (position == tonConnectHeaderRow) {
                        cell.setText(LocaleController.getString("TonConnect", R.string.TonConnect));
                    }
                    break;
                }
                case 1: {
                    SettingsTextCell cell = (SettingsTextCell) holder.itemView;
                    cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText2));
                    cell.setTag(Theme.key_windowBackgroundWhiteBlackText2);
                    if (position == walletTypeRow) {
                        int walletType = getTonController().getCurrentWalletType();
                        if (walletType == Wallets.WALLET_TYPE_V3R1) {
                            cell.setTextAndValue(LocaleController.getString("WalletAddressType", R.string.WalletAddressType), "v3R1", true);
                        } else if (walletType == Wallets.WALLET_TYPE_V3R2) {
                            cell.setTextAndValue(LocaleController.getString("WalletAddressType", R.string.WalletAddressType), "v3R2", true);
                        } else if (walletType == Wallets.WALLET_TYPE_V4R2) {
                            cell.setTextAndValue(LocaleController.getString("WalletAddressType", R.string.WalletAddressType), "v4R2", true);
                        }
                    } else if (position == currencyRow) {
                        cell.setTextAndValue(LocaleController.getString("WalletPrimaryCurrency", R.string.WalletPrimaryCurrency), Settings.instance().optionShowedCurrency.get(), true);
                    } else if (position == listOfTokensRow) {
                        cell.setText(LocaleController.getString("WalletListOfTokens", R.string.WalletListOfTokens), false);
                    } else if (position == exportRow) {
                        cell.setText(LocaleController.getString("WalletExport", R.string.WalletExport), true);
                    } else if (position == changePasscodeRow) {
                        cell.setText(LocaleController.getString("WalletChangePasscode", R.string.WalletChangePasscode), true);
                    } else if (position == deleteRow) {
                        cell.setText(LocaleController.getString("WalletDelete", R.string.WalletDelete), false);
                        cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText2));
                        cell.setTag(Theme.key_windowBackgroundWhiteRedText2);
                    } else if (position == serverSettingsRow) {
                        cell.setText(LocaleController.getString("WalletServerSettings", R.string.WalletServerSettings), changePasscodeRow != -1);
                    } else if (position == clearLogsRow) {
                        cell.setText("Clear Logs", true);
                    } else if (position == sendLogsRow) {
                        cell.setText("Send Logs", true);
                    } else if (position == tonConnectActiveConnectionsRow) {
                        cell.setText(LocaleController.getString("TonConnectActiveConnections", R.string.TonConnectActiveConnections), false);
                    }
                    break;
                }
                case 2: {
                    if (position == walletSectionRow || position == tonConnectEmptyRow) {
                        Drawable drawable = Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                        CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                        combinedDrawable.setFullsize(true);
                        holder.itemView.setBackgroundDrawable(combinedDrawable);
                    }
                    break;
                }
                case 3: {
                    SettingsSwitchCell cell = (SettingsSwitchCell) holder.itemView;
                    if (position == notificationsRow) {
                        cell.setTextAndCheck(LocaleController.getString("WalletNotifications", R.string.WalletNotifications), getTonController().isNotificationsEnabled(), true);
                    } else if (position == biometricAuthRow) {
                        cell.setTextAndCheck(LocaleController.getString("WalletBiometricAuth", R.string.WalletBiometricAuth), getTonController().getKeyProtectionType() == TonController.KEY_PROTECTION_TYPE_BIOMETRIC, true);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType (int position) {
            if (position == generalRow || position == headerRow || position == tonConnectHeaderRow) {
                return 0;
            } else if (position == walletTypeRow || position == currencyRow || position == listOfTokensRow) {
                return 1;
            } else if (position == exportRow || position == tonConnectActiveConnectionsRow || position == changePasscodeRow || position == deleteRow || position == serverSettingsRow || position == sendLogsRow || position == clearLogsRow) {
                return 1;
            } else if (position == walletSectionRow || position == tonConnectEmptyRow) {
                return 2;
            } else if (position == notificationsRow || position == biometricAuthRow) {
                return 3;
            }

            return -1;
        }

        @Override
        public boolean isEnabled (RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 1 || type == 3;
        }

        @Override
        public int getItemCount () {
            return rowCount;
        }
    }
}
