package org.Utils.network;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;

public class CoingeckoApi {

    @Nullable
    private static JSONObject currentPrice;
    private static boolean waitPriceLoading;

    public static double getPrice (String currency) {
        if (currentPrice == null) {
            if (!waitPriceLoading) {
                AndroidUtilities.runOnUIThread(CoingeckoApi::loadPrices);
            }
            return -1;
        } else {
            return currentPrice.optDouble(currency, -1);
        }
    }

    private static void loadPrices () {
        waitPriceLoading = true;
        JsonFetch.fetch("https://api.coingecko.com/api/v3/coins/the-open-network", jsonObject -> {
            waitPriceLoading = false;
            if (jsonObject == null) return;
            try {
                JSONObject marketData = jsonObject.getJSONObject("market_data");
                currentPrice = marketData.getJSONObject("current_price");
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.coingeckoPricesUpdated);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }
}

