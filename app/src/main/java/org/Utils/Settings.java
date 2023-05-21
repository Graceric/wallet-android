package org.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public class Settings {
    private static volatile Settings instance;
    private static final AtomicBoolean hasInstance = new AtomicBoolean(false);

    public final StringOption optionShowedCurrency;

    private Settings () {
        SharedPreferences settings = ApplicationLoader.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE);
        optionShowedCurrency = new StringOption(settings, "showed_currency", "USD");
    }

    public static Settings instance () {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    if (hasInstance.getAndSet(true))
                        throw new AssertionError();
                    instance = new Settings();
                }
            }
        }
        return instance;
    }


    /**/

    public static final class StringOption extends Option {
        private String option;
        private final String def;

        public StringOption(SharedPreferences settings, String key, String def) {
            super(settings, key);
            this.def = def;
        }

        public String get() {
            if (option == null) {
                option = settings.getString(key, def);
            }
            return option;
        }

        public void set(String b) {
            option = b;
            settings.edit().putString(key, b).apply();
        }
    }

    public static final class BooleanOption extends Option {
        private Boolean option;
        private final boolean def;

        public BooleanOption(SharedPreferences settings, String key, boolean def) {
            super(settings, key);
            this.def = def;
        }

        public boolean get() {
            if (option == null) {
                option = settings.getBoolean(key, def);
            }
            return option;
        }

        public void set(boolean b) {
            option = b;
            settings.edit().putBoolean(key, b).apply();
        }

        public void reverse() {
            set(!get());
        }
    }

    public static final class IntegerOption extends Option {
        private Integer option;
        private final int def;

        public IntegerOption(SharedPreferences settings, String key, int def) {
            super(settings, key);
            this.def = def;
        }

        public int get() {
            if (option == null) {
                option = settings.getInt(key, def);
            }
            return option;
        }

        public void set(int i) {
            option = i;
            settings.edit().putInt(key, i).apply();
        }
    }

    public static final class LongOption extends Option {
        private Long option;
        private final long def;

        public LongOption(SharedPreferences settings, String key, long def) {
            super(settings, key);
            this.def = def;
        }

        public long get() {
            if (option == null) {
                option = settings.getLong(key, def);
            }
            return option;
        }

        public void set(long i) {
            option = i;
            settings.edit().putLong(key, i).apply();
        }
    }

    public static class Option {
        protected final SharedPreferences settings;
        protected final String key;

        public Option (SharedPreferences settings, String key) {
            this.settings = settings;
            this.key = key;
        }
    }
}
