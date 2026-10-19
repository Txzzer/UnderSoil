package com.example.undersoil.network;

import com.example.undersoil.data.db.GardenEntity;
import com.example.undersoil.data.model.IotAlert;
import com.example.undersoil.data.model.ScanLog;
import com.example.undersoil.data.model.Sector;
import com.example.undersoil.data.model.WateringSchedule;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/Valve/toggle")
    Call<Void> toggleValve(@Body ValveRequest request);

    @GET("api/v1/Valve/history")
    Call<List<ValveHistoryEntry>> getValveHistory();

    @GET("api/v1/Telemetry/latest")
    Call<SensorData> getLatestSensorData();

    // Offline sync: each endpoint takes a batch of unsynced rows
    @POST("api/sync/sectors")
    Call<Void> syncSectors(@Body List<Sector> sectors);

    @POST("api/sync/garden")
    Call<Void> syncGarden(@Body List<GardenEntity> gardenCrops);

    @POST("api/sync/garden/delete")
    Call<Void> deleteGardenPlants(@Body List<Integer> plantIds);

    @POST("api/sync/logs")
    Call<Void> syncScanLogs(@Body List<ScanLog> logs);

    @POST("api/sync/alerts")
    Call<Void> syncAlerts(@Body List<IotAlert> alerts);

    @POST("api/sync/schedules")
    Call<Void> syncSchedules(@Body List<WateringSchedule> schedules);

    // Restore: everything the server holds for the logged-in user
    @GET("api/sync/all")
    Call<UserDataResponse> getAllUserData();

    @POST("api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);
}
