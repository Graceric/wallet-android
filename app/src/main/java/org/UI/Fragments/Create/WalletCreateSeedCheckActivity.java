package org.UI.Fragments.Create;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.UI.Fragments.Templates.WalletCreateBaseSeedInputActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class WalletCreateSeedCheckActivity extends WalletCreateBaseSeedInputActivity {

    private ArrayList<Integer> checkWordIndices;

    public void setSecretWords (String[] secretWords) {
        this.secretWords = secretWords;
    }

    @Override
    public boolean onFragmentCreate () {
        if (secretWords == null || secretWords.length != 24) {
            return false;
        }
        checkWordIndices = new ArrayList<>();
        while (checkWordIndices.size() < 3) {
            int index = Utilities.random.nextInt(24);
            if (checkWordIndices.contains(index)) {
                continue;
            }
            checkWordIndices.add(index);
        }
        Collections.sort(checkWordIndices);
        return super.onFragmentCreate();
    }

    @Override
    public View createView (Context context) {
        super.createView(context);
        swipeBackEnabled = false;

        LinearLayout scrollViewLinearLayout = new LinearLayout(context);
        scrollViewLinearLayout.setOrientation(LinearLayout.VERTICAL);

        maxEditNumberWidth = 0;
        editTexts = new NumericEditText[3];
        for (int a = 0; a < editTexts.length; a++) {
            scrollViewLinearLayout.addView(editTexts[a] = new NumericEditText(context, a, String.format(Locale.US, "%d:", checkWordIndices.get(a) + 1)), LayoutHelper.createLinear(200, 44, Gravity.CENTER_HORIZONTAL, 0, a == 0 ? 21 : 13, 0, 0));
        }

        this.scrollViewLinearLayout.addView(scrollViewLinearLayout, 1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 80, 28, 80, 28));

        actionBar.setTitle(LocaleController.getString("WalletTestTimeTitle", R.string.WalletTestTimeTitle));
        titleTextView.setText(LocaleController.getString("WalletTestTime", R.string.WalletTestTime));
        descriptionText.setText(AndroidUtilities.replaceTags(LocaleController.formatString("WalletTestTimeInfo", R.string.WalletTestTimeInfo, checkWordIndices.get(0) + 1, checkWordIndices.get(1) + 1, checkWordIndices.get(2) + 1)));
        buttonTextView.setText(LocaleController.getString("WalletContinue", R.string.WalletContinue));

        return fragmentView;
    }

    @Override
    protected void onMainButtonClick (View v) {
        hideHint();
        if (!checkEditTexts()) {
            return;
        }
        for (int a = 0, N = checkWordIndices.size(); a < N; a++) {
            int index = checkWordIndices.get(a);
            if (!secretWords[index].equals(editTexts[a].getText().toString())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("WalletTestTimeAlertTitle", R.string.WalletTestTimeAlertTitle));
                builder.setMessage(LocaleController.getString("WalletTestTimeAlertText", R.string.WalletTestTimeAlertText));
                builder.setNegativeButton(LocaleController.getString("WalletTestTimeAlertButtonSee", R.string.WalletTestTimeAlertButtonSee), (dialog, which) -> finishFragment());
                builder.setPositiveButton(LocaleController.getString("WalletTestTimeAlertButtonTry", R.string.WalletTestTimeAlertButtonTry), null);
                showDialog(builder.create());
                return;
            }
        }
        if (fragmentToRemove != null) {
            fragmentToRemove.removeSelfFromStack();
        }
        presentFragment(new WalletCreatePerfectActivity(), true);
    }

    @Override
    protected int getImageAnimation () {
        return R.raw.wallet_test_time;
    }
}
