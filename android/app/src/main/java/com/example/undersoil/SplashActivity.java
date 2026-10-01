package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.data.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(SplashActivity.this, HomeScreenActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(SplashActivity.this, Log_in.class);
            startActivity(intent);
        }
        finish();
    }
}
