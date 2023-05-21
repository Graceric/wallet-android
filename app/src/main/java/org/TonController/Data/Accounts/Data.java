package org.TonController.Data.Accounts;

import android.content.SharedPreferences;

public abstract class Data {
    protected final SharedPreferences preferences;
    protected final String key;

    public Data (SharedPreferences preferences, String key) {
        this.preferences = preferences;
        this.key = key;
    }

    public abstract void save (SharedPreferences.Editor editor);

    public void save () {
        SharedPreferences.Editor editor = preferences.edit();
        save(editor);
        editor.apply();
    }
}
