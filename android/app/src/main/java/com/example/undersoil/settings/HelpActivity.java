package com.example.undersoil.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undersoil.R;

public class HelpActivity extends AppCompatActivity {

    // Each step is { title, description }
    private static final String[][] SCAN_STEPS = {
            {"Open My Garden",
                    "From the Home screen tap \"Diagnose\", or tap the Garden tab in the bottom bar."},
            {"Add your crop",
                    "If your garden is empty, tap \"Add Plant\" (or the search icon), choose your crop and tap \"Add to My Garden\"."},
            {"Choose the plant to scan",
                    "In My Garden, tap the plant you want to check. This opens its Crop Logs."},
            {"Start a scan",
                    "Tap \"Scan Plant\" to open the AI Crop Health Detector."},
            {"Take or upload a photo",
                    "Tap \"Camera\" to photograph a single leaf, or \"Upload Image\" to pick one from your gallery. Allow camera access when asked. Use good lighting and fill the frame with the leaf."},
            {"Read your diagnosis",
                    "The result and AI confidence appear under Diagnosis. Tap \"Show Recommendations\" for treatment advice. Past scans are listed under Scan History in Crop Logs."}
    };

    private static final String[][] IOT_STEPS = {
            {"Open IoT Watering",
                    "From the Home screen tap \"IOT Watering\", or tap the IoT tab in the bottom bar."},
            {"Create a sector",
                    "First time? Tap \"Create new sector\", give it a name, assign crops from My Garden and tap \"Add new sector\"."},
            {"Open the dashboard",
                    "Tap a saved sector to open its IoT Dashboard."},
            {"Check the connection",
                    "The top card shows your device ID, the live status and when the data was last updated."},
            {"Read your sensors",
                    "The gauges show temperature and humidity, and the Flow Rate card shows how much water is flowing in L/min."},
            {"Control watering",
                    "Tap \"Turn Valve ON\" / \"OFF\" to control the water, or \"Schedule Watering\" to set a time. Recent valve actions are listed at the bottom."},
            {"Watch for alerts",
                    "If water flows while the valve is closed, a red \"Unintended flow detected\" banner appears. Check your lines for leaks. Alerts are also saved in the Alerts Log on the Home screen."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        populateSteps(findViewById(R.id.llScanSteps), SCAN_STEPS);
        populateSteps(findViewById(R.id.llIotSteps), IOT_STEPS);
    }

    private void populateSteps(LinearLayout container, String[][] steps) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < steps.length; i++) {
            View row = inflater.inflate(R.layout.item_help_step, container, false);
            ((TextView) row.findViewById(R.id.tvStepNumber)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.tvStepTitle)).setText(steps[i][0]);
            ((TextView) row.findViewById(R.id.tvStepBody)).setText(steps[i][1]);
            container.addView(row);
        }
    }
}
