/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.telegram.messenger;

import org.TonController.TonController;

public class BaseController {

    protected int currentAccount;
    private AccountInstance parentAccountInstance;

    public BaseController(int num) {
        parentAccountInstance = AccountInstance.getInstance(num);
        currentAccount = num;
    }

    protected final AccountInstance getAccountInstance() {
        return parentAccountInstance;
    }

    public final NotificationCenter getNotificationCenter() {
        return parentAccountInstance.getNotificationCenter();
    }

    protected final TonController getTonController() {
        return parentAccountInstance.getTonController();
    }
}
