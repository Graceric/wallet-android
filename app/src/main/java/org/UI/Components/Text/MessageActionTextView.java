package org.UI.Components.Text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import org.TonController.Parsers.PayloadParser;
import org.UI.Utils.Drawables;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;

public class MessageActionTextView extends TextView {
    private Drawable iconDrawable;
    private Paint iconPaint;

    public MessageActionTextView (Context context) {
        super(context);

        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        setTextColor(Theme.getColor(Theme.key_wallet_blackText));
        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), Theme.getColor(Theme.key_wallet_transactionCommentBackground)));
    }

    public void setIcon (@DrawableRes int res) {
        setPadding(AndroidUtilities.dp(res == 0 ? 12 : 48), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_wallet_transactionCommentIcon), PorterDuff.Mode.SRC_IN));
        iconDrawable = Drawables.get(res);
        requestLayout();
        invalidate();
    }

    @Override
    protected void dispatchDraw (Canvas canvas) {
        if (iconDrawable != null) {
            Drawables.drawCentered(canvas, iconDrawable, AndroidUtilities.dp(24), getMeasuredHeight() / 2f, iconPaint);
        }
        super.dispatchDraw(canvas);
    }

    public void setTransactionMessage (@Nullable PayloadParser.Result parsedPayload, byte[] stateInit, boolean inline) {
        boolean isVisible = true;
        if (parsedPayload instanceof PayloadParser.ResultComment) {
            String comment = ((PayloadParser.ResultComment) parsedPayload).comment;
            if (inline) {
                comment = comment.replaceAll("\n", " ");
            }
            setText(comment);
            setIcon(0);
        } else if (!Utilities.isEmpty(stateInit) && (parsedPayload == null || parsedPayload instanceof PayloadParser.ResultUnknown || parsedPayload instanceof PayloadParser.ResultError)) {
            setText(LocaleController.getString("WalletTransactionActionDeploy", R.string.WalletTransactionActionDeploy));
            setIcon(R.drawable.baseline_code_24);
        } else if (parsedPayload != null) {
            setText(parsedPayload.getPayloadActionName());
            setIcon(parsedPayload.getPayloadIconId());
        } else {
            isVisible = false;
        }

        setVisibility(isVisible ? VISIBLE : GONE);
    }
}
