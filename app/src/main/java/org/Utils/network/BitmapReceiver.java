package org.Utils.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.Utils.Callbacks;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.ton.java.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BitmapReceiver {
    private static final HashMap<String, ReceiverInfo> callbacks = new HashMap<>();
    private String currentUrl;
    private Callbacks.BitmapCallback bitmapCallback;

    public @Nullable Bitmap receive (final String url, Callbacks.BitmapCallback bitmapCallback) {
        stop();
        if (TextUtils.isEmpty(url)) return null;

        this.currentUrl = url;
        this.bitmapCallback = bitmapCallback;
        ReceiverInfo info = callbacks.get(currentUrl);
        if (info == null) {
            info = new ReceiverInfo(url);
            callbacks.put(currentUrl, info);

            try {
                File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "tokens_cache");
                if (dir.exists()) {
                    File imageFile = new File(dir, info.urlHash + ".png");
                    if (imageFile.exists()) {
                        info.bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }

        }

        info.callbacks.add(this);
        if (info.bitmap != null) {
            return info.bitmap;
        } else if (!info.isLoading) {
            info.isLoading = true;
            ReceiverInfo finalInfo = info;
            BitmapFetch.fetch(currentUrl, bitmap -> {
                onBitmapReceive(finalInfo, bitmap);
            });
        }
        return null;
    }

    public void stop () {
        if (TextUtils.isEmpty(currentUrl)) return;
        ReceiverInfo info = callbacks.get(currentUrl);
        if (info == null) return;
        info.callbacks.remove(this);
    }

    public String getCurrentUrl () {
        return currentUrl;
    }

    private void onBitmapReceive (ReceiverInfo info, Bitmap bitmap) {
        info.isLoading = false;
        info.bitmap = bitmap;
        for (BitmapReceiver receiver : info.callbacks) {
            if (receiver.bitmapCallback != null) {
                receiver.bitmapCallback.run(bitmap);
            }
        }

        try {
            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "tokens_cache");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File imageFile = new File(dir, info.urlHash + ".png");
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static class ReceiverInfo {
        private final String url;
        private final String urlHash;
        private Bitmap bitmap = null;
        private final Set<BitmapReceiver> callbacks = new HashSet<>();
        boolean isLoading = false;

        private ReceiverInfo (String url) {
            this.url = url;
            this.urlHash = Utils.sha256(url);
        }
    }

    public static @Nullable Bitmap get (String url) {
        ReceiverInfo receiverInfo = callbacks.get(url);
        if (receiverInfo == null) return null;

        return receiverInfo.bitmap;
    }
}
