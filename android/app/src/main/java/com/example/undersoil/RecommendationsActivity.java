package com.example.undersoil;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class RecommendationsActivity extends AppCompatActivity {

    private static final String TAG = "RecommendationsActivity";

    /** Raw model label, e.g. "Tomato___Early_blight"; used as the key into recommendations.json. */
    public static final String EXTRA_DISEASE_NAME = "DISEASE_NAME";

    private static final String RECOMMENDATIONS_ASSET = "recommendations.json";

    private static final String FALLBACK_MESSAGE =
            "General care recommended: Ensure optimal watering, remove affected leaves, and monitor for further spread. "
                    + "Consult a local agricultural extension for specific chemical controls.";

    private static final String HEALTHY_MESSAGE =
            "No disease detected. Keep up regular care: consistent watering at the base of the plant, good air "
                    + "circulation, and routine checks of the leaves for early signs of disease.";

    private TextView tvDiseaseName;
    private TextView tvDiseaseDescription;
    private TextView tvTreatmentRecommendations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        ImageButton backButton = findViewById(R.id.backButton);
        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvDiseaseDescription = findViewById(R.id.tvDiseaseDescription);
        tvTreatmentRecommendations = findViewById(R.id.tvTreatmentRecommendations);

        backButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String diseaseKey = (intent != null) ? intent.getStringExtra(EXTRA_DISEASE_NAME) : null;
        displayRecommendations(diseaseKey);
    }

    private void displayRecommendations(String diseaseKey) {
        tvDiseaseName.setText(formatLabel(diseaseKey));

        if (diseaseKey == null || diseaseKey.trim().isEmpty()) {
            tvTreatmentRecommendations.setText(FALLBACK_MESSAGE);
            return;
        }

        // The JSON only covers diseases; healthy results get their own message instead of the disease fallback
        if (diseaseKey.toLowerCase(Locale.US).endsWith("healthy")) {
            tvTreatmentRecommendations.setText(HEALTHY_MESSAGE);
            return;
        }

        try {
            JSONObject root = new JSONObject(loadJSONFromAsset());

            if (root.has(diseaseKey)) {
                JSONObject entry = root.getJSONObject(diseaseKey);

                String crop = entry.optString("crop", "");
                String disease = entry.optString("disease", "");
                if (!disease.isEmpty()) {
                    tvDiseaseName.setText(crop.isEmpty() ? disease : String.format("%s • %s", crop, disease));
                }

                String cause = entry.optString("cause", "");
                if (!cause.isEmpty()) {
                    tvDiseaseDescription.setText(String.format("Cause: %s", cause));
                    tvDiseaseDescription.setVisibility(View.VISIBLE);
                }

                JSONArray steps = entry.optJSONArray("recommendations");
                if (steps != null && steps.length() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < steps.length(); i++) {
                        if (i > 0) sb.append("\n\n");
                        sb.append("• ").append(steps.getString(i));
                    }
                    tvTreatmentRecommendations.setText(sb.toString());
                } else {
                    tvTreatmentRecommendations.setText(FALLBACK_MESSAGE);
                }
            } else {
                Log.w(TAG, "No recommendations entry for key: " + diseaseKey);
                tvTreatmentRecommendations.setText(FALLBACK_MESSAGE);
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Failed to load " + RECOMMENDATIONS_ASSET, e);
            tvTreatmentRecommendations.setText(FALLBACK_MESSAGE);
        }
    }

    private String loadJSONFromAsset() throws IOException {
        try (InputStream is = getAssets().open(RECOMMENDATIONS_ASSET);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    /** "Pepper,_bell___Bacterial_spot" -> "Pepper bell • Bacterial spot" (used until/unless the JSON gives a name). */
    private String formatLabel(String rawLabel) {
        if (rawLabel == null || rawLabel.trim().isEmpty()) return "Unknown condition";
        if (!rawLabel.contains("___")) return rawLabel.replace("_", " ").trim();

        String[] parts = rawLabel.split("___", 2);
        String plant = parts[0].replace("_", " ").replace(",", "").trim();
        String condition = parts[1].replace("_", " ").trim();
        return condition.isEmpty() ? plant : String.format("%s • %s", plant, condition);
    }
}
