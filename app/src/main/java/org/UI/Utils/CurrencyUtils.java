package org.UI.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.style.ImageSpan;

import org.TonController.Data.Jettons.RootJettonContract;
import org.Utils.network.BitmapReceiver;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;

public class CurrencyUtils {
    public static ImageSpan createSpan (int size) {
        return new ImageSpan(Drawables.getGemDrawable(size), Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ImageSpan.ALIGN_CENTER: ImageSpan.ALIGN_BASELINE);
    }

    public static ImageSpan createSpan (int size, RootJettonContract contract) {
        if (contract != null) {
            Bitmap b = BitmapReceiver.get(contract.content.imageUrl);
            return createSpan(size, b);
        }
        return createSpan(size);
    }

    public static ImageSpan createSpan (int size, Bitmap bitmap) {
        Path p = new Path();
        p.reset();
        p.addCircle(AndroidUtilities.dp(size) / 2f, AndroidUtilities.dp(size) / 2f, AndroidUtilities.dp(size) / 2f, Path.Direction.CW);

        BitmapDrawable d = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), bitmap) {
            @Override
            public void draw (Canvas canvas) {
                canvas.save();
                canvas.clipPath(p);
                super.draw(canvas);
                canvas.restore();
            }
        };
        d.setBounds(0, 0, AndroidUtilities.dp(size), AndroidUtilities.dp(size));
        return new ImageSpan(d, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ImageSpan.ALIGN_CENTER: ImageSpan.ALIGN_BASELINE);
    }
}