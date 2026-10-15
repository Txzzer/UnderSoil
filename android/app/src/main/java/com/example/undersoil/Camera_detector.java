package com.example.undersoil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.metadata.MetadataExtractor;

import com.example.undersoil.data.ScanLog;
import com.example.undersoil.data.ScanLogDatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Camera_detector extends AppCompatActivity {

    public static final String EXTRA_CROP_NAME = "EXTRA_CROP_NAME";
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView ivCapturedImage;
    private TextView tvDiagnosisStatus;
    private TextView tvConfidenceText;
    private TextView tvClassLabel;
    private TextView tvRecommendation;
    private ProgressBar pbConfidence;
    private Button btnTakePhoto;
    private Button btnPickGallery;
    private Button btnShowRecommendations;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private Interpreter tfliteInterpreter;
    private List<String> labels;
    private String cropName;
    // Last raw model label (e.g. "Tomato___Early_blight"); matches the keys in assets/recommendations.json
    private String recognizedDisease;

    // Single-thread executor for background ML model loading & inference
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_detector);

        initViews();
        setupCameraLauncher();
        setupGalleryLauncher();

        Intent intent = getIntent();
        cropName = (intent != null) ? intent.getStringExtra(EXTRA_CROP_NAME) : null;

        // Load model in background thread to prevent UI freezing & ANRs
        executorService.execute(() -> initializeModel(cropName));
    }

    private String getModelFilenameForCrop(String cropName) {
        if (cropName == null || cropName.trim().isEmpty()) {
            return null;
        }
        String normalized = cropName.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "apple":
            case "apple.tflite":
                return "Apple.tflite";
            case "cherry":
            case "cherry (including sour)":
            case "cherry_(including_sour)":
            case "cherry.tflite":
                return "Cherry_(including_sour).tflite";
            case "corn":
            case "maize":
            case "corn (maize)":
            case "corn_(maize)":
            case "corn.tflite":
                return "Corn_(maize).tflite";
            case "grape":
            case "grape.tflite":
                return "Grape.tflite";
            case "peach":
            case "peach.tflite":
                return "Peach.tflite";
            case "pepper":
            case "bell pepper":
            case "pepper bell":
            case "pepper, bell":
            case "pepper,_bell":
            case "pepper.tflite":
                return "Pepper,_bell.tflite";
            case "potato":
            case "potato.tflite":
                return "Potato.tflite";
            case "strawberry":
            case "strawberry.tflite":
                return "Strawberry.tflite";
            case "tomato":
            case "tomato.tflite":
                return "Tomato.tflite";
            default:
                return null;
        }
    }

    private void initializeModel(String cropName) {
        String modelFilename = getModelFilenameForCrop(cropName);
        boolean loaded = false;

        if (modelFilename != null) {
            loaded = loadTfliteModel(modelFilename);
        }

        if (!loaded) {
            // Fallback to default generic model
            String defaultModel = "model2_plant_disease_meta.tflite";
            loaded = loadTfliteModel(defaultModel);

            boolean finalLoaded = loaded;
            runOnUiThread(() -> {
                if (finalLoaded) {
                    if (cropName != null && !cropName.trim().isEmpty()) {
                        Toast.makeText(Camera_detector.this, "No specific model found for '" + cropName + "'. Loaded generic model.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Camera_detector.this, "Loaded generic plant disease model.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Camera_detector.this, "Failed to load ML Model. Camera scanning disabled.", Toast.LENGTH_LONG).show();
                    if (btnTakePhoto != null) {
                        btnTakePhoto.setEnabled(false);
                    }
                    if (btnPickGallery != null) {
                        btnPickGallery.setEnabled(false);
                    }
                }
            });
        } else {
            runOnUiThread(() -> Toast.makeText(Camera_detector.this, "Loaded model for " + cropName, Toast.LENGTH_SHORT).show());
        }
    }

    private boolean loadTfliteModel(String modelFilename) {
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this, modelFilename);
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
                return true;
            } else {
                throw new Exception("labels.txt not found in model metadata");
            }
        } catch (Exception e) {
            return false;
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

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnShowRecommendations = findViewById(R.id.btnShowRecommendations);

        backButton.setOnClickListener(v -> finish());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnShowRecommendations.setOnClickListener(v -> {
            if (recognizedDisease == null) return;
            Intent intent = new Intent(Camera_detector.this, RecommendationsActivity.class);
            intent.putExtra(RecommendationsActivity.EXTRA_DISEASE_NAME, recognizedDisease);
            startActivity(intent);
        });
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap bitmap = getBitmapFromCameraResult(result.getData());
                        if (bitmap != null) {
                            ivCapturedImage.setImageBitmap(bitmap);
                            // Run classification in background thread
                            executorService.execute(() -> classifyLeafImage(bitmap));
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
                                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                ivCapturedImage.setImageBitmap(bitmap);
                                Bitmap processBitmap = bitmap;
                                // Run classification in background thread
                                executorService.execute(() -> classifyLeafImage(processBitmap));
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
            runOnUiThread(() -> Toast.makeText(this, "ML Model not fully initialized.", Toast.LENGTH_SHORT).show());
            return;
        }

        Bitmap scaledBitmap = null;

        // Hide the previous result's button until this scan produces a new one
        runOnUiThread(() -> btnShowRecommendations.setVisibility(View.GONE));

        try {
            // Dynamically calculate input size and output class count using interpreter tensors
            int[] inputShape = tfliteInterpreter.getInputTensor(0).shape();
            int inputHeight = (inputShape.length >= 3) ? inputShape[1] : 224;
            int inputWidth = (inputShape.length >= 3) ? inputShape[2] : 224;

            int[] outputShape = tfliteInterpreter.getOutputTensor(0).shape();
            int outputClasses = (outputShape.length >= 2) ? outputShape[outputShape.length - 1] : labels.size();

            // Create a lightweight 224x224 software copy specifically for TFLite processing
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);

            // Apply correct preprocessing: TFLite model expects [-1, 1] input range.
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new CastOp(DataType.FLOAT32))
                    .build();

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(scaledBitmap);
            tensorImage = imageProcessor.process(tensorImage);

            // Output buffer dynamically sized based on tensor shape / labels count
            float[][] outputProbabilities = new float[1][outputClasses];

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

            if (maxIndex != -1 && maxIndex < labels.size()) {
                String rawLabel = labels.get(maxIndex);

                float confidencePercent = maxScore * 100f;
                confidencePercent = Math.min(100f, Math.max(0f, confidencePercent));

                String[] parsed = formatPlantLabel(rawLabel);
                String plantName = parsed[0];
                String conditionName = parsed[1];

                boolean isHealthy = conditionName.toLowerCase(Locale.US).contains("healthy") ||
                                    rawLabel.toLowerCase(Locale.US).contains("healthy");

                final float finalConfidence = confidencePercent;

                // Post results back to the main UI thread
                String diseaseLabel = String.format("%s • %s", plantName, conditionName);

                runOnUiThread(() -> {
                    recognizedDisease = rawLabel;
                    tvClassLabel.setText(diseaseLabel);
                    tvConfidenceText.setText(String.format(Locale.US, "%.1f%% Confidence", finalConfidence));
                    pbConfidence.setProgress((int) finalConfidence);

                    if (isHealthy) {
                        tvDiagnosisStatus.setText("HEALTHY 🌱");
                        tvDiagnosisStatus.setTextColor(ContextCompat.getColor(this, R.color.us_bio_green));
                        tvRecommendation.setText(String.format("Foliage for %s shows optimal health with no detected symptoms of blight or infection.", plantName));
                    } else {
                        tvDiagnosisStatus.setText("DISEASE DETECTED ⚠️");
                        tvDiagnosisStatus.setTextColor(ContextCompat.getColor(this, R.color.us_danger));
                        tvRecommendation.setText(getRecommendationForCondition(plantName, conditionName));
                    }

                    btnShowRecommendations.setVisibility(View.VISIBLE);
                });

                // Save scan log to SQLite database
                saveScanLog(plantName, conditionName, confidencePercent, bitmap);
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            runOnUiThread(() -> Toast.makeText(this, "Inference Error: " + errorMsg, Toast.LENGTH_LONG).show());
        } finally {
            // Recycle scaled bitmap immediately after TFLite finishes to prevent OOM & memory thrashing
            if (scaledBitmap != null && !scaledBitmap.isRecycled() && scaledBitmap != bitmap) {
                scaledBitmap.recycle();
            }
        }
    }

    private void saveScanLog(String plantName, String conditionName, float confidence, Bitmap bitmap) {
        try {
            String userId = com.example.undersoil.data.SessionManager.getInstance(this).getUserIdString();
            String logCrop = (cropName != null && !cropName.trim().isEmpty()) ? cropName : plantName;
            String diagnosisText = String.format("%s • %s", plantName, conditionName);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            ScanLog scanLog = new ScanLog(logCrop, diagnosisText, confidence, dateStr, userId);
            scanLog.setImagePath(saveScanImage(bitmap, scanLog.getId()));
            ScanLogDatabaseHelper.getInstance(this).insertLog(scanLog, userId);
            runOnUiThread(() -> Toast.makeText(this, "Scan saved to history", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            // Ignore log save failure
        }
    }

    /** Saves a downscaled JPEG copy of the scanned image to app storage; returns its path or null. */
    private String saveScanImage(Bitmap bitmap, String logId) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        Bitmap toSave = bitmap;
        try {
            File dir = new File(getFilesDir(), "scan_images");
            if (!dir.exists() && !dir.mkdirs()) return null;

            int maxSide = 1024;
            int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (longest > maxSide) {
                float scale = (float) maxSide / longest;
                toSave = Bitmap.createScaledBitmap(bitmap,
                        Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
            }

            File file = new File(dir, logId + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                toSave.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        } finally {
            if (toSave != bitmap) toSave.recycle();
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
        // Shutdown executor service to free threads & memory
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }
}
