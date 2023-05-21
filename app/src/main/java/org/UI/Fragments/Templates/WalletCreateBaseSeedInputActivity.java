package org.UI.Fragments.Templates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class WalletCreateBaseSeedInputActivity extends WalletCreateBaseScrollActivity {
    protected NumericEditText[] editTexts;
    protected RecyclerListView hintListView;
    protected LinearLayoutManager hintLayoutManager;

    protected ActionBarPopupWindow hintPopupWindow;
    protected ActionBarPopupWindow.ActionBarPopupWindowLayout hintPopupLayout;
    protected HintAdapter hintAdapter;

    private NumericEditText hintEditText;
    protected String[] hintWords;


    private boolean globalIgnoreTextChange;
    protected int maxEditNumberWidth;


    protected void hideHint () {
        if (hintPopupWindow != null && hintPopupWindow.isShowing()) {
            hintPopupWindow.dismiss();
        }
    }

    @Override
    public boolean onFragmentCreate () {
        hintWords = getTonController().getHintWords();
        if (hintWords == null) {
            return false;
        }
        return super.onFragmentCreate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View createView (Context context) {
        super.createView(context);

        hintPopupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context);
        hintPopupLayout.setAnimationEnabled(false);
        hintPopupLayout.setOnTouchListener(new View.OnTouchListener() {
            private final Rect popupRect = new Rect();
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (hintPopupWindow != null && hintPopupWindow.isShowing()) {
                        v.getHitRect(popupRect);
                        if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                            hintPopupWindow.dismiss();
                        }
                    }
                }
                return false;
            }
        });
        hintPopupLayout.setDispatchKeyEventListener(keyEvent -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && hintPopupWindow != null && hintPopupWindow.isShowing()) {
                hintPopupWindow.dismiss();
            }
        });
        hintPopupLayout.setShowedFromBotton(false);

        hintListView = new RecyclerListView(context);
        hintListView.setAdapter(hintAdapter = new HintAdapter(context));
        hintListView.setPadding(AndroidUtilities.dp(9), 0, AndroidUtilities.dp(9), 0);
        hintListView.setLayoutManager(hintLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        hintListView.setClipToPadding(false);
        hintPopupLayout.addView(hintListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
        hintListView.setOnItemClickListener((view, position) -> {
            hintEditText.setText(hintAdapter.getItem(position));
            hintPopupWindow.dismiss();
        });

        hintPopupWindow = new ActionBarPopupWindow(hintPopupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        hintPopupWindow.setAnimationEnabled(false);
        hintPopupWindow.setAnimationStyle(R.style.PopupAnimation);
        hintPopupWindow.setClippingEnabled(true);
        hintPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        hintPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        hintPopupWindow.getContentView().setFocusableInTouchMode(true);
        hintPopupWindow.setFocusable(false);

        return fragmentView;
    }

    @Override
    public void onResume () {
        super.onResume();
        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
        fragmentView.requestLayout();
    }

    @Override
    public void onPause () {
        super.onPause();
        hideHint();
    }

    @Override
    public void onFragmentDestroy () {
        super.onFragmentDestroy();
        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
    }

    @Override
    public void onBeginSlide () {
        super.onBeginSlide();
        hideHint();
    }

    @Override
    protected void onTransitionAnimationEnd (boolean isOpen, boolean backward) {
        super.onTransitionAnimationEnd(isOpen, backward);
        if (!isOpen) return;
        editTexts[0].editText.requestFocus();
      //  AndroidUtilities.showKeyboard(editTexts[0].editText);
    }

    @Override
    protected void onTransitionAnimationStart (boolean isOpen, boolean backward) {
        hideHint();
    }

    protected boolean checkEditTexts () {
        if (editTexts == null) {
            return true;
        }
        for (NumericEditText editText : editTexts) {
            if (editText.length() == 0) {
                editText.editText.clearFocus();
                editText.editText.requestFocus();
                AndroidUtilities.shakeAndVibrateView(editText, 2, 0);
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean needSetFlagSecure () {
        return true;
    }

    protected class HintAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;
        private final int[] location = new int[2];
        private Runnable searchRunnable;
        private ArrayList<String> searchResult = new ArrayList<>();

        public HintAdapter (Context c) {
            context = c;
        }

        public void searchHintsFor (NumericEditText editText) {
            String text = editText.getText().toString();
            if (text.length() == 0) {
                if (hintPopupWindow.isShowing()) {
                    hintPopupWindow.dismiss();
                }
                return;
            }
            if (searchRunnable != null) {
                Utilities.searchQueue.cancelRunnable(searchRunnable);
                searchRunnable = null;
            }
            Utilities.searchQueue.postRunnable(searchRunnable = () -> {
                ArrayList<String> newSearchResults = new ArrayList<>();
                for (String hintWord : hintWords) {
                    if (hintWord.startsWith(text)) {
                        newSearchResults.add(hintWord);
                    }
                }
                if (newSearchResults.size() == 1 && newSearchResults.get(0).equals(text)) {
                    newSearchResults.clear();
                }
                AndroidUtilities.runOnUIThread(() -> {
                    searchRunnable = null;
                    searchResult = newSearchResults;
                    notifyDataSetChanged();
                    if (searchResult.isEmpty()) {
                        hideHint();
                    } else {
                        if (hintEditText != editText || !hintPopupWindow.isShowing()) {
                            hintPopupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
                            editText.getLocationInWindow(location);
                            hintLayoutManager.scrollToPositionWithOffset(0, 10000);
                            hintPopupWindow.showAtLocation(fragmentView, Gravity.LEFT | Gravity.TOP, location[0] - AndroidUtilities.dp(48), location[1] - AndroidUtilities.dp(48 + 16));
                        }
                        hintEditText = editText;
                    }
                });
            }, 200);
        }

        public String getItem (int position) {
            return searchResult.get(position);
        }

        @Override
        public boolean isEnabled (RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new RecyclerListView.Holder(textView);
        }

        @Override
        public void onBindViewHolder (RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setPadding(AndroidUtilities.dp(9), 0, AndroidUtilities.dp(9), 0);
            textView.setText(searchResult.get(position));
        }

        @Override
        public int getItemViewType (int position) {
            return 0;
        }

        @Override
        public int getItemCount () {
            return searchResult.size();
        }
    }

    protected class NumericEditText extends FrameLayout {

        private final ImageView deleteImageView;
        protected EditTextBoldCursor editText;
        private final TextPaint numericPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private String number;
        private int numberWidth;
        private boolean ignoreSearch;

        public NumericEditText (Context context, int number, String numberStr) {
            super(context);
            setWillNotDraw(false);
            numericPaint.setTextSize(AndroidUtilities.dp(15));

            editText = new EditTextBoldCursor(context);
            editText.setTag(number);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            setNumber(numberStr);
            editText.setBackgroundDrawable(Theme.createEditTextDrawable(context, false));
            editText.setPadding(AndroidUtilities.dp(31), AndroidUtilities.dp(2), AndroidUtilities.dp(30), 0);
            editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            editText.setCursorWidth(1.5f);
            editText.setMaxLines(1);
            editText.setLines(1);
            editText.setSingleLine(true);
            editText.setImeOptions((number != editTexts.length - 1 ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE) | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
            editText.setCursorSize(AndroidUtilities.dp(20));
            editText.setGravity(Gravity.LEFT);
            addView(editText, LayoutHelper.createFrame(200, 36));
            editText.setOnEditorActionListener((textView, i, keyEvent) -> {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    int num = (Integer) textView.getTag();
                    if (num < editTexts.length - 1) {
                        editTexts[num + 1].editText.requestFocus();
                        editTexts[num + 1].editText.setSelection(editTexts[num + 1].length());
                    }
                    hideHint();
                    return true;
                } else if (i == EditorInfo.IME_ACTION_DONE) {
                    buttonTextView.callOnClick();
                    return true;
                }
                return false;
            });
            editText.addTextChangedListener(new TextWatcher() {

                private boolean ignoreTextChange;
                private boolean isPaste;

                @Override
                public void beforeTextChanged (CharSequence s, int start, int count, int after) {
                    isPaste = after > count && after > 40;
                }

                @Override
                public void onTextChanged (CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged (Editable s) {
                    if (ignoreTextChange || globalIgnoreTextChange) {
                        return;
                    }
                    ignoreTextChange = true;
                    if (isPaste) {
                        globalIgnoreTextChange = true;
                        try {
                            String[] args = s.toString().split("\n");
                            if (args.length == 24) {
                                for (int a = 0; a < 24; a++) {
                                    editTexts[a].editText.setText(args[a].toLowerCase());
                                }
                            }
                            editTexts[23].editText.requestFocus();
                            return;
                        } catch (Exception e) {
                            FileLog.e(e);
                        } finally {
                            globalIgnoreTextChange = false;
                        }
                    }
                    s.replace(0, s.length(), s.toString().toLowerCase().trim());
                    for (int a = 0; a < s.length(); a++) {
                        char ch = s.charAt(a);
                        if (!(ch >= 'a' && ch <= 'z')) {
                            s.replace(a, a + 1, "");
                            a--;
                        }
                    }
                    ignoreTextChange = false;
                    updateClearButton();
                    if (hintAdapter != null && !ignoreSearch) {
                        hintAdapter.searchHintsFor(WalletCreateBaseSeedInputActivity.NumericEditText.this);
                    }
                }
            });
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                updateClearButton();
                hideHint();
            });

            deleteImageView = new ImageView(context);
            deleteImageView.setFocusable(false);
            deleteImageView.setScaleType(ImageView.ScaleType.CENTER);
            deleteImageView.setImageResource(R.drawable.miniplayer_close);
            deleteImageView.setAlpha(0.0f);
            deleteImageView.setScaleX(0.0f);
            deleteImageView.setScaleY(0.0f);
            deleteImageView.setRotation(45);
            deleteImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText7), PorterDuff.Mode.MULTIPLY));
            deleteImageView.setContentDescription(LocaleController.getString("ClearButton", R.string.ClearButton));
            addView(deleteImageView, LayoutHelper.createFrame(30, 30, Gravity.RIGHT | Gravity.TOP));
            deleteImageView.setOnClickListener(v -> {
                if (deleteImageView.getAlpha() != 1.0f) {
                    return;
                }
                editText.setText("");
            });
        }

        private void updateClearButton () {
            boolean show = editText.length() > 0 && editText.hasFocus();
            boolean visible = deleteImageView.getTag() != null;
            if (show != visible) {
                deleteImageView.setTag(show ? 1 : null);
                deleteImageView.animate().alpha(show ? 1.0f : 0.0f).scaleX(show ? 1.0f : 0.0f).scaleY(show ? 1.0f : 0.0f).rotation(show ? 0 : 45).setDuration(150).start();
            }
        }

        public int length () {
            return editText.length();
        }

        public Editable getText () {
            return editText.getText();
        }

        public void setNumber (String value) {
            number = value;
            numberWidth = (int) Math.ceil(numericPaint.measureText(number));
            maxEditNumberWidth = Math.max(maxEditNumberWidth, numberWidth);
        }

        public void setText (CharSequence text) {
            ignoreSearch = true;
            editText.setText(text);
            editText.setSelection(editText.length());
            ignoreSearch = false;
            int num = (Integer) editText.getTag();
            if (num < editTexts.length - 1) {
                editTexts[num + 1].editText.requestFocus();
                editTexts[num + 1].editText.setSelection(editTexts[num + 1].length());
            }
        }

        @Override
        protected void onDraw (Canvas canvas) {
            super.onDraw(canvas);
            if (number != null) {
                numericPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
                canvas.drawText(number, (maxEditNumberWidth - numberWidth) / 2.0f, AndroidUtilities.dp(20), numericPaint);
            }
        }
    }
}
