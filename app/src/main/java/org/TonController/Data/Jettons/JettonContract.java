package org.TonController.Data.Jettons;

import org.TonController.Data.TonAmount;

public class JettonContract {
    public final RootJettonContract root;
    public final String address;
    public final TonAmount balance;

    public JettonContract (String address, RootJettonContract root) {
        this.root = root;
        this.address = address;
        this.balance = new TonAmount(root.content.decimals);
    }
}
