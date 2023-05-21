package org.UI.Sheets.Templates;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

public class BottomSheetPageLoading extends BottomSheetPaginated.Page {
    private final int heightDp;

    public BottomSheetPageLoading (int heightDp) {
        this.heightDp = heightDp;
    }

    @Override
    protected ViewGroup createView (Context context) {
        FrameLayout frameLayout = new FrameLayout(context);

        RadialProgressView progressView = new RadialProgressView(context);
        progressView.setSize(AndroidUtilities.dp(40));
        progressView.setStrokeWidth(3f);
        progressView.setProgressColor(Theme.getColor(Theme.key_wallet_defaultTonBlue));

        frameLayout.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, heightDp, Gravity.BOTTOM));

        return frameLayout;
    }

    @Override
    protected int getHeight () {
        return AndroidUtilities.dp(heightDp);
    }
}
