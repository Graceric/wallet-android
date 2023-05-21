package org.UI.Fragments.Templates;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class WalletCreateBaseLinearActivity extends WalletCreateBaseActivity {
    protected LinearLayout topLayout;
    protected LinearLayout bottomLayout;

    @Override
    public View createView (Context context) {
        onCreateFragment(context);

        topLayout = new LinearLayout(context);
        topLayout.setOrientation(LinearLayout.VERTICAL);
        topLayout.addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 100, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
        topLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
        topLayout.addView(descriptionText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        bottomLayout = new LinearLayout(context);
        bottomLayout.setOrientation(LinearLayout.VERTICAL);
        bottomLayout.addView(buttonTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL));
        if (secondButton != null) {
            bottomLayout.addView(secondButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));
        }

        walletFragmentView.addView(topLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, getTopLayoutOffset(), 0, 0));
        walletFragmentView.addView(bottomLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, getBottomLayoutOffset()));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setOnTouchListener((v, event) -> true);
        container.addView(actionBar);
        container.addView(walletFragmentView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 1f, Gravity.CENTER_HORIZONTAL));
        fragmentView = container;

        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        return fragmentView;
    }

    protected int getTopLayoutOffset () {
        return 90; // override
    }

    protected int getBottomLayoutOffset () {
        return secondButton != null ? 44 : 100;
    }
}
