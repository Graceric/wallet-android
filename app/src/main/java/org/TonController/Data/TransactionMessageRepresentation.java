package org.TonController.Data;

import androidx.annotation.Nullable;

import org.TonController.Parsers.PayloadParser;

public class TransactionMessageRepresentation {
    public final @Nullable PayloadParser.Result payload;
    public final @Nullable String senderOrReceiverAddress;
    public final @Nullable String feeTextFull;
    public final @Nullable String feeTextShort;
    public final String dateTimeFull;
    public final String dateTimeShort;
    public final TonAmount valueChange;

    public TransactionMessageRepresentation (
      @Nullable String senderOrReceiverAddress,
      TonAmount valueChange,
      @Nullable PayloadParser.Result payload,
      @Nullable String feeTextFull,
      @Nullable String feeTextShort,
      String dateTimeFull,
      String dateTimeShort) {

        this.senderOrReceiverAddress = senderOrReceiverAddress;
        this.valueChange = valueChange;
        this.payload = payload;
        this.feeTextFull = feeTextFull;
        this.feeTextShort = feeTextShort;
        this.dateTimeFull = dateTimeFull;
        this.dateTimeShort = dateTimeShort;

    }
}
