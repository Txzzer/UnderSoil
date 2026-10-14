package network;

import com.example.undersoil.data.GardenEntity;
import com.example.undersoil.data.ScanLog;
import com.example.undersoil.data.Sector;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/Valve/toggle")
    Call<Void> toggleValve(@Body ValveRequest request);

    @GET("api/v1/Telemetry/latest")
    Call<SensorData> getLatestSensorData();

    // Offline-First Sync Endpoints (Batch-upload List<T>)
    @POST("api/sync/sectors")
    Call<Void> syncSectors(@Body List<Sector> sectors);

    @POST("api/sync/garden")
    Call<Void> syncGarden(@Body List<GardenEntity> gardenCrops);

    @POST("api/sync/logs")
    Call<Void> syncScanLogs(@Body List<ScanLog> logs);

    @POST("api/sync/alerts")
    Call<Void> syncAlerts(@Body List<com.example.undersoil.data.IotAlert> alerts);

    @POST("api/sync/schedules")
    Call<Void> syncSchedules(@Body List<com.example.undersoil.data.WateringSchedule> schedules);

    @POST("api/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);
}
