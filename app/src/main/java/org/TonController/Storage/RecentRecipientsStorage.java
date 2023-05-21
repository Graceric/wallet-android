package org.TonController.Storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;

public class RecentRecipientsStorage {
    private final SharedPreferences preferences;
    private final ArrayList<RecentRecipient> recentRecipients = new ArrayList<>(0);

    public RecentRecipientsStorage () {
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("recent_recipient_cache", Context.MODE_PRIVATE);
        load();
    }

    public void add (String address, String domain) {
        for (int a = 0; a < recentRecipients.size(); a++) {
            if (TextUtils.equals(recentRecipients.get(a).address, address)) {
                recentRecipients.remove(a);
                break;
            }
        }

        recentRecipients.add(0, new RecentRecipient(address, domain, System.currentTimeMillis() / 1000));
        save();
    }

    public void clear () {
        preferences.edit().clear().apply();
        recentRecipients.clear();
    }

    public ArrayList<RecentRecipient> getRecipients () {
        return recentRecipients;
    }

    private void save () {
        SharedPreferences.Editor editor = preferences.edit();
        int count = Math.min(10, recentRecipients.size());
        editor.putInt("count", count);
        for (int a = 0; a < count; a++) {
            RecentRecipient r = recentRecipients.get(a);
            editor.putString("recent." + a + ".address", r.address);
            editor.putString("recent." + a + ".domain", r.domain);
            editor.putLong("recent." + a + ".time", r.time);
        }
        editor.apply();
    }

    private void load () {
        int count = preferences.getInt("count", 0);
        for (int a = 0; a < count; a++) {
            RecentRecipient r = new RecentRecipient(
                preferences.getString("recent." + a + ".address", null),
                preferences.getString("recent." + a + ".domain", null),
                preferences.getLong("recent." + a + ".time", 0)
            );
            if (r.address == null || r.time == 0) continue;
            recentRecipients.add(r);
        }
    }

    public static class RecentRecipient {
        public final String address;
        public final String domain;
        public final long time;

        public RecentRecipient (String address, String domain, long time) {
            this.address = address;
            this.domain = domain;
            this.time = time;
        }
    }
}
