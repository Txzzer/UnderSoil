package com.example.undersoil.scan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.undersoil.R;
import com.example.undersoil.data.db.ScanLogDatabaseHelper;
import com.example.undersoil.data.model.ScanLog;
import com.example.undersoil.data.sync.SyncManager;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.Locale;

public class LogResultActivity extends AppCompatActivity {

    public static final String EXTRA_LOG_ID = "EXTRA_LOG_ID";

    private String logId;
    private ScanLogDatabaseHelper dbHelper;

    private TextView titleText;
    private TextView tvCropName;
    private ImageView ivCropImage;
    private TextView tvLogDate;
    private TextView tvLogDiagnosis;
    private TextView tvLogConfidence;
    private TextInputEditText etLogNote;
    private Button btnSaveNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_result);
        applyWindowInsets();

        dbHelper = ScanLogDatabaseHelper.getInstance(this);

        Intent intent = getIntent();
        logId = (intent != null) ? intent.getStringExtra(EXTRA_LOG_ID) : null;

        initViews();

        ScanLog log = dbHelper.getLogById(logId);
        if (log == null) {
            Toast.makeText(this, "Scan log not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Only populate on first launch so an in-progress edit survives rotation
        bindLog(log, savedInstanceState == null);
    }

    /**
     * Targeting SDK 35 forces edge-to-edge on Android 15+, where adjustResize no longer shrinks
     * the window. Pad the root by the system bars and keyboard instead, so the scroll view
     * shrinks and keeps the focused note field visible above the keyboard.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.logResultRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        tvCropName = findViewById(R.id.tvCropName);
        ivCropImage = findViewById(R.id.ivCropImage);
        tvLogDate = findViewById(R.id.tvLogDate);
        tvLogDiagnosis = findViewById(R.id.tvLogDiagnosis);
        tvLogConfidence = findViewById(R.id.tvLogConfidence);
        etLogNote = findViewById(R.id.etLogNote);
        btnSaveNote = findViewById(R.id.btnSaveNote);

        backButton.setOnClickListener(v -> finish());
        btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void bindLog(ScanLog log, boolean populateNote) {
        String crop = (log.getCropName() != null) ? log.getCropName() : "Crop";
        titleText.setText(String.format("%s Log", crop));
        tvCropName.setText(crop);
        tvLogDate.setText(String.format("Date: %s", log.getDateStr()));
        tvLogDiagnosis.setText(String.format("Diagnosis: %s", log.getDiagnosis()));
        tvLogConfidence.setText(String.format(Locale.US, "Confidence Score - %.1f%%", log.getConfidence()));

        String imagePath = log.getImagePath();
        if (imagePath != null && new File(imagePath).exists()) {
            ivCropImage.setImageURI(Uri.fromFile(new File(imagePath)));
        }

        if (populateNote && log.getNotes() != null) {
            etLogNote.setText(log.getNotes());
        }
    }

    private void saveNote() {
        String note = (etLogNote.getText() != null) ? etLogNote.getText().toString() : "";

        if (dbHelper.updateLogNote(logId, note)) {
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
            SyncManager.scheduleSync(this);
        } else {
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }
}
