package com.example.undersoil.settings;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.R;
import com.example.undersoil.data.SessionManager;
import com.example.undersoil.data.db.GardenDatabase;
import com.example.undersoil.data.db.ScanLogDatabaseHelper;
import com.example.undersoil.data.db.SectorDatabaseHelper;

// Read-only profile: everything shown comes from the local session and databases, so it works offline.
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        SessionManager session = SessionManager.getInstance(this);
        String name = session.getUserName();
        String email = session.getUserEmail();
        String role = session.getUserRole();

        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvProfileRoleBadge)).setText(role);
        ((TextView) findViewById(R.id.tvDetailName)).setText(name);
        ((TextView) findViewById(R.id.tvDetailEmail)).setText(email.isEmpty() ? "Not available" : email);
        ((TextView) findViewById(R.id.tvDetailRole)).setText(role);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFarmOverview();
    }

    private void loadFarmOverview() {
        String userId = SessionManager.getInstance(this).getUserIdString();
        int crops = 0;
        int sectors = 0;
        long scans = 0;

        try {
            crops = GardenDatabase.Companion.getInstance(this).gardenDao().getAllSync(userId).size();
            sectors = SectorDatabaseHelper.getInstance(this).getAllSectors(userId).size();
            scans = ScanLogDatabaseHelper.getInstance(this).getLogCount(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ((TextView) findViewById(R.id.tvCropCount)).setText(String.valueOf(crops));
        ((TextView) findViewById(R.id.tvSectorCount)).setText(String.valueOf(sectors));
        ((TextView) findViewById(R.id.tvScanCount)).setText(String.valueOf(scans));
    }
}
