/*
 * This is the source code of Wallet for Android v. 1.0.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright Nikolai Kudashov, 2019-2020.
 */

package org.telegram.ui.ActionBar;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import org.TonController.AccountsStateManager;
import org.TonController.TonConnect.TonConnectController;
import org.TonController.TonController;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BiometricPromtHelper;
import org.UI.Fragments.CameraScanActivity;
import org.UI.Fragments.Passcode.PasscodeActivity;
import org.Utils.Callbacks;

import java.util.ArrayList;

import drinkless.org.ton.TonApi;

public class BaseFragment {

    protected int currentAccount = Utilities.selectedAccount;

    private static int lastClassGuid = 0;

    private boolean isFinished;
    private boolean finishing;

    protected int classGuid = lastClassGuid++;
    protected Dialog visibleDialog;

    protected View fragmentView;
    protected ActionBarLayout parentLayout;
    protected ActionBar actionBar;
    protected boolean inPreviewMode;
    protected Bundle arguments;
    protected boolean swipeBackEnabled = true;
    protected boolean hasOwnBackground = false;
    protected boolean isPaused = true;

    public BaseFragment() {

    }

    public BaseFragment(Bundle args) {
        arguments = args;
    }

    public ActionBar getActionBar() {
        return actionBar;
    }

    public View getFragmentView() {
        return fragmentView;
    }

    public View createView(Context context) {
        return null;
    }

    public Bundle getArguments() {
        return arguments;
    }

    protected void setInPreviewMode(boolean value) {
        inPreviewMode = value;
        if (actionBar != null) {
            if (inPreviewMode) {
                actionBar.setOccupyStatusBar(false);
            } else {
                actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21);
            }
        }
    }

    protected void clearViews() {
        if (fragmentView != null) {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                try {
                    onRemoveFromParent();
                    parent.removeView(fragmentView);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            fragmentView = null;
        }
        if (actionBar != null) {
            ViewGroup parent = (ViewGroup) actionBar.getParent();
            if (parent != null) {
                try {
                    parent.removeView(actionBar);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            actionBar = null;
        }
        parentLayout = null;
    }

    protected void onRemoveFromParent() {

    }

    public void setParentFragment(BaseFragment fragment) {
        setParentLayout(fragment.parentLayout);
        fragmentView = createView(parentLayout.getContext());
    }

    protected void setParentLayout(ActionBarLayout layout) {
        if (parentLayout != layout) {
            parentLayout = layout;
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    try {
                        onRemoveFromParent();
                        parent.removeView(fragmentView);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (parentLayout != null && parentLayout.getContext() != fragmentView.getContext()) {
                    fragmentView = null;
                }
            }
            if (actionBar != null) {
                boolean differentParent = parentLayout != null && parentLayout.getContext() != actionBar.getContext();
                if (actionBar.getAddToContainer() || differentParent) {
                    ViewGroup parent = (ViewGroup) actionBar.getParent();
                    if (parent != null) {
                        try {
                            parent.removeView(actionBar);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }
                if (differentParent) {
                    actionBar = null;
                }
            }
            if (parentLayout != null && actionBar == null) {
                actionBar = createActionBar(parentLayout.getContext());
                actionBar.parentFragment = this;
            }
        }
    }

    protected ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarActionModeDefaultSelector), true);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon), true);
        if (inPreviewMode) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    public void movePreviewFragment(float dy) {
        parentLayout.movePreviewFragment(dy);
    }

    public void finishPreviewFragment() {
        parentLayout.finishPreviewFragment();
    }

    public void finishFragment() {
        finishFragment(true);
    }

    public void finishFragment(boolean animated) {
        if (onSheetDismissDelegate != null) {
            onSheetDismissDelegate.run();
            return;
        }
        if (isFinished || parentLayout == null) {
            return;
        }
        finishing = true;
        parentLayout.closeLastFragment(animated);
    }

    public void removeSelfFromStack() {
        if (onSheetDismissDelegate != null) {
            onSheetDismissDelegate.run();
            return;
        }
        if (isFinished || parentLayout == null) {
            return;
        }
        parentLayout.removeFragmentFromStack(this);
    }

    protected boolean isFinishing() {
        return finishing;
    }

    public boolean onFragmentCreate() {
        return true;
    }

    public void onFragmentDestroy() {
        isFinished = true;
        if (actionBar != null) {
            actionBar.setEnabled(false);
        }
    }

    public boolean needDelayOpenAnimation() {
        return false;
    }

    public void onResume() {
        isPaused = false;
        if (visibleDialog instanceof BottomSheet) {
            ((BottomSheet) visibleDialog).onParentFragmentResume();
        }
        if (cameraScanLayout != null && cameraScanLayout.length != 0) {
            if (cameraScanLayout[cameraScanLayout.length - 1] != null) {
                cameraScanLayout[cameraScanLayout.length - 1].onResume();
            }
        }
        if (progressShowCounter > 0 && progressDialog == null) {
            progressDialog = new AlertDialog(getParentActivity(), 3);
            progressDialog.setCanCacnel(false);
            progressDialog.show();
        }
    }

    public void onPause() {
        if (actionBar != null) {
            actionBar.onPause();
        }
        if (biometricPromtHelper != null) {
            biometricPromtHelper.onPause();
        }
        if (cameraScanLayout != null && cameraScanLayout.length != 0) {
            if (cameraScanLayout[cameraScanLayout.length - 1] != null) {
                cameraScanLayout[cameraScanLayout.length - 1].onPause();
            }
        }
        isPaused = true;
        try {
            if (visibleDialog != null && visibleDialog.isShowing()) {
                if (dismissDialogOnPause(visibleDialog)) {
                    visibleDialog.dismiss();
                    visibleDialog = null;
                } else if (visibleDialog instanceof BottomSheet) {
                    ((BottomSheet) visibleDialog).onParentFragmentPause();
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public BaseFragment getFragmentForAlert(int offset) {
        if (parentLayout == null || parentLayout.fragmentsStack.size() <= 1 + offset) {
            return this;
        }
        return parentLayout.fragmentsStack.get(parentLayout.fragmentsStack.size() - 2 - offset);
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {

    }

    public boolean onBackPressed() {
        return true;
    }

    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (cameraScanLayout != null && cameraScanLayout[0] != null) {
            cameraScanLayout[0].fragmentsStack.get(0).onActivityResultFragment(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (cameraScanLayout != null && cameraScanLayout[0] != null) {
            cameraScanLayout[0].fragmentsStack.get(0).onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
    }

    public void saveSelfArgs(Bundle args) {

    }

    public void restoreSelfArgs(Bundle args) {

    }

    public boolean presentFragmentAsPreview(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragmentAsPreview(fragment);
    }

    public boolean presentFragment(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragment(fragment);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast) {
        return parentLayout != null && parentLayout.presentFragment(fragment, removeLast);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation) {
        return parentLayout != null && parentLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, true, false);
    }

    public Activity getParentActivity() {
        if (parentLayout != null) {
            return parentLayout.parentActivity;
        }
        return null;
    }

    protected void setParentActivityTitle(CharSequence title) {
        Activity activity = getParentActivity();
        if (activity != null) {
            activity.setTitle(title);
        }
    }

    public void startActivityForResult(final Intent intent, final int requestCode) {
        if (parentLayout != null) {
            parentLayout.startActivityForResult(intent, requestCode);
        }
    }

    public void dismissCurrentDialig() {
        if (visibleDialog == null) {
            return;
        }
        try {
            visibleDialog.dismiss();
            visibleDialog = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public boolean dismissDialogOnPause(Dialog dialog) {
        return true;
    }

    public boolean canBeginSlide() {
        return true;
    }

    public void onBeginSlide() {
        try {
            if (visibleDialog != null && visibleDialog.isShowing()) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (actionBar != null) {
            actionBar.onPause();
        }
    }

    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {

    }

    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {

    }

    protected void onBecomeFullyVisible() {
        AccessibilityManager mgr = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (mgr.isEnabled()) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                String title = actionBar.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    setParentActivityTitle(title);
                }
            }
        }
    }

    protected void onBecomeFullyHidden() {

    }

    protected AnimatorSet onCustomTransitionAnimation(boolean isOpen, final Runnable callback) {
        return null;
    }

    public void onLowMemory() {

    }

    public Dialog showDialog(Dialog dialog) {
        return showDialog(dialog, false, null);
    }

    public Dialog showDialog(Dialog dialog, Dialog.OnDismissListener onDismissListener) {
        return showDialog(dialog, false, onDismissListener);
    }

    public Dialog showDialog(Dialog dialog, boolean allowInTransition, final Dialog.OnDismissListener onDismissListener) {
        if (dialog == null || parentLayout == null || parentLayout.animationInProgress || parentLayout.startedTracking || !allowInTransition && parentLayout.checkTransitionAnimation()) {
            return null;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            visibleDialog = dialog;
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(dialog1 -> {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(dialog1);
                }
                onDialogDismiss(dialog);
                if (dialog == visibleDialog) {
                    visibleDialog = null;
                }
            });
            visibleDialog.show();
            return visibleDialog;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    protected void onDialogDismiss(Dialog dialog) {

    }

    public Dialog getVisibleDialog() {
        return visibleDialog;
    }

    public void setVisibleDialog(Dialog dialog) {
        visibleDialog = dialog;
    }

    public boolean extendActionMode(Menu menu) {
        return false;
    }

    public ThemeDescription[] getThemeDescriptions() {
        return new ThemeDescription[0];
    }

    public AccountInstance getAccountInstance() {
        return AccountInstance.getInstance(currentAccount);
    }

    protected ConnectionsManager getConnectionsManager() {
        return getAccountInstance().getConnectionsManager();
    }

    public TonController getTonController() {
        return getAccountInstance().getTonController();
    }

    public TonConnectController getTonConnectController() {
        return getAccountInstance().getTonController().getTonConnectController();
    }

    public AccountsStateManager getTonAccountStateManager () {
        return getAccountInstance().getTonController().getAccountsStateManager();
    }

    public NotificationCenter getNotificationCenter() {
        return getAccountInstance().getNotificationCenter();
    }

    /**/

    public void defaultErrorCallback (String text, TonApi.Error error) {
        AndroidUtilities.runOnUIThread(() -> {
            AlertsCreator.showSimpleAlert(this, LocaleController.getString("Wallet", R.string.Wallet), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + (error != null ? error.message : text));
        });
    }



    /**/

    private ActionBarLayout[] cameraScanLayout;

    public void openQrCodeReader (Callbacks.StringCallback callback) {
        CameraScanActivity cameraScanActivity = new CameraScanActivity();
        cameraScanActivity.setDelegate(callback);
        cameraScanLayout = showAsSheet(cameraScanActivity);
    }



    /**/

    private BiometricPromtHelper biometricPromtHelper;

    public BiometricPromtHelper getBiometricPromtHelper() {
        if (biometricPromtHelper == null) {
            biometricPromtHelper = new BiometricPromtHelper(this);
        }
        return biometricPromtHelper;
    }

    public void requestPasscodeOrBiometricAuth (String text, Callbacks.AuthCallback callback) {
        switch (getTonController().getKeyProtectionType()) {
            case TonController.KEY_PROTECTION_TYPE_BIOMETRIC: {
                getBiometricPromtHelper().promtWithCipher(getTonController().getCipherForDecrypt(), text, cipher ->
                    callback.run(new TonController.UserAuthInfo(null, cipher, null))
                );
                break;
            }
            case TonController.KEY_PROTECTION_TYPE_NONE: {
                PasscodeActivity passcodeActivity = new PasscodeActivity();
                passcodeActivity.setOnSuccessPasscodeDelegate(passcode -> {
                    callback.run(new TonController.UserAuthInfo(passcode, null, null));
                    passcodeActivity.finishFragment(true);
                });
                showAsSheet(passcodeActivity);
                break;
            }
        }
    }

    public void requestPasscodeActivity (Callbacks.StringCallback callback, boolean needCloseAfterSuccess) {
        PasscodeActivity passcodeActivity = new PasscodeActivity();
        passcodeActivity.setOnSuccessPasscodeDelegate(passcode -> {
            callback.run(passcode);
            if (needCloseAfterSuccess) {
                passcodeActivity.finishFragment(true);
            }
        });
        presentFragment(passcodeActivity);
    }


    /**/

    private Runnable onSheetDismissDelegate;

    private void setOnSheetDismissDelegate(Runnable onSheetDismissDelegate) {
        this.onSheetDismissDelegate = onSheetDismissDelegate;
    }

    public ActionBarLayout[] showAsSheet (BaseFragment fragment) {
        Context context = getParentActivity();
        if (context == null) {
            return null;
        }

        ActionBarLayout[] actionBarLayout = new ActionBarLayout[]{new ActionBarLayout(context)};
        BottomSheet bottomSheet = new BottomSheet(this, false) {
            {
                fragment.setOnSheetDismissDelegate(this::dismiss);
                actionBarLayout[0].init(new ArrayList<>());
                actionBarLayout[0].addFragmentToStack(fragment);
                actionBarLayout[0].showLastFragment();
                actionBarLayout[0].setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
                containerView = actionBarLayout[0];
                setApplyBottomPadding(false);
                setOnDismissListener(dialog -> fragment.onFragmentDestroy());
            }

            @Override
            protected boolean canDismissWithSwipe() {
                return false;
            }

            @Override
            public void onBackPressed() {
                if (actionBarLayout[0] == null || actionBarLayout[0].fragmentsStack.size() <= 1) {
                    super.onBackPressed();
                } else {
                    actionBarLayout[0].onBackPressed();
                }
            }

            @Override
            public void dismiss() {
                super.dismiss();
                actionBarLayout[0] = null;
            }
        };

        bottomSheet.show();
        return actionBarLayout;
    }






    private AlertDialog progressDialog;
    private int progressShowCounter = 0;

    protected void showProgressDialog () {
        progressShowCounter += 1;
        if (progressShowCounter > 1) return;

        progressDialog = new AlertDialog(getParentActivity(), 3);
        progressDialog.setCanCacnel(false);
        progressDialog.show();
    }

    protected void hideProgressDialog () {
        progressShowCounter -= 1;
        if (progressShowCounter < 0) {
            progressShowCounter = 0;
        }
        if (progressShowCounter != 0) return;

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
