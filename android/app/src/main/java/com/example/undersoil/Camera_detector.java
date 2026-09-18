package com.example.undersoil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.metadata.MetadataExtractor;

import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

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

    private Interpreter tfliteInterpreter;
    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_detector);

        initViews();
        setupCameraLauncher();
        setupGalleryLauncher();
        
        loadTfliteModel();
    }

    private void loadTfliteModel() {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, "agri_model_ready.tflite");
            Interpreter.Options options = new Interpreter.Options();
            tfliteInterpreter = new Interpreter(tfliteModel, options);

            // Extract labels manually to avoid metadata parsing bugs
            MetadataExtractor extractor = new MetadataExtractor(tfliteModel);
            InputStream labelsInput = extractor.getAssociatedFile("labels.txt");
            if (labelsInput != null) {
                Scanner scanner = new Scanner(labelsInput).useDelimiter("\\A");
                String rawLabels = scanner.hasNext() ? scanner.next() : "";
                labels = new ArrayList<>();
                for (String label : rawLabels.split("\n")) {
                    if (!label.trim().isEmpty()) {
                        labels.add(label.trim());
                    }
                }
            } else {
                throw new Exception("labels.txt not found in model metadata");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading ML Model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        backButton.setOnClickListener(v -> finish());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
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
                            Toast.makeText(this, "No image captured. Try uploading from gallery.", Toast.LENGTH_SHORT).show();
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
                                // Convert hardware bitmaps to software to be readable by TFLite
                                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                ivCapturedImage.setImageBitmap(bitmap);
                                classifyLeafImage(bitmap);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private Bitmap getBitmapFromCameraResult(Intent data) {
        if (data == null) return null;

        if (data.getData() != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                return bitmap.copy(Bitmap.Config.ARGB_8888, true);
            } catch (Exception ignored) {
            }
        }

        Bundle extras = data.getExtras();
        if (extras != null) {
            Object bitmapObj = extras.get("data");
            if (bitmapObj instanceof Bitmap) {
                return ((Bitmap) bitmapObj).copy(Bitmap.Config.ARGB_8888, true);
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
                Toast.makeText(this, "Camera permission denied. Use 'Upload Image' button.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void classifyLeafImage(Bitmap bitmap) {
        if (tfliteInterpreter == null || labels == null) {
            Toast.makeText(this, "ML Model not fully initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Apply correct preprocessing: TFLite model expects [-1, 1] input range.
            // NormalizeOp(127.5f, 127.5f) means: (pixel_value - 127.5) / 127.5
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(256, 256, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                    .add(new CastOp(DataType.FLOAT32))
                    .build();

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Output buffer for 38 classes
            float[][] outputProbabilities = new float[1][labels.size()];

            // Run inference
            tfliteInterpreter.run(tensorImage.getBuffer(), outputProbabilities);

            // Find Argmax (Top Probability) globally
            float maxScore = -1f;
            int maxIndex = -1;

            float[] probs = outputProbabilities[0];
            
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] > maxScore) {
                    maxScore = probs[i];
                    maxIndex = i;
                }
            }

            if (maxIndex != -1) {
                String rawLabel = labels.get(maxIndex);

                float confidencePercent = maxScore * 100f;
                confidencePercent = Math.min(100f, Math.max(0f, confidencePercent));

                String[] parsed = formatPlantLabel(rawLabel);
                String plantName = parsed[0];
                String conditionName = parsed[1];

                boolean isHealthy = conditionName.toLowerCase(Locale.US).contains("healthy") ||
                                    rawLabel.toLowerCase(Locale.US).contains("healthy");

                tvClassLabel.setText(String.format("%s • %s", plantName, conditionName));
                tvConfidenceText.setText(String.format(Locale.US, "%.1f%% Confidence", confidencePercent));
                pbConfidence.setProgress((int) confidencePercent);

                if (isHealthy) {
                    tvDiagnosisStatus.setText("HEALTHY 🌱");
                    tvDiagnosisStatus.setTextColor(Color.parseColor("#2E7D32"));
                    tvRecommendation.setText(String.format("Foliage for %s shows optimal health with no detected symptoms of blight or infection.", plantName));
                } else {
                    tvDiagnosisStatus.setText("DISEASE DETECTED ⚠️");
                    tvDiagnosisStatus.setTextColor(Color.parseColor("#D32F2F"));
                    tvRecommendation.setText(getRecommendationForCondition(plantName, conditionName));
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, "Inference Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String[] formatPlantLabel(String rawLabel) {
        if (rawLabel == null || rawLabel.isEmpty()) {
            return new String[]{"Crop", "Unknown Condition"};
        }

        if (rawLabel.contains("___")) {
            String[] parts = rawLabel.split("___");
            String plantName = parts[0].replace("_", " ").replace(",", "").trim();
            String conditionName = parts[1].replace("_", " ").trim();

            if (conditionName.isEmpty()) {
                conditionName = "Healthy";
            }
            return new String[]{plantName, conditionName};
        }

        return new String[]{"Crop", rawLabel.replace("_", " ")};
    }

    private String getRecommendationForCondition(String plantName, String condition) {
        String lowerCondition = condition.toLowerCase(Locale.US);

        if (lowerCondition.contains("blight")) {
            return String.format("Early/Late Blight detected on %s. Prune infected leaves, avoid wet foliage, and apply copper fungicide.", plantName);
        } else if (lowerCondition.contains("bacterial")) {
            return String.format("Bacterial infection detected on %s. Ensure good air circulation, avoid overhead watering, and apply bactericide spray.", plantName);
        } else if (lowerCondition.contains("mildew")) {
            return String.format("Powdery mildew fungal growth detected on %s. Increase sunlight exposure and apply neem oil or sulfur spray.", plantName);
        } else if (lowerCondition.contains("rust") || lowerCondition.contains("rot") || lowerCondition.contains("scab")) {
            return String.format("Fungal condition (%s) detected on %s. Remove diseased foliage and ensure proper soil drainage.", condition, plantName);
        }

        return String.format("Symptoms of %s detected on %s. Isolate affected leaves, inspect moisture levels, and apply appropriate treatment.", condition, plantName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }
}
