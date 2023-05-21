package org.TonController.Data.Jettons;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.TonController.Parsers.ContentParser;
import org.telegram.messenger.Utilities;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.Utils.Callbacks;
import org.Utils.network.JsonFetch;

import drinkless.org.ton.TonApi;

public class RootJettonContract {
    public final String address;
    public final @Nullable String adminAddress;
    public final long totalSupply;
    public final boolean isMintable;
    public final RootJettonContent content;

    private RootJettonContract (String address, @Nullable String adminAddress, long totalSupply, boolean isMintable, RootJettonContent content) {
        this.address = address;
        this.adminAddress = adminAddress;
        this.totalSupply = totalSupply;
        this.isMintable = isMintable;
        this.content = content;
    }

    public static void valueOf (final String address, TonApi.SmcRunResult result, Callbacks.RootJettonContractCallback callback, Callbacks.ErrorCallback onErrorRunnable) {
        try {
            if (result.exitCode != 0 || result.stack == null || result.stack.length != 5) {
                onErrorRunnable.run("Wrong get method call result", null);
                return;
            }
            if (!(result.stack[0] instanceof TonApi.TvmStackEntryNumber
                && result.stack[1] instanceof TonApi.TvmStackEntryNumber && (
                result.stack[2] instanceof TonApi.TvmStackEntrySlice ||
                    result.stack[2] instanceof TonApi.TvmStackEntryCell)
                && result.stack[3] instanceof TonApi.TvmStackEntryCell
                && result.stack[4] instanceof TonApi.TvmStackEntryCell
            )) {
                onErrorRunnable.run("Wrong get method call types", null);
                return;
            }

            byte[] contentBytes = ((TonApi.TvmStackEntryCell) result.stack[3]).cell.bytes;
            String totalSupplyStr = ((TonApi.TvmStackEntryNumber) result.stack[0]).number.number;
            final byte[] addressBytes = (result.stack[2] instanceof TonApi.TvmStackEntrySlice) ?
                ((TonApi.TvmStackEntrySlice) result.stack[2]).slice.bytes :
                ((TonApi.TvmStackEntryCell) result.stack[2]).cell.bytes;
            Address adminAddressAdr = CellSlice.beginParse(Cell.deserializeBoc(addressBytes)).loadAddress();

            final String adminAddress = adminAddressAdr != null ? Utilities.normalizeAddress(adminAddressAdr.toString(true)) : null;
            final long totalSupply = Utilities.parseLong(totalSupplyStr);
            final boolean isMintable = !TextUtils.equals(((TonApi.TvmStackEntryNumber) result.stack[1]).number.number, "0");
            final ContentParser.Content parsedContent = ContentParser.parseContent(contentBytes);

            if (!TextUtils.isEmpty(parsedContent.offChain)) {
                JsonFetch.fetch(parsedContent.offChain, offchainContent -> {
                    if (offchainContent == null) {
                        onErrorRunnable.run("Parse fail", null);
                        return;
                    }
                    try {
                        callback.run(new RootJettonContract(address, adminAddress, totalSupply, isMintable,
                            RootJettonContent.valueOf(parsedContent.onChain, offchainContent)));
                    } catch (Throwable e) {
                        onErrorRunnable.run("Parse fail", null);
                    }
                });
            } else {
                callback.run(new RootJettonContract(address, adminAddress, totalSupply, isMintable,
                    RootJettonContent.valueOf(parsedContent.onChain, null)));
            }
        } catch (Throwable t) {
            onErrorRunnable.run("Parse fail", null);
        }
    }

    public void save (SharedPreferences.Editor editor, String key) {
        editor.putBoolean(key + "contains", true);
        editor.putString(key + "address", address);
        editor.putString(key + "adminAddress", adminAddress);
        editor.putLong(key + "totalSupply", totalSupply);
        editor.putBoolean(key + "isMintable", isMintable);
        content.save(editor, key + "content");
    }

    public static RootJettonContract load (SharedPreferences preferences, String key) {
        if (!preferences.getBoolean(key + "contains", false)) {
            throw new Error("contract not found");
        }

        return new RootJettonContract(
            preferences.getString(key + "address", null),
            preferences.getString(key + "adminAddress", null),
            preferences.getLong(key + "totalSupply", 0),
            preferences.getBoolean(key + "isMintable", false),
            RootJettonContent.load(preferences, key + "content")
        );
    }
}
