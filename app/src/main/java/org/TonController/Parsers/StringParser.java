package org.TonController.Parsers;

import org.ton.java.cell.CellSlice;

public class StringParser {
    public static String readString (CellSlice slice) {
        int prefix = slice.loadUint(8).intValue();
        if (prefix == 0) {
            return readSnakeString(slice);
        } else if (prefix == 1) {
            return readChunkedString(slice);
        } else {
            throw new Error();
        }
    }

    public static String readSnakeString (CellSlice slice) {
        StringBuilder b = new StringBuilder();
        while (!slice.isSliceEmpty()) {
            b.append(slice.loadString(slice.remainingBits()));
            if (slice.hasRef()) {
                slice = CellSlice.beginParse(slice.preloadRef());
            }
        }

        return b.toString();
    }

    public static String readChunkedString (CellSlice slice) {
        return "";      // todo: read hashmap string
    }
}
