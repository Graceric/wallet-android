package org.UI.Fragments.Main;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class WalletActivityListDecorator extends RecyclerView.ItemDecoration {
    @Override
    public void getItemOffsets (@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        final int position = parent.getChildAdapterPosition(view);
        final RecyclerView.Adapter<?> adapter = parent.getAdapter();
        final int itemsCount = adapter != null ? adapter.getItemCount() : 0;
        int bottom = 0;

        if (position == itemsCount - 1) {
            int n = parent.getChildCount();
            int totalHeight = 0;
            for (int i = 0; i < n; i++) {
                View v = parent.getChildAt(i);
                int pos = parent.getChildAdapterPosition(v);
                if (pos >= 2) {
                    totalHeight += v.getMeasuredHeight();
                }
            }
            bottom = Math.max(parent.getMeasuredHeight() - totalHeight - 1, 0);
        }

        outRect.set(0, 0, 0, bottom);
    }
}
