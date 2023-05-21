/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.telegram.ui.Components;

import android.app.Dialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.TonController.Data.Wallets;
import org.TonController.TonController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.RadioColorCell;
import org.Utils.Settings;

public class AlertsCreator {

    public static Toast showSimpleToast(BaseFragment baseFragment, final String text) {
        if (text == null) {
            return null;
        }
        Context context;
        if (baseFragment != null && baseFragment.getParentActivity() != null) {
            context = baseFragment.getParentActivity();
        } else {
            context = ApplicationLoader.applicationContext;
        }
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.show();
        return toast;
    }

    public static AlertDialog.Builder createSimpleAlert(Context context, final String text) {
        return createSimpleAlert(context, null, text);
    }

    public static AlertDialog.Builder createSimpleAlert(Context context, final String title, final String text) {
        if (text == null) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title == null ? LocaleController.getString("AppName", R.string.AppName) : title);
        builder.setMessage(text);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        return builder;
    }

    public static Dialog showSimpleAlert(BaseFragment baseFragment, final String text) {
        return showSimpleAlert(baseFragment, null, text);
    }

    public static Dialog showSimpleAlert(BaseFragment baseFragment, final String title, final String text) {
        if (text == null || baseFragment == null || baseFragment.getParentActivity() == null) {
            return null;
        }
        AlertDialog.Builder builder = createSimpleAlert(baseFragment.getParentActivity(), title, text);
        Dialog dialog = builder.create();
        baseFragment.showDialog(dialog);
        return dialog;
    }

    public static Dialog createWalletTypeSelectDialog(Context context, int currentAccount, Runnable onSelect) {
        TonController controller = TonController.getInstance(currentAccount);
        /*HashMap<String, AccountsStateManager.WalletInfo> wallets =
            controller.getAccountsStateManager().getSupportedWalletsMapWithAddressKey();*/

        final int selected = controller.getCurrentWalletType();
        String[] descriptions = new String[]{ "Wallet v3R1", "Wallet v3R2", "Wallet v4R2" };
        String[] addresses = new String[]{
            controller.getWalletAddressFromType(Wallets.WALLET_TYPE_V3R1),
            controller.getWalletAddressFromType(Wallets.WALLET_TYPE_V3R2),
            controller.getWalletAddressFromType(Wallets.WALLET_TYPE_V4R2)
        };

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        for (int a = 0; a < descriptions.length; a++) {
            boolean isSelected = (a == 0 && selected == Wallets.WALLET_TYPE_V3R1)
                || (a == 1 && selected == Wallets.WALLET_TYPE_V3R2)
                || (a == 2 && selected == Wallets.WALLET_TYPE_V4R2);

            RadioColorCell cell = new RadioColorCell(context);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(a);
            cell.setTextAndText2AndValue(descriptions[a], Utilities.truncateString(addresses[a], 5, 5), isSelected);
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                final int newSelected = (Integer) v.getTag();
                int toSelect = -1;
                if (newSelected == 0) {
                    toSelect = Wallets.WALLET_TYPE_V3R1;
                } else if (newSelected == 1) {
                    toSelect = Wallets.WALLET_TYPE_V3R2;
                } else if (newSelected == 2) {
                    toSelect = Wallets.WALLET_TYPE_V4R2;
                }
                if (toSelect != -1) {
                    TonController.getInstance(currentAccount).updateWalletAddressAndInfo(toSelect);
                }
                builder.getDismissRunnable().run();
                if (onSelect != null) {
                    onSelect.run();
                }


            });
        }
        builder.setTitle(LocaleController.getString("WalletAddressType", R.string.WalletAddressType));
        builder.setView(linearLayout);
        builder.setPositiveButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        return builder.create();
    }

    public static Dialog createCurrencySelectDialog(Context context, Runnable onSelect) {
        final String current = Settings.instance().optionShowedCurrency.get();
        String[] descriptions = new String[]{ "USD", "EUR", "BTC" };

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        for (String description : descriptions) {
            boolean isSelected = description.equals(current);

            RadioColorCell cell = new RadioColorCell(context);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(description);
            cell.setTextAndValue(description, isSelected);
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                final String newSelected = (String) v.getTag();
                Settings.instance().optionShowedCurrency.set(newSelected);

                builder.getDismissRunnable().run();
                if (onSelect != null) {
                    onSelect.run();
                }
            });
        }
        builder.setTitle(LocaleController.getString("WalletPrimaryCurrency", R.string.WalletPrimaryCurrency));
        builder.setView(linearLayout);
        builder.setPositiveButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        return builder.create();
    }
}
