package network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
public interface ApiService {


    @POST("api/v1/Valve/toggle")
    Call<Void> toggleValve(@Body ValveRequest request);

    @GET("api/v1/telemetry/latest")
    Call<TelemetryReading> getLatestTelemetry();
}
