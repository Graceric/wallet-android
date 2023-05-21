package org.TonController.Data.Jettons;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.TonController.Parsers.ContentParser;
import org.json.JSONObject;
import org.telegram.messenger.Utilities;

public class RootJettonContent {
    public final String name;
    public final String description;
    public final String imageUrl;
    public final String imageData;
    public final String symbol;
    public final int decimals;

    private RootJettonContent (String name, String description, String imageUrl, String imageData, String symbol, int decimals) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageData = imageData;
        this.symbol = symbol;
        this.decimals = decimals;
    }

    public static RootJettonContent valueOf (@Nullable ContentParser.OnChainContent onChainContent, @Nullable JSONObject offChainContent) throws Throwable {
        if (offChainContent == null && onChainContent == null)
            throw new RuntimeException("No Content");

        int decimals = getFieldInt(onChainContent, offChainContent, "decimals", 9);
        return new RootJettonContent(
            getFieldString(onChainContent, offChainContent, "name"),
            getFieldString(onChainContent, offChainContent, "description"),
            getFieldString(onChainContent, offChainContent, "image"),
            getFieldString(onChainContent, offChainContent, "image_data"),
            getFieldString(onChainContent, offChainContent, "symbol"),
            decimals > 0 ? decimals : 9
        );
    }

    private static @Nullable String getFieldString (@Nullable ContentParser.OnChainContent onChainContent, @Nullable JSONObject offChainContent, String fieldName) {
        if (onChainContent != null) {
            String field = onChainContent.known.get(fieldName);
            if (field != null) return field;
        }
        if (offChainContent != null) {
            String field = offChainContent.optString(fieldName);
            if (!TextUtils.isEmpty(field)) return field;
        }

        return null;
    }

    private static int getFieldInt (@Nullable ContentParser.OnChainContent onChainContent, @Nullable JSONObject offChainContent, String fieldName, int defValue) {
        if (onChainContent != null) {
            String field = onChainContent.known.get(fieldName);
            if (field != null) return Utilities.parseInt(field);
        }
        if (offChainContent != null) {
            int field = offChainContent.optInt(fieldName, defValue);
            return field;
        }

        return defValue;
    }

    public String getSymbolOrName () {
        return !TextUtils.isEmpty(symbol) ? symbol: name;
    }

    public void save (SharedPreferences.Editor editor, String key) {
        editor.putBoolean(key + "contains", true);
        editor.putString(key + "name", name);
        editor.putString(key + "description", description);
        editor.putString(key + "imageUrl", imageUrl);
        editor.putString(key + "imageData", imageData);
        editor.putString(key + "symbol", symbol);
        editor.putInt(key + "decimals", decimals);
    }

    public static RootJettonContent load (SharedPreferences preferences, String key) {
        if (!preferences.getBoolean(key + "contains", false)) {
            throw new Error("Contract not found");
        }

        return new RootJettonContent(
            preferences.getString(key + "name", null),
            preferences.getString(key + "description", null),
            preferences.getString(key + "imageUrl", null),
            preferences.getString(key + "imageData", null),
            preferences.getString(key + "symbol", null),
            preferences.getInt(key + "decimals", 9)
        );
    }

}
