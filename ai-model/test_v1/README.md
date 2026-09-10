# Crop Disease Detection - AI Model Testbed

A TensorFlow/Keras testbed for training and testing a MobileNetV2-based TensorFlow Lite model for crop disease detection on Android devices.

## Project Overview

This project implements an edge AI solution for crop disease diagnosis using:
- **Model Architecture**: MobileNetV2 (lightweight, efficient for mobile)
- **Framework**: TensorFlow/Keras
- **Output Format**: TensorFlow Lite (.tflite) for Android deployment
- **Dataset**: Dummy dataset with crop disease images organized by class

## Project Structure

```
ai-model/
├── config.py              # Configuration settings
├── data_loader.py         # Dataset loading and preprocessing
├── model_trainer.py       # Model training and TFLite conversion
├── inference.py           # Inference utilities and evaluation
├── train.py               # Main training script
├── test_inference.py      # Testing and inference script
├── requirements.txt       # Python dependencies
├── README.md              # This file
├── model_checkpoint.h5    # Saved model checkpoint (generated)
├── model.tflite           # TensorFlow Lite model (generated)
└── Dummy Dataset/         # Training dataset
    ├── Class_1/           # Disease class 1 images
    └── Class_2/           # Disease class 2 images
```

## Quick Start

### 1. Setup Environment

```bash
# Create a virtual environment (recommended)
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Prepare Dataset

Organize your crop disease images in the `Dummy Dataset` folder:

```
Dummy Dataset/
├── Potato__Early_blight/
│   ├── image1.jpg
│   ├── image2.jpg
│   └── ...
└── Potato__Healthy/
    ├── image1.jpg
    ├── image2.jpg
    └── ...
```

Images should be in common formats: `.jpg`, `.jpeg`, `.png`

### 3. Train the Model

```bash
python train.py
```

This will:
1. Load and preprocess images from `Dummy Dataset/`
2. Split data into train (80%), validation (10%), and test (10%) sets
3. Build and train a MobileNetV2 model
4. Save the trained model checkpoint as `model_checkpoint.h5`
5. Convert the model to TensorFlow Lite format as `model.tflite`
6. Display evaluation metrics on the test set

### 4. Test Inference

```bash
python test_inference.py
```

This will:
1. Load the TFLite model
2. Test single image inference on sample images from each class
3. Run batch prediction to evaluate overall accuracy
4. Display confidence scores and predictions

## Configuration

Edit `config.py` to customize:

- **Dataset settings**: Path, train/validation/test splits
- **Model settings**: Input size, output paths
- **Training settings**: Batch size, epochs, learning rate
- **Inference settings**: Confidence threshold for cloud model recommendation

### Key Configuration Parameters

```python
# Model input size (MobileNetV2 standard)
MODEL_INPUT_SIZE = (224, 224)

# Confidence threshold for recommending cloud API
MIN_CONFIDENCE_FOR_LOCAL = 0.7  # Use cloud model if confidence < 70%

# Training hyperparameters
BATCH_SIZE = 32
EPOCHS = 10
LEARNING_RATE = 0.001
```

## Usage Examples

### Example 1: Train and Export Model

```python
from train import main
model, results, class_names = main()
```

### Example 2: Make Predictions on a Single Image

```python
from inference import DiseaseDetector

detector = DiseaseDetector(class_names=['Healthy', 'Early_blight'])
result = detector.predict('path/to/image.jpg')

print(f"Predicted: {result['predicted_class']}")
print(f"Confidence: {result['confidence']:.4f}")
print(f"Use cloud model: {result['use_cloud_model']}")
```

### Example 3: Batch Processing

```python
from inference import DiseaseDetector

detector = DiseaseDetector()
results = detector.batch_predict([
    'image1.jpg',
    'image2.jpg',
    'image3.jpg'
])

for item in results:
    print(f"{item['image']}: {item['result']['predicted_class']}")
```

## Model Output

The trained TFLite model outputs:
- **Confidence scores** for each disease class
- **Predicted class** with highest confidence
- **Recommendation flag** to use cloud model if confidence is below threshold

### Confidence Score Interpretation

- **High confidence (≥ 0.7)**: Local edge model result is reliable
- **Low confidence (< 0.7)**: Recommend switching to cloud-based model for better accuracy

## Performance Metrics

The model evaluates:
- **Overall accuracy** on test set
- **Per-class accuracy** for each disease type
- **Inference time** for single predictions

Results are printed to console and can be logged for analysis.

## Android Integration

The generated `model.tflite` file can be integrated into Android applications:

1. Copy `model.tflite` to `app/src/main/assets/` in your Android project
2. Load using TensorFlow Lite Interpreter in Java/Kotlin
3. Use confidence scores to determine if cloud API should be called

```kotlin
// Example Android usage
val interpreter = Interpreter(loadModelFile())
val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
val output = Array(1) { FloatArray(numClasses) }
interpreter.run(input, output)
val confidence = output[0].maxOrNull() ?: 0f
```

## Troubleshooting

### No images found error
- Ensure images are in subdirectories named after disease classes
- Check file extensions are `.jpg`, `.jpeg`, or `.png`
- Verify the `Dummy Dataset/` folder exists in the current directory

### Out of memory during training
- Reduce `BATCH_SIZE` in `config.py`
- Reduce `EPOCHS` to test with fewer iterations
- Ensure input images aren't too large

### Low accuracy
- Add more training data (currently using dummy dataset)
- Adjust `EPOCHS` for longer training
- Try different `LEARNING_RATE` values
- Ensure images are properly labeled by class

## Project Requirements

✓ Model usable in Android applications (TensorFlow Lite format)
✓ Testbed for training and testing MobileNetV2 model
✓ Trains on dummy dataset in `Dummy Dataset/` folder
✓ Uses confidence scores for disease probability assessment
✓ All content contained in `ai-model/` folder

## Next Steps

1. **Expand dataset**: Add more real crop disease images to improve accuracy
2. **Fine-tune parameters**: Experiment with learning rate, batch size, epochs
3. **Implement data augmentation**: Rotate, flip, and adjust images for better generalization
4. **Deploy to Android**: Integrate the TFLite model into the mobile application
5. **Monitor performance**: Track accuracy metrics in production

## License

This project is part of the UnderSoil agricultural disease detection system.

## References

- [TensorFlow Lite Guide](https://www.tensorflow.org/lite)
- [MobileNetV2 Architecture](https://github.com/tensorflow/models/tree/master/research/slim/nets/mobilenet)
- [TensorFlow Model Conversion](https://www.tensorflow.org/lite/guide/concrete_function)
