package org.UI.Sheets.Templates;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.UI.Components.Buttons.DefaultButtonView;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class BottomSheetPageSimple extends BottomSheetPaginated.Page {
    protected ActionBar actionBar;
    protected DefaultButtonView nextButton;

    @Override
    protected ViewGroup createView (Context context) {
        FrameLayout frameLayout = new FrameLayout(context);

        nextButton = new DefaultButtonView(context);
        frameLayout.addView(nextButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM, 16, 16, 16, 16));

        actionBar = new ActionBar(context);
        actionBar.setItemsColor(Theme.getColor(Theme.key_dialogTextBlack), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_dialogButtonSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_dialogTextBlack));
        actionBar.setOccupyStatusBar(false);
        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return frameLayout;
    }
}
