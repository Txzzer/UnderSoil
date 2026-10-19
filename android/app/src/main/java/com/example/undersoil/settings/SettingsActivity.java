package com.example.undersoil.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.sync.SyncRunner;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
            logoutButton.setOnClickListener(v -> logoutAfterBackup());
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    // Upload anything recorded offline while the token is still valid, then log out
    private void logoutAfterBackup() {
        AlertDialog progress = new MaterialAlertDialogBuilder(this)
                .setTitle("Logging out")
                .setMessage("Backing up your data…")
                .setCancelable(false)
                .show();

        executor.execute(() -> {
            boolean backedUp;
            try {
                backedUp = !SyncRunner.hasPendingUploads(this) || SyncRunner.uploadPending(this);
            } catch (Exception e) {
                backedUp = false;
            }

            boolean finalBackedUp = backedUp;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                progress.dismiss();
                if (finalBackedUp) {
                    SessionManager.getInstance(this).logoutUser(this);
                } else {
                    confirmLogoutWithoutBackup();
                }
            });
        });
    }

    private void confirmLogoutWithoutBackup() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Not everything is backed up")
                .setMessage("Some changes couldn't be uploaded, usually because there's no connection. "
                        + "They're kept on this phone and will upload the next time you log in here.\n\n"
                        + "Log out anyway?")
                .setPositiveButton("Log out", (d, w) -> SessionManager.getInstance(this).logoutUser(this))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
