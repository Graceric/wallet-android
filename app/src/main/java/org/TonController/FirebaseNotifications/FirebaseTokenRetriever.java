package org.TonController.FirebaseNotifications;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;

import org.Utils.Callbacks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FirebaseTokenRetriever {
    public interface ErrorRegisterCallback {
        void onError (@NonNull String errorKey, @Nullable Throwable e);
    }

    public static void fetchDeviceToken (Callbacks.StringCallback callback, ErrorRegisterCallback errorRegisterCallback) {
        try {
            Log.i(FirebaseListenerService.TAG, "FirebaseMessaging: requesting token...");
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                Log.i(FirebaseListenerService.TAG, "FirebaseMessaging: successfully fetched token: " + token);
                callback.run(token);
            }).addOnFailureListener(e -> {
                String errorName = extractFirebaseErrorName(e);
                Log.e(FirebaseListenerService.TAG, "FirebaseMessaging: token fetch failed with remote error: " + errorName);
                if (errorRegisterCallback != null) {
                    errorRegisterCallback.onError(errorName, e);
                }
            });
        } catch (Throwable e) {
            Log.e(FirebaseListenerService.TAG, "FirebaseMessaging: token fetch failed with error: ", e);
            if (errorRegisterCallback != null) {
                errorRegisterCallback.onError("FIREBASE_REQUEST_ERROR", e);
            }
        }
    }

    private static String extractFirebaseErrorName (Throwable e) {
        String message = e.getMessage();
        if (!TextUtils.isEmpty(message)) {
            Matcher matcher = Pattern.compile("(?<=: )[A-Z_]+$").matcher(message);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return e.getClass().getSimpleName();
    }
}
