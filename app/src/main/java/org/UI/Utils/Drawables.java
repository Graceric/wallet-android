/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 25/04/2015 at 09:10
 */
package org.UI.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.Components.RLottieDrawable;

// FIXME memory usage for all vector drawings
public class Drawables {
    public static void setAlpha (Drawable d, int alpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (d.getAlpha() != alpha) {
                d.setAlpha(alpha);
            }
        } else {
            d.setAlpha(alpha);
        }
    }

    public static void prepare (Drawable d, @Nullable Paint paint) {
        if (paint == null)
            return;
        int alpha = paint.getAlpha();
        ColorFilter filter = paint.getColorFilter();

        if (d instanceof BitmapDrawable) {
            BitmapDrawable b = (BitmapDrawable) d;
            if (b.getPaint().getColorFilter() != filter) {
                d.setColorFilter(filter);
            }
            b.setAlpha(alpha);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (filter != d.getColorFilter()) {
                    d.setColorFilter(filter);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (alpha != d.getAlpha()) {
                    d.setAlpha(alpha);
                }
            }
        }
    }

    public static void drawCentered (Canvas c, Drawable d, float cx, float cy, @Nullable Paint paint) {
        draw(c, d, cx - d.getMinimumWidth() / 2f, cy - d.getMinimumHeight() / 2f, paint);
    }

    public static void draw (Canvas c, Drawable d, float x, float y, @Nullable Paint paint) {
        if (d == null)
            return;
        Rect bounds = d.getBounds();
        int minWidth = d.getMinimumWidth();
        int minHeight = d.getMinimumHeight();
        if (bounds.top != 0 || bounds.left != 0 || bounds.right != minWidth || bounds.bottom != minHeight) {
            d.setBounds(0, 0, minWidth, minHeight);
        }
        prepare(d, paint);
        final int saveCount;
        final boolean needRestore = x != 0 || y != 0;
        if (needRestore) {
            saveCount = c.save();
            c.translate(x, y);
        } else {
            saveCount = -1;
        }
        d.draw(c);
        if (needRestore) {
            c.restoreToCount(saveCount);
        }
    }

    public static Drawable bitmapDrawable (Context context, Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return new BitmapDrawable(context.getResources(), bitmap);
        } else {
            return new BitmapDrawable(bitmap);
        }
    }

    public static Drawable get (int res) {
        return get(ApplicationLoader.applicationContext.getResources(), res);
    }

    public static Drawable get (Resources resources, int res) {
        Drawable d = load(resources, res);
        return d != null ? d.mutate() : null;
    }

    public static Bitmap toBitmap (Drawable d) {
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable) d).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return bitmap;
    }

    public static Drawable load (int res) {
        return load(ApplicationLoader.applicationContext.getResources(), res);
    }

    public static Drawable load (Resources resources, int res) {
        if (res == 0)
            return null;
        Drawable drawable = ResourcesCompat.getDrawable(resources, res, null);
        if (drawable == null)
            throw new Resources.NotFoundException("res == " + res);
        return drawable;
    }

    public static Bitmap getBitmap (int res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable drawable = get(res);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(), res);
        }
    }

    public static RLottieDrawable getGemDrawable (int sizeDp) {
        RLottieDrawable gem = new RLottieDrawable(R.raw.wallet_main, "" + R.raw.wallet_main, AndroidUtilities.dp(sizeDp), AndroidUtilities.dp(sizeDp), true);
        gem.setBounds(0, 0, AndroidUtilities.dp(sizeDp), AndroidUtilities.dp(sizeDp));
        return gem;
    }
}
