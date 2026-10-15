package com.example.undersoil.network;

import android.content.Context;
import android.content.Intent;

import com.example.undersoil.auth.LoginActivity;
import com.example.undersoil.data.SessionManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.1.52:5196/";

    private static Retrofit retrofit;

    public static synchronized Retrofit getClient(Context context) {
        if (retrofit == null && context != null) {
            final Context appContext = context.getApplicationContext();

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    String path = originalRequest.url().encodedPath();

                    if (path.contains("api/auth/login") || path.contains("api/auth/register")) {
                        return chain.proceed(originalRequest);
                    }

                    SessionManager sessionManager = SessionManager.getInstance(appContext);
                    String token = sessionManager.getToken();

                    Request.Builder builder = originalRequest.newBuilder();
                    if (token != null && !token.trim().isEmpty()) {
                        builder.header("Authorization", "Bearer " + token.trim());
                    }

                    Response response = chain.proceed(builder.build());

                    if (response.code() == 401 && !path.contains("api/auth/")) {
                        sessionManager.clearSessionOnly(SessionManager.InvalidReason.EXPIRED);

                        Intent intent = new Intent(appContext, LoginActivity.class);
                        intent.putExtra("session_expired", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        appContext.startActivity(intent);
                    }

                    return response;
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(SensorData.class, new SensorDataDeserializer())
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            throw new IllegalStateException("ApiClient has not been initialized with Context. Call ApiClient.getClient(context) first.");
        }
        return retrofit;
    }
}
