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
import com.example.undersoil.data.db.GardenDao;
import com.example.undersoil.data.db.GardenDatabase;
import com.example.undersoil.data.db.IotAlertDatabaseHelper;
import com.example.undersoil.data.db.ScanLogDatabaseHelper;
import com.example.undersoil.data.db.SectorDatabaseHelper;
import com.example.undersoil.data.db.WateringScheduleDatabaseHelper;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import retrofit2.Call;
import retrofit2.Response;

public class SyncWorker extends Worker {

    public static final String CHANNEL_ID = "undersoil_sync_channel";
    public static final String CHANNEL_NAME = "UnderSoil Sync Notifications";
    private static final int NOTIFICATION_ID = 1001;

    /** The server rejected our token, so there's no point sending the remaining batches. */
    private static class UnauthorizedException extends Exception {
    }

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
            ApiService api = ApiClient.getClient(context).create(ApiService.class);
            SectorDatabaseHelper sectors = SectorDatabaseHelper.getInstance(context);
            GardenDao garden = GardenDatabase.Companion.getInstance(context).gardenDao();
            ScanLogDatabaseHelper scanLogs = ScanLogDatabaseHelper.getInstance(context);
            IotAlertDatabaseHelper alerts = IotAlertDatabaseHelper.getInstance(context);
            WateringScheduleDatabaseHelper schedules = WateringScheduleDatabaseHelper.getInstance(context);

            upload(sectors.getUnsyncedSectors(), api::syncSectors, s -> sectors.markSectorAsSynced(s.getId()));
            upload(garden.getUnsyncedCrops(), api::syncGarden, c -> garden.markCropAsSynced(c.getId()));
            upload(scanLogs.getUnsyncedLogs(), api::syncScanLogs, l -> scanLogs.markLogAsSynced(l.getId()));
            upload(alerts.getUnsyncedAlerts(), api::syncAlerts, a -> alerts.markAlertAsSynced(a.getId()));
            upload(schedules.getUnsyncedSchedules(), api::syncSchedules, s -> schedules.markScheduleAsSynced(s.getId()));

            showNotification(notificationManager, "UnderSoil Sync", "Sync Complete", false);
            return Result.success();

        } catch (UnauthorizedException e) {
            sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);
            if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
            return Result.success();

        } catch (Exception e) {
            showNotification(notificationManager, "UnderSoil Sync Failed", "Will retry when network connection is restored", false);
            return Result.retry();
        }
    }

    private <T> void upload(List<T> items, Function<List<T>, Call<Void>> request, Consumer<T> markSynced)
            throws IOException, UnauthorizedException {
        if (items.isEmpty()) return;

        Response<Void> response = request.apply(items).execute();
        if (response.code() == 401) {
            throw new UnauthorizedException();
        }
        if (response.isSuccessful()) {
            for (T item : items) markSynced.accept(item);
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
