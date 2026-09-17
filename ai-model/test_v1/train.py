"""Main script for training the disease detection model."""

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'  # Suppress TensorFlow warnings

from data_loader import load_dataset, split_dataset
from model_trainer import build_model, train_model, convert_to_tflite
from inference import evaluate_model
from add_metadata import add_metadata_to_tflite_model
from config import MODEL_CHECKPOINT_PATH


def main():
    """Main training pipeline."""
    print("\n" + "="*60)
    print("CROP DISEASE DETECTION - TRAINING PIPELINE")
    print("="*60 + "\n")
    
    # Load dataset
    print("Step 1: Loading dataset...")
    images, labels, class_names = load_dataset()
    print(f"Classes: {class_names}\n")
    
    # Split dataset
    print("Step 2: Splitting dataset...")
    X_train, X_val, X_test, y_train, y_val, y_test = split_dataset(
        images, labels,
        test_size=0.2,
        val_size=0.1
    )
    print()
    
    # Build model
    print("Step 3: Building MobileNetV2 model...")
    model = build_model(len(class_names))
    model.summary()
    print()
    
    # Train model
    print("Step 4: Training model...")
    history = train_model(model, X_train, y_train, X_val, y_val)
    print()
    
    # Evaluate on test set
    print("Step 5: Evaluating model on test set...")
    test_results = evaluate_model(model, X_test, y_test, class_names)
    
    # Convert to TFLite
    print("Step 6: Converting to TensorFlow Lite format...")
    convert_to_tflite(model)
    print()

    # Adding metadata to the model
    print("Step 7: Adding metadata to the model...")
    add_metadata_to_tflite_model()
    print()

    print("="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    print(f"Model checkpoint saved: {MODEL_CHECKPOINT_PATH}")
    print("TFLite model ready for Android deployment")
    print("="*60 + "\n")
    
    return model, test_results, class_names


if __name__ == "__main__":
    model, results, class_names = main()
