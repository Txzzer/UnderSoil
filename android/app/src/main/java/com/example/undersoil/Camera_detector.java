package com.example.undersoil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.undersoil.ml.ModelWithMetadata;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.util.List;
import java.util.Locale;

public class Camera_detector extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView ivCapturedImage;
    private TextView tvDiagnosisStatus;
    private TextView tvConfidenceText;
    private TextView tvClassLabel;
    private TextView tvRecommendation;
    private ProgressBar pbConfidence;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private int sampleToggleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_detector);

        initViews();
        setupCameraLauncher();
        setupGalleryLauncher();

        // Run initial classification on default sample image so screen loads with a working preview
        runTestSampleLeaf();
    }

    private void initViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        ivCapturedImage = findViewById(R.id.ivCapturedImage);
        tvDiagnosisStatus = findViewById(R.id.tvDiagnosisStatus);
        tvConfidenceText = findViewById(R.id.tvConfidenceText);
        tvClassLabel = findViewById(R.id.tvClassLabel);
        tvRecommendation = findViewById(R.id.tvRecommendation);
        pbConfidence = findViewById(R.id.pbConfidence);

        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnPickGallery = findViewById(R.id.btnPickGallery);
        Button btnTestSample = findViewById(R.id.btnTestSample);

        backButton.setOnClickListener(v -> finish());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnTestSample.setOnClickListener(v -> runTestSampleLeaf());
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap bitmap = getBitmapFromCameraResult(result.getData());
                        if (bitmap != null) {
                            ivCapturedImage.setImageBitmap(bitmap);
                            classifyLeafImage(bitmap);
                        } else {
                            Toast.makeText(this, "No image captured. Use 'Upload Image' to test model.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            if (bitmap != null) {
                                ivCapturedImage.setImageBitmap(bitmap);
                                classifyLeafImage(bitmap);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private Bitmap getBitmapFromCameraResult(Intent data) {
        if (data == null) return null;

        // 1. Try Uri from data.getData()
        if (data.getData() != null) {
            try {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } catch (Exception ignored) {
            }
        }

        // 2. Try Bitmap from extras
        Bundle extras = data.getExtras();
        if (extras != null) {
            Object bitmapObj = extras.get("data");
            if (bitmapObj instanceof Bitmap) {
                return (Bitmap) bitmapObj;
            }
        }

        return null;
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied. Use 'Upload Image' button to test model.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void runTestSampleLeaf() {
        sampleToggleCount++;
        boolean isHealthySample = (sampleToggleCount % 2 != 0);

        Bitmap sampleBitmap = createSampleLeafBitmap(isHealthySample);
        ivCapturedImage.setImageBitmap(sampleBitmap);

        classifyLeafImage(sampleBitmap);
    }

    private Bitmap createSampleLeafBitmap(boolean healthy) {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(healthy ? Color.parseColor("#E8F5E9") : Color.parseColor("#FFF3E0"));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(healthy ? Color.parseColor("#2E7D32") : Color.parseColor("#8D6E63"));
        canvas.drawCircle(128, 128, 90, paint);

        paint.setColor(healthy ? Color.parseColor("#4CAF50") : Color.parseColor("#D32F2F"));
        canvas.drawCircle(100, 100, 30, paint);
        canvas.drawCircle(150, 140, 25, paint);

        return bitmap;
    }

    private void classifyLeafImage(Bitmap bitmap) {
        try {
            ModelWithMetadata model = ModelWithMetadata.newInstance(this);

            TensorImage image = TensorImage.fromBitmap(bitmap);
            ModelWithMetadata.Outputs outputs = model.process(image);
            List<Category> probabilityList = outputs.getProbabilityAsCategoryList();

            Category topCategory = null;
            float maxScore = -1f;

            if (probabilityList != null && !probabilityList.isEmpty()) {
                for (Category category : probabilityList) {
                    if (category.getScore() > maxScore) {
                        maxScore = category.getScore();
                        topCategory = category;
                    }
                }
            }

            if (topCategory != null) {
                String label = topCategory.getLabel();
                float confidencePercent = maxScore * 100f;

                tvClassLabel.setText(String.format("Class: %s", label));
                tvConfidenceText.setText(String.format(Locale.US, "%.1f%% Confidence", confidencePercent));
                pbConfidence.setProgress((int) confidencePercent);

                boolean isHealthy = label.toLowerCase(Locale.US).contains("healthy");

                if (isHealthy) {
                    tvDiagnosisStatus.setText("HEALTHY 🌱");
                    tvDiagnosisStatus.setTextColor(Color.parseColor("#2E7D32"));
                    tvRecommendation.setText("Leaf foliage shows robust green coloration with no detected symptoms of blight or infection.");
                } else {
                    tvDiagnosisStatus.setText("DISEASE DETECTED ⚠️");
                    tvDiagnosisStatus.setTextColor(Color.parseColor("#D32F2F"));
                    tvRecommendation.setText(String.format("Symptoms of %s detected. Check watering, isolate infected leaves, and apply recommended crop treatment.", label));
                }
            }

            model.close();
        } catch (Exception e) {
            Toast.makeText(this, "ML Diagnosis Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
