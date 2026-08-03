package network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("api/v1/test")
    Call<String> testConnection();
}
