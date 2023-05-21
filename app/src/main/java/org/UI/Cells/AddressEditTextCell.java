package org.UI.Cells;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.PollEditTextCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.Utils.Callbacks;
import org.TonController.Parsers.UriParser;

public class AddressEditTextCell extends PollEditTextCell {
    private Callbacks.TonLinkDelegate pasteDelegate;
    private Callbacks.StringCallback editDelegate;

    public AddressEditTextCell (Context context) {
        super(context, null);

        EditTextBoldCursor editText = getTextView();
        editText.setSingleLine(false);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setMinLines(2);
        editText.setTypeface(Typeface.DEFAULT);
        editText.setGravity(Gravity.LEFT | Gravity.TOP);
        editText.setBackground(Theme.createEditTextDrawable(context, true));
        editText.setPadding(0, AndroidUtilities.dp(13), 0, AndroidUtilities.dp(8));

        addTextWatcher(new TextWatcher() {
            private boolean ignoreTextChange;
            private boolean isPaste;

            @Override
            public void beforeTextChanged (CharSequence s, int start, int count, int after) {
                isPaste = after >= 24;
            }

            @Override
            public void onTextChanged (CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged (Editable s) {
                if (ignoreTextChange) {
                    return;
                }
                String str = s.toString();

                if (isPaste && str.toLowerCase().startsWith("ton://transfer")) {
                    ignoreTextChange = true;
                    UriParser.Result result = UriParser.parse(str);
                    if (result instanceof UriParser.ResultTonLink) {
                        UriParser.ResultTonLink parsed = (UriParser.ResultTonLink) result;
                        setText(str = parsed.address, drawDivider());
                        if (pasteDelegate != null) {
                            pasteDelegate.run(parsed);
                        }
                    }
                    ignoreTextChange = false;
                }

                if (editDelegate != null) {
                    editDelegate.run(str);
                }
            }
        });
    }

    public void setPasteDelegate (Callbacks.TonLinkDelegate pasteDelegate) {
        this.pasteDelegate = pasteDelegate;
    }

    public void setEditDelegate (Callbacks.StringCallback editDelegate) {
        this.editDelegate = editDelegate;
    }
}
