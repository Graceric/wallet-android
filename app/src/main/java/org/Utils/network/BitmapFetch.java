package org.Utils.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.Utils.Callbacks;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class BitmapFetch extends AsyncTask<Void, Void, Bitmap> {

    private final String currentUrl;
    private final Callbacks.BitmapCallback onFinishCallback;

    public static void fetch (String url, Callbacks.BitmapCallback callback) {
        new BitmapFetch(Utilities.fixUrl(url), callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
    }

    private BitmapFetch(String url, Callbacks.BitmapCallback callback) {
        super();
        currentUrl = url;
        onFinishCallback = callback;
    }

    protected Bitmap doInBackground(Void... voids) {
        InputStream httpConnectionStream = null;
        try {
            URL downloadUrl = new URL(currentUrl);
            URLConnection httpConnection = downloadUrl.openConnection();
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
            httpConnection.setConnectTimeout(1000);
            httpConnection.setReadTimeout(2000);
            httpConnection.connect();
            httpConnectionStream = httpConnection.getInputStream();

            return BitmapFactory.decodeStream(httpConnectionStream);
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                if (httpConnectionStream != null) {
                    httpConnectionStream.close();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Bitmap result) {
        AndroidUtilities.runOnUIThread(() -> {
            if (onFinishCallback != null) {
                onFinishCallback.run(result);
            }
        });
    }
}