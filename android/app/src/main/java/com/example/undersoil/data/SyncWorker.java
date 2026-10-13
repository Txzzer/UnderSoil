package com.example.undersoil.data;

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

import java.util.List;

import network.ApiClient;
import network.ApiService;
import retrofit2.Response;

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

        // Skip sync if session is invalid or token missing; keep local data queued
        if (!sessionManager.isLoggedIn() || sessionManager.getToken() == null) {
            return Result.success();
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(notificationManager);

        NotificationCompat.Builder startingNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("UnderSoil Syncing...")
                .setContentText("Synchronizing offline data with the cloud")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        notifySafely(notificationManager, NOTIFICATION_ID, startingNotification, context);

        try {
            ApiService apiService = ApiClient.getClient(context).create(ApiService.class);

            // 1. Sync Unsynced Sectors
            List<Sector> unsyncedSectors = SectorDatabaseHelper.getInstance(context).getUnsyncedSectors();
            if (!unsyncedSectors.isEmpty()) {
                Response<Void> sectorResponse = apiService.syncSectors(unsyncedSectors).execute();
                if (sectorResponse.code() == 401) {
                    sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
                    return Result.success();
                }
                if (sectorResponse.isSuccessful()) {
                    for (Sector sector : unsyncedSectors) {
                        SectorDatabaseHelper.getInstance(context).markSectorAsSynced(sector.getId());
                    }
                }
            }

            // 2. Sync Unsynced Garden Crops
            List<GardenEntity> unsyncedCrops = GardenDatabase.Companion.getInstance(context).gardenDao().getUnsyncedCrops();
            if (!unsyncedCrops.isEmpty()) {
                Response<Void> cropResponse = apiService.syncGarden(unsyncedCrops).execute();
                if (cropResponse.code() == 401) {
                    sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
                    return Result.success();
                }
                if (cropResponse.isSuccessful()) {
                    for (GardenEntity crop : unsyncedCrops) {
                        GardenDatabase.Companion.getInstance(context).gardenDao().markCropAsSynced(crop.getId());
                    }
                }
            }

            // 3. Sync Unsynced Scan Logs
            List<ScanLog> unsyncedLogs = ScanLogDatabaseHelper.getInstance(context).getUnsyncedLogs();
            if (!unsyncedLogs.isEmpty()) {
                Response<Void> logResponse = apiService.syncScanLogs(unsyncedLogs).execute();
                if (logResponse.code() == 401) {
                    sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
                    return Result.success();
                }
                if (logResponse.isSuccessful()) {
                    for (ScanLog log : unsyncedLogs) {
                        ScanLogDatabaseHelper.getInstance(context).markLogAsSynced(log.getId());
                    }
                }
            }

            // 4. Sync Unsynced IoT Alerts
            List<IotAlert> unsyncedAlerts = IotAlertDatabaseHelper.getInstance(context).getUnsyncedAlerts();
            if (!unsyncedAlerts.isEmpty()) {
                Response<Void> alertResponse = apiService.syncAlerts(unsyncedAlerts).execute();
                if (alertResponse.code() == 401) {
                    sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
                    return Result.success();
                }
                if (alertResponse.isSuccessful()) {
                    for (IotAlert alert : unsyncedAlerts) {
                        IotAlertDatabaseHelper.getInstance(context).markAlertAsSynced(alert.getId());
                    }
                }
            }

            NotificationCompat.Builder completeNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("UnderSoil Sync")
                    .setContentText("Sync Complete")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(false)
                    .setAutoCancel(true);

            notifySafely(notificationManager, NOTIFICATION_ID, completeNotification, context);

            return Result.success();

        } catch (Exception e) {
            NotificationCompat.Builder errorNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("UnderSoil Sync Failed")
                    .setContentText("Will retry when network connection is restored")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(false)
                    .setAutoCancel(true);

            notifySafely(notificationManager, NOTIFICATION_ID, errorNotification, context);

            return Result.retry();
        }
    }

    private void notifySafely(NotificationManager manager, int id, NotificationCompat.Builder builder, Context context) {
        if (manager == null || builder == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            manager.notify(id, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Shows status of background data synchronization");
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
