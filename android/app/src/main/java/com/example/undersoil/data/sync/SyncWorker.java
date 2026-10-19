package com.example.undersoil.data.sync;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;

public class SyncWorker extends Worker {

    public static final String CHANNEL_ID = "undersoil_sync_channel";
    public static final String CHANNEL_NAME = "UnderSoil Sync Notifications";
    private static final int NOTIFICATION_ID = 1001;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager sessionManager = SessionManager.getInstance(context);

        // Not logged in: leave everything queued locally until the next sync
        if (sessionManager.getToken() == null) {
            return Result.success();
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        createNotificationChannel(notificationManager);
        showNotification(notificationManager, "UnderSoil Syncing...", "Synchronizing offline data with the cloud", true);

        try {
            // Upload first so the download below can't resurrect older server copies of local edits
            SyncRunner.uploadPending(context);
            SyncRunner.downloadAll(context);

            showNotification(notificationManager, "UnderSoil Sync", "Sync Complete", false);
            return Result.success();

        } catch (SyncRunner.UnauthorizedException e) {
            sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
            if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
            return Result.success();

        } catch (Exception e) {
            showNotification(notificationManager, "UnderSoil Sync Failed", "Will retry when network connection is restored", false);
            return Result.retry();
        }
    }

    private void showNotification(NotificationManager manager, String title, String text, boolean ongoing) {
        if (manager == null) return;

        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing);
        try {
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private void createNotificationChannel(NotificationManager manager) {
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows status of background data synchronization");
        manager.createNotificationChannel(channel);
    }
}
