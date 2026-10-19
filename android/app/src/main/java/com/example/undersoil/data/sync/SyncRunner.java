package com.example.undersoil.data.sync;

import android.content.Context;

import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.GardenDao;
import com.example.undersoil.data.db.GardenDatabase;
import com.example.undersoil.data.db.GardenEntity;
import com.example.undersoil.data.db.IotAlertDatabaseHelper;
import com.example.undersoil.data.db.ScanLogDatabaseHelper;
import com.example.undersoil.data.db.SectorDatabaseHelper;
import com.example.undersoil.data.db.WateringScheduleDatabaseHelper;
import com.example.undersoil.data.model.IotAlert;
import com.example.undersoil.data.model.ScanLog;
import com.example.undersoil.data.model.Sector;
import com.example.undersoil.data.model.WateringSchedule;
import com.example.undersoil.network.ApiClient;
import com.example.undersoil.network.ApiService;
import com.example.undersoil.network.UserDataResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import retrofit2.Call;
import retrofit2.Response;

// Blocking sync operations shared by SyncWorker and logout. Call off the main thread.
public final class SyncRunner {

    /** The server rejected our token, so there's no point sending the remaining batches. */
    public static class UnauthorizedException extends Exception {
    }

    private SyncRunner() {
    }

    public static boolean hasPendingUploads(Context context) {
        String userId = SessionManager.getInstance(context).getUserIdString();
        return !PendingDeletes.getGardenPlants(context, userId).isEmpty()
                || !SectorDatabaseHelper.getInstance(context).getUnsyncedSectors().isEmpty()
                || !GardenDatabase.Companion.getInstance(context).gardenDao().getUnsyncedCrops().isEmpty()
                || !ScanLogDatabaseHelper.getInstance(context).getUnsyncedLogs().isEmpty()
                || !IotAlertDatabaseHelper.getInstance(context).getUnsyncedAlerts().isEmpty()
                || !WateringScheduleDatabaseHelper.getInstance(context).getUnsyncedSchedules().isEmpty();
    }

    /** Uploads every unsynced row. Returns true only if every batch was accepted. */
    public static boolean uploadPending(Context context) throws IOException, UnauthorizedException {
        ApiService api = ApiClient.getClient(context).create(ApiService.class);
        SectorDatabaseHelper sectors = SectorDatabaseHelper.getInstance(context);
        GardenDao garden = GardenDatabase.Companion.getInstance(context).gardenDao();
        ScanLogDatabaseHelper scanLogs = ScanLogDatabaseHelper.getInstance(context);
        IotAlertDatabaseHelper alerts = IotAlertDatabaseHelper.getInstance(context);
        WateringScheduleDatabaseHelper schedules = WateringScheduleDatabaseHelper.getInstance(context);

        String userId = SessionManager.getInstance(context).getUserIdString();
        List<Integer> deletedPlants = PendingDeletes.getGardenPlants(context, userId);
        boolean allOk = true;
        if (!deletedPlants.isEmpty()) {
            Response<Void> response = api.deleteGardenPlants(deletedPlants).execute();
            if (response.code() == 401) throw new UnauthorizedException();
            if (response.isSuccessful()) {
                PendingDeletes.clearGardenPlants(context, userId, deletedPlants);
            } else {
                allOk = false;
            }
        }

        allOk &= upload(sectors.getUnsyncedSectors(), api::syncSectors, s -> sectors.markSectorAsSynced(s.getId()));
        allOk &= upload(garden.getUnsyncedCrops(), api::syncGarden, c -> garden.markCropAsSynced(c.getId()));
        allOk &= upload(scanLogs.getUnsyncedLogs(), api::syncScanLogs, l -> scanLogs.markLogAsSynced(l.getId()));
        allOk &= upload(alerts.getUnsyncedAlerts(), api::syncAlerts, a -> alerts.markAlertAsSynced(a.getId()));
        allOk &= upload(schedules.getUnsyncedSchedules(), api::syncSchedules, s -> schedules.markScheduleAsSynced(s.getId()));
        return allOk;
    }

    /**
     * Restores everything the server holds for this user into the local databases.
     * Run after uploadPending so the server already has this phone's changes; local rows
     * that still have un-uploaded changes are never overwritten.
     */
    public static void downloadAll(Context context) throws IOException, UnauthorizedException {
        ApiService api = ApiClient.getClient(context).create(ApiService.class);
        Response<UserDataResponse> response = api.getAllUserData().execute();
        if (response.code() == 401) {
            throw new UnauthorizedException();
        }
        UserDataResponse data = response.body();
        if (!response.isSuccessful() || data == null) {
            throw new IOException("Download failed: HTTP " + response.code());
        }

        SectorDatabaseHelper sectors = SectorDatabaseHelper.getInstance(context);
        for (Sector sector : data.getSectors()) sectors.upsertFromServer(sector);

        // Skip plants deleted on this phone whose delete hasn't reached the server yet
        List<Integer> deletedPlants = PendingDeletes.getGardenPlants(
                context, SessionManager.getInstance(context).getUserIdString());
        List<GardenEntity> garden = new ArrayList<>();
        for (GardenEntity crop : data.getGarden()) {
            if (!deletedPlants.contains(crop.getPlantId())) garden.add(crop);
        }
        GardenDatabase.Companion.getInstance(context).gardenDao().insertAllFromServer(garden);

        ScanLogDatabaseHelper scanLogs = ScanLogDatabaseHelper.getInstance(context);
        for (ScanLog log : data.getScanLogs()) scanLogs.upsertFromServer(log);

        IotAlertDatabaseHelper alerts = IotAlertDatabaseHelper.getInstance(context);
        for (IotAlert alert : data.getAlerts()) alerts.upsertFromServer(alert);

        WateringScheduleDatabaseHelper schedules = WateringScheduleDatabaseHelper.getInstance(context);
        for (WateringSchedule schedule : data.getSchedules()) schedules.upsertFromServer(schedule);
    }

    private static <T> boolean upload(List<T> items, Function<List<T>, Call<Void>> request, Consumer<T> markSynced)
            throws IOException, UnauthorizedException {
        if (items.isEmpty()) return true;

        Response<Void> response = request.apply(items).execute();
        if (response.code() == 401) {
            throw new UnauthorizedException();
        }
        if (response.isSuccessful()) {
            for (T item : items) markSynced.accept(item);
            return true;
        }
        return false;
    }
}
