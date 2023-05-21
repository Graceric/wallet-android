package org.TonController.Parsers;

import androidx.annotation.Nullable;

import org.ton.java.bitstring.BitString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;
import org.ton.java.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class ContentParser {
    public static final HashMap<String, String> knownParameters;

    static {
        knownParameters = new HashMap<>();
        knownParameters.put("70e5d7b6a29b392f85076fe15ca2f2053c56c2338728c4e33c9e8ddb1ee827cc".toUpperCase(), "uri");
        knownParameters.put("82a3537ff0dbce7eec35d69edc3a189ee6f17d82f353a553f9aa96cb0be3ce89".toUpperCase(), "name");
        knownParameters.put("c9046f7a37ad0ea7cee73355984fa5428982f8b37c8f7bcec91f7ac71a7cd104".toUpperCase(), "description");
        knownParameters.put("6105d6cc76af400325e94d588ce511be5bfdbb73b437dc51eca43917d7a43e3d".toUpperCase(), "image");
        knownParameters.put("d9a88ccec79eef59c84b671136a20ece4cd00caaad5bc47e2c208829154ee9e4".toUpperCase(), "image_data");
        knownParameters.put("b76a7ca153c24671658335bbd08946350ffc621fa1c516e7123095d4ffd5c581".toUpperCase(), "symbol");
        knownParameters.put("ee80fd2f1e03480e2282363596ee752d7bb27f50776b95086a0279189675923e".toUpperCase(), "decimals");
    }

    public static Content parseContent (byte[] bytes) {
        CellSlice slice = CellSlice.beginParse(Cell.deserializeBoc(bytes));
        int prefix = slice.loadUint(8).intValue();
        if (prefix == 1) {
            return new Content(StringParser.readSnakeString(slice), null);
        } else if (prefix == 0) {
            if (slice.remainingBits() == 0 || slice.remainingBits() == 1) {
                slice = CellSlice.beginParse(slice.loadRef());
            }

            TonHashMap h = slice.loadDict(256, ContentParser::parseKey, ContentParser::parseValue);
            OnChainContent onChainContent = new OnChainContent();
            for (Map.Entry<Object, Object> entry : h.elements.entrySet()) {
                String hex = (String) entry.getKey();
                String value = (String) entry.getValue();
                String key = knownParameters.get(hex);
                if (key != null) {
                    onChainContent.known.put(key, value);
                } else {
                    onChainContent.unknown.put(hex, value);
                }
            }

            String uri = onChainContent.known.get("uri");
            return new Content(uri, onChainContent);
        } else {
            throw new Error("wrong prefix");
        }
    }

    private static String parseKey (BitString bits) {
        return Utils.bytesToHex(bits.readBytes(bits.length));
    }

    private static String parseValue (Cell value) {
        CellSlice slice = CellSlice.beginParse(value);
        if (slice.remainingBits() == 0) {
            slice = CellSlice.beginParse(slice.loadRef());
        }
        return StringParser.readString(slice);
    }

    public static class OnChainContent {
        public final HashMap<String, String> known = new HashMap<>();
        public final HashMap<String, String> unknown = new HashMap<>();
    }

    public static class Content {
        public final @Nullable String offChain;
        public final @Nullable OnChainContent onChain;

        private Content (@Nullable String offChain, @Nullable OnChainContent onChain) {
            this.offChain = offChain;
            this.onChain = onChain;
        }
    }
}
