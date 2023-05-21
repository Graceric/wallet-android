package org.TonController.Data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.Utilities;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Arrays;

import drinkless.org.ton.TonApi;

public class Wallets {
    public static final int WALLET_TYPE_V3R1 = 1;
    public static final int WALLET_TYPE_V3R2 = 2;
    public static final int WALLET_TYPE_V4R2 = 3;

    public static final int[] SUPPORTED_WALLET_TYPES = {WALLET_TYPE_V3R1, WALLET_TYPE_V3R2, WALLET_TYPE_V4R2};

    public static final byte[] WALLET_V3R1_CODE = Utils.hexToBytes("B5EE9C724101010100620000C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED543FBE6EE0");
    public static final byte[] WALLET_V3R2_CODE = Utils.hexToBytes("B5EE9C724101010100710000DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5410BD6DAD");
    public static final byte[] WALLET_V4R2_CODE = Utils.hexToBytes("B5EE9C72410214010002D4000114FF00F4A413F4BCF2C80B010201200203020148040504F8F28308D71820D31FD31FD31F02F823BBF264ED44D0D31FD31FD3FFF404D15143BAF2A15151BAF2A205F901541064F910F2A3F80024A4C8CB1F5240CB1F5230CBFF5210F400C9ED54F80F01D30721C0009F6C519320D74A96D307D402FB00E830E021C001E30021C002E30001C0039130E30D03A4C8CB1F12CB1FCBFF1011121302E6D001D0D3032171B0925F04E022D749C120925F04E002D31F218210706C7567BD22821064737472BDB0925F05E003FA403020FA4401C8CA07CBFFC9D0ED44D0810140D721F404305C810108F40A6FA131B3925F07E005D33FC8258210706C7567BA923830E30D03821064737472BA925F06E30D06070201200809007801FA00F40430F8276F2230500AA121BEF2E0508210706C7567831EB17080185004CB0526CF1658FA0219F400CB6917CB1F5260CB3F20C98040FB0006008A5004810108F45930ED44D0810140D720C801CF16F400C9ED540172B08E23821064737472831EB17080185005CB055003CF1623FA0213CB6ACB1FCB3FC98040FB00925F03E20201200A0B0059BD242B6F6A2684080A06B90FA0218470D4080847A4937D29910CE6903E9FF9837812801B7810148987159F31840201580C0D0011B8C97ED44D0D70B1F8003DB29DFB513420405035C87D010C00B23281F2FFF274006040423D029BE84C600201200E0F0019ADCE76A26840206B90EB85FFC00019AF1DF6A26840106B90EB858FC0006ED207FA00D4D422F90005C8CA0715CBFFC9D077748018C8CB05CB0222CF165005FA0214CB6B12CCCCC973FB00C84014810108F451F2A7020070810108D718FA00D33FC8542047810108F451F2A782106E6F746570748018C8CB05CB025006CF165004FA0214CB6A12CB1FCB3FC973FB0002006C810108D718FA00D33F305224810108F459F2A782106473747270748018C8CB05CB025005CF165003FA0213CB6ACB1F12CB3FC973FB00000AF400C9ED54696225E5");

    public static String getWalletVersionName (int walletType) {
        if (walletType == Wallets.WALLET_TYPE_V3R1) {
            return "v3R1";
        } else if (walletType == Wallets.WALLET_TYPE_V3R2) {
            return "v3R2";
        } else if (walletType == Wallets.WALLET_TYPE_V4R2) {
            return "v4R2";
        } else {
            return "Unknown";
        }
    }

    public static byte[] makeWalletInternalMessageCommentBody (String comment) {
        byte[] bytes = comment.getBytes();
        int parts = (bytes.length / 120) + (bytes.length % 120 > 0 ? 1 : 0);
        Cell result = null;

        for (int a = parts - 1; a >= 0; a--) {
            CellBuilder b = CellBuilder.beginCell();
            if (a == 0) {
                b.storeUint(0, 32);
            }
            b.storeBytes(Arrays.copyOfRange(bytes, 120 * a, Math.min(120 * (a + 1), bytes.length)));
            if (result != null) {
                b.storeRef(result);
            }
            result = b.endCell();
        }

        if (result == null) {
            result = new Cell();
        }

        return result.toBoc(false, true, false);
    }

    public static byte[] makeWalletInternalMessageJettonTransfer (long amount, String toOwnerAddress, String responseAddress, byte[] customPayload, long forwardTonAmount, byte[] forwardPayload) {
        CellBuilder b = CellBuilder.beginCell();
        b.storeUint(0xf8a7ea5L, 32);
        b.storeUint(System.currentTimeMillis(), 64);
        b.storeCoins(BigInteger.valueOf(amount));
        b.storeAddress(Address.of(toOwnerAddress));
        b.storeAddress(Address.of(responseAddress));
        if (!Utilities.isEmpty(customPayload)) {
            b.storeUint(1, 1);
            b.storeRef(Cell.deserializeBoc(customPayload));
        } else {
            b.storeUint(0, 1);
        }
        b.storeCoins(BigInteger.valueOf(forwardTonAmount));
        if (!Utilities.isEmpty(forwardPayload)) {
            b.storeUint(1, 1);
            b.storeRef(Cell.deserializeBoc(forwardPayload));
        } else {
            b.storeUint(0, 1);
        }
        return b.toBoc(false, true, false);
    }


    public static Message makeWalletInternalMessage (String dst, long amount, @Nullable byte[] payload, @Nullable byte[] stateInit, int sendMode) {
        CellBuilder b = CellBuilder.beginCell()
            .storeUint(0, 1)
            .storeUint(1, 1)
            .storeUint(0, 1)    // disable bounce
            .storeUint(0, 1)
            .storeUint(0, 2)    // src
            .storeAddress(Address.of(dst))      // dst
            .storeCoins(BigInteger.valueOf(amount))
            .storeUint(0, 1 + 4 + 4 + 64 + 32);

        if (!Utilities.isEmpty(stateInit)) {
            b.storeUint(3, 2);
            b.storeRef(Cell.deserializeBoc(stateInit));
        } else {
            b.storeUint(0, 1);
        }

        if (!Utilities.isEmpty(payload)) {
            b.storeUint(1, 1);
            b.storeRef(Cell.deserializeBoc(payload));
        } else {
            b.storeUint(0, 1);
        }

        return new Message(b.toBoc(false, true, false), sendMode);
    }

    public static byte[] makeWalletExternalMessageBodyData (long walletId, long validUntil, long msgSeqno, boolean isWalletV4, Message[] messages) {
        CellBuilder builder = CellBuilder.beginCell()
            .storeUint(walletId, 32)
            .storeUint(validUntil, 32)
            .storeUint(msgSeqno, 32);

        if (isWalletV4) {
            builder.storeUint(0, 8);
        }

        for (Message message : messages) {
            builder.storeUint(message.sendMode, 8);
            builder.storeRef(Cell.deserializeBoc(message.message));
        }

        return builder.toBoc(false, true, false);
    }

    public static byte[] makeExternalMessageBody (byte[] privateKey, @NonNull byte[] messageData) {
        Cell messageDataCell = Cell.deserializeBoc(messageData);
        byte[] messageDataHash = messageDataCell.hash();
        byte[] signature = new byte[64];
        Utilities.signEd25519hash(messageDataHash, privateKey, signature);
        return CellBuilder.beginCell()
            .storeBytes(signature)
            .storeSlice(CellSlice.beginParse(messageDataCell))
            .toBoc(false, true, false);
    }

    public static long getSeqnoFromDataCell (@Nullable byte[] data) {
        if (Utilities.isEmpty(data)) return 0;
        return CellSlice.beginParse(Cell.deserializeBoc(data)).preloadUint(32).longValue();
    }

    public static final class Message {
        public byte[] message;
        public int sendMode;

        public Message (byte[] message, int sendMode) {
            this.message = message;
            this.sendMode = sendMode;
        }
    }


    /* TonLib utils */

    public static String getAddressFromInitialAccountState (int wc, @NonNull TonApi.RawInitialAccountState state) {
        byte[] initState = makeInitialAccountState(state);
        String address = wc + ":" + Utils.bytesToHex(Cell.deserializeBoc(initState).hash());
        return Utilities.normalizeAddress(address);
    }

    public static byte[] makeInitialAccountState (@NonNull TonApi.RawInitialAccountState state) {
        CellBuilder b = CellBuilder.beginCell();
        b.storeUint(0, 2);

        if (state.code != null) {
            b.storeUint(1, 1);
            b.storeRef(Cell.deserializeBoc(state.code));
        } else {
            b.storeUint(0, 1);
        }

        if (state.data != null) {
            b.storeUint(1, 1);
            b.storeRef(Cell.deserializeBoc(state.data));
        } else {
            b.storeUint(0, 1);
        }

        b.storeUint(0, 1);
        return b.toBoc(false, true, false);
    }

    public static @NonNull TonApi.RawInitialAccountState makeWalletInitialAccountState (int walletVersion, byte[] publicKey, long walletId) {
        if (walletVersion == Wallets.WALLET_TYPE_V3R1) {
            return Wallets.makeWalletV3R1InitialAccountState(publicKey, walletId);
        } else if (walletVersion == Wallets.WALLET_TYPE_V3R2) {
            return Wallets.makeWalletV3R2InitialAccountState(publicKey, walletId);
        } else if (walletVersion == Wallets.WALLET_TYPE_V4R2) {
            return Wallets.makeWalletV4R2InitialAccountState(publicKey, walletId);
        }
        throw new RuntimeException();
    }

    public static TonApi.RawInitialAccountState makeWalletV3R1InitialAccountState (byte[] publicKey, long walletId) {
        return new TonApi.RawInitialAccountState(WALLET_V3R1_CODE, CellBuilder.beginCell()
            .storeUint(0, 32)
            .storeUint(walletId, 32)
            .storeBytes(publicKey)
            .toBoc(false, true, false));
    }

    public static TonApi.RawInitialAccountState makeWalletV3R2InitialAccountState (byte[] publicKey, long walletId) {
        return new TonApi.RawInitialAccountState(WALLET_V3R2_CODE, CellBuilder.beginCell()
            .storeUint(0, 32)
            .storeUint(walletId, 32)
            .storeBytes(publicKey)
            .toBoc(false, true, false));
    }

    public static TonApi.RawInitialAccountState makeWalletV4R2InitialAccountState (byte[] publicKey, long walletId) {
        return new TonApi.RawInitialAccountState(WALLET_V4R2_CODE, CellBuilder.beginCell()
            .storeUint(0, 32)
            .storeUint(walletId, 32)
            .storeBytes(publicKey)
            .storeUint(0, 1)
            .toBoc(false, true, false));
    }

    public static byte[] makeAddressSlice (String address) {
        return CellBuilder.beginCell().storeAddress(Address.of(address)).toBoc(false, true, false);
    }
}
