package network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface ApiService {

    @POST("api/v1/telemetry/ingest")
    Call<String> sendTelemetry(@Body TelemetryReading reading);
}
