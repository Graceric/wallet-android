package org.TonController.Parsers;

import androidx.annotation.Nullable;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

public class PayloadParser {
    public static Result parse (byte[] cell) {
        if (Utilities.isEmpty(cell)) return null;

        /*
        int op::ownership_assigned() asm "0x05138d91 PUSHINT";
        int op::edit_content() asm "0x1a0b9d51 PUSHINT";
        int op::transfer_editorship() asm "0x1c04412a PUSHINT";
        int op::editorship_assigned() asm "0x511a4463 PUSHINT";
        int op::transfer_notification() asm " PUSHINT";
        int op::burn_notification() asm "0x7bdd97de PUSHINT";
        int op::mint() asm "21 PUSHINT";
        */

        try {
            Cell c = Cell.deserializeBoc(cell);
            CellSlice cs = CellSlice.beginParse(c);
            if (cs.remainingBits() == 0) return null;

            long opCode = cs.loadUint(32).longValue();
            if (opCode == 0) {                                                  // comment
                String comment = StringParser.readSnakeString(cs);
                return new ResultComment(comment);
            } else {
                long queryId = cs.loadUint(64).longValue();
                if (opCode == 0x5fcc3d14L) {
                    String newOwner = Utilities.normalizeAddress(cs.loadAddress().toString(true));
                    return new ResultNftTransfer(queryId, newOwner);
                } else if (opCode == 0x05138d91L) {
                    return new ResultNftReceiveNotification(queryId);
                } else if (opCode == 0xf8a7ea5L) {
                    BigInteger jettonAmount = cs.loadCoins();
                    String toOwnerAddress = Utilities.normalizeAddress(cs.loadAddress());
                    return new ResultJettonTransfer(queryId, jettonAmount, toOwnerAddress);
                } else if (opCode == 0x178d4519L) {
                    BigInteger jettonAmount = cs.loadCoins();
                    String fromAddress = Utilities.normalizeAddress(cs.loadAddress());
                    String responseAddress = Utilities.normalizeAddress(cs.loadAddress());
                    long forwardTonAmount = cs.loadCoins().longValue();
                    byte[] forwardPayload = cs.loadBit() ?
                        cs.loadRef().toBoc(false, true, false):
                        new byte[0];
                    return new ResultJettonInternalTransfer(queryId, jettonAmount, fromAddress, responseAddress, forwardTonAmount, forwardPayload);
                } else if (opCode == 0x7362d09cL) {
                    return new ResultJettonReceiveNotification(queryId);
                } else if (opCode == 0x595f07bcL) {
                    return new ResultJettonBurn(queryId);
                } else if (opCode == 0xd53276dbL) {
                    return new ResultExcesses(queryId);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
            return new ResultError();
        }
        return new ResultUnknown();
    }

    public static abstract class Result {
        public abstract String getPayloadActionName ();

        public abstract int getPayloadIconId ();
    }

    public static class ResultComment extends Result {
        public final String comment;

        private ResultComment (String comment) {
            this.comment = comment;
        }

        public String getPayloadActionName () {
            return comment;
        }

        public int getPayloadIconId () {
            return 0;
        }
    }

    public abstract static class ResultQuery extends Result {
        public final long queryId;

        private ResultQuery (long queryId) {
            this.queryId = queryId;
        }
    }

    public static class ResultNftTransfer extends ResultQuery {
        public final String newOwner;

        private ResultNftTransfer (long queryId, String newOwner) {
            super(queryId);
            this.newOwner = newOwner;
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionNftTransfer", R.string.WalletTransactionActionNftTransfer);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultNftReceiveNotification extends ResultQuery {
        private ResultNftReceiveNotification (long queryId) {
            super(queryId);
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionNftReceive", R.string.WalletTransactionActionNftReceive);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultJettonTransfer extends ResultQuery {
        public BigInteger jettonAmount;
        public String toOwnerAddress;

        public ResultJettonTransfer (long queryId, BigInteger jettonAmount, String toOwnerAddress) {
            super(queryId);
            this.jettonAmount = jettonAmount;
            this.toOwnerAddress = toOwnerAddress;
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionJettonTransfer", R.string.WalletTransactionActionJettonTransfer);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultJettonInternalTransfer extends ResultQuery {
        public final BigInteger amount;
        public final @Nullable String fromAddress;
        public final @Nullable String responseAddress;
        public final long forwardTonAmount;
        public final byte[] forwardPayload;
        public final PayloadParser.Result parsedForwardPayload;

        private ResultJettonInternalTransfer (long queryId, BigInteger amount, @Nullable String fromAddress, @Nullable String responseAddress, long forwardTonAmount, byte[] forwardPayload) {
            super(queryId);
            this.amount = amount;
            this.fromAddress = fromAddress;
            this.responseAddress = responseAddress;
            this.forwardTonAmount = forwardTonAmount;
            this.forwardPayload = forwardPayload;
            this.parsedForwardPayload = parse(forwardPayload);
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionJettonTransfer", R.string.WalletTransactionActionJettonTransfer);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultJettonReceiveNotification extends ResultQuery {
        private ResultJettonReceiveNotification (long queryId) {
            super(queryId);
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionJettonReceive", R.string.WalletTransactionActionJettonReceive);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultJettonBurn extends ResultQuery {
        private ResultJettonBurn (long queryId) {
            super(queryId);
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionJettonBurn", R.string.WalletTransactionActionJettonBurn);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultExcesses extends ResultQuery {


        private ResultExcesses (long queryId) {
            super(queryId);
        }

        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionExcesses", R.string.WalletTransactionActionExcesses);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_transfer_24;
        }
    }

    public static class ResultUnknown extends Result {
        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionUnknown", R.string.WalletTransactionActionUnknown);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_code_24;
        }
    }

    public static class ResultError extends Result {
        public String getPayloadActionName () {
            return LocaleController.getString("WalletTransactionActionUnknown", R.string.WalletTransactionActionUnknown);
        }

        public int getPayloadIconId () {
            return R.drawable.baseline_code_24;
        }
    }
}
