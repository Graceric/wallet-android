/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public class BuildVars {

    public static boolean DEBUG_VERSION = false;
    public static boolean LOGS_ENABLED = false;
    public static boolean ASK_PASSCODE_ON_START = true;
    public static final int BUILD_VERSION = 1002;
    public static final String BUILD_VERSION_STRING = "1.2";

    public static String[] FAST_IMPORT_MNEMONIC = null; /*new String[]{
        "*", "*", "*", "*", "*", "*",
        "*", "*", "*", "*", "*", "*",
        "*", "*", "*", "*", "*", "*",
        "*", "*", "*", "*", "*", "*"
    };*/

    /* Clear app storage when changing value */
    public static final boolean USE_TESTNET = false;


    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
        }
    }
}
