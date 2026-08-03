package com.example.undersoil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import network.ApiClient;
import network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button testApiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testApiButton = findViewById(R.id.testApiButton);

        testApiButton.setOnClickListener(view -> {

            ApiService api = ApiClient.getClient()
                    .create(ApiService.class);

            api.testConnection().enqueue(new Callback<String>() {

                @Override
                public void onResponse(Call<String> call, Response<String> response) {

                    Toast.makeText(
                            MainActivity.this,
                            "Success",
                            Toast.LENGTH_SHORT
                    ).show();

                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                    Toast.makeText(
                            MainActivity.this,
                            "Failed: " + t.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                }

            });

        });
    }
}