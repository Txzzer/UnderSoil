package com.example.undersoil.weather;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Open-Meteo (https://open-meteo.com): free, no API key. Kept separate from ApiClient,
// which is bound to the UnderSoil backend and attaches the user's JWT.
public interface WeatherService {

    String BASE_URL = "https://api.open-meteo.com/";
    String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,weather_code,is_day";

    @GET("v1/forecast")
    Call<WeatherResponse> getCurrentWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String currentFields,
            @Query("timezone") String timezone
    );

    static WeatherService get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final WeatherService INSTANCE = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherService.class);

        private Holder() {
        }
    }
}
