package org.TonController;

import androidx.annotation.NonNull;

import org.Utils.Callbacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import drinkless.org.ton.TonApi;

class AccountStateReceiver {
    public interface AccountStateReceiveGlobalCallback {
        void run (@NonNull String address, @NonNull TonApi.RawFullAccountState state);
    }

    public final TonController.GuaranteedReceiver receiver =
        new TonController.GuaranteedReceiver(TonApi.RawFullAccountState.CONSTRUCTOR);

    private final HashMap<String, HashSet<Callbacks.AccountStateCallback>> callbacks = new HashMap<>();
    private final AccountStateReceiveGlobalCallback accountStateReceiveGlobalCallback;
    private final ArrayList<String> queue = new ArrayList<>();

    private boolean loading = false;

    public AccountStateReceiver (AccountStateReceiveGlobalCallback callback) {
        this.accountStateReceiveGlobalCallback = callback;
    }

    public void receive (String address, Callbacks.AccountStateCallback callback) {
        addRequestToQueue(address, callback);
        if (loading) {
            return;
        }

        loading = true;
        receiveImpl(queue.get(0));
    }

    private void receiveImpl (String address) {
        receiver.receive(new TonApi.RawGetAccountState(new TonApi.AccountAddress(address)), result -> {
            TonApi.RawFullAccountState state = (TonApi.RawFullAccountState) result;
            accountStateReceiveGlobalCallback.run(address, state);
            HashSet<Callbacks.AccountStateCallback> callbacks = this.callbacks.remove(address);
            if (callbacks != null) {
                for (Callbacks.AccountStateCallback callback : callbacks) {
                    callback.run(state);
                }
            }
            if (queue.size() > 0) {
                if (queue.get(0).equals(address)) {
                    queue.remove(0);
                }
            }
            if (queue.isEmpty()) {
                loading = false;
            } else {
                receiveImpl(queue.get(0));
            }
        });
    }

    private void addRequestToQueue (String address, Callbacks.AccountStateCallback callback) {
        HashSet<Callbacks.AccountStateCallback> addrCallbacks = callbacks.get(address);
        if (addrCallbacks == null) {
            callbacks.put(address, addrCallbacks = new HashSet<>());
            queue.add(address);
        }

        addrCallbacks.add(callback);
    }
}
