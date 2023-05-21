package org.TonController.FirebaseNotifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.TonController.TonController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;

public class FirebaseListenerService extends FirebaseMessagingService {

    public static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "transactions";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        AndroidUtilities.runOnUIThread(() -> {
            TonController.getInstance(Utilities.selectedAccount).onUpdateDeviceToken(token);
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        try {
            // Обрабатываем полученные данные уведомления
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification != null) {
                String title = notification.getTitle();
                String body = notification.getBody();

                // Отображаем уведомление
                showNotification(title, body);
            }

            // Дополнительная обработка данных сообщения
            if (remoteMessage.getData() != null) {
                // Обработка данных сообщения
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

    }

    private void showNotification(String title, String body) {
        // Создаем канал уведомлений (требуется для версии Android 8.0 и выше)
        createNotificationChannel();

        // Создаем построитель уведомления
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        // Отображаем уведомление с использованием NotificationManagerCompat
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {
        // Проверяем версию Android, создаем канал уведомлений только для версии Android 8.0 (Oreo) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Channel";
            String description = "My Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
