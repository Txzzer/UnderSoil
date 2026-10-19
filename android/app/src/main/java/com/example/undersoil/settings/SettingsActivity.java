package com.example.undersoil.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        View viewProfileButton = findViewById(R.id.viewProfileButton);
        if (viewProfileButton != null) {
            viewProfileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        View helpButton = findViewById(R.id.helpButton);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));
        }

        View faqButton = findViewById(R.id.faqButton);
        if (faqButton != null) {
            faqButton.setOnClickListener(v -> startActivity(new Intent(this, FaqActivity.class)));
        }

        View logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                SessionManager.getInstance(this).logoutUser(this);
            });
        }
    }
}
