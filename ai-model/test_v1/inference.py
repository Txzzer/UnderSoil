"""Inference utilities for the disease detection model."""

import numpy as np
import tensorflow as tf
from keras.preprocessing.image import load_img, img_to_array
from keras.applications.mobilenet_v2 import preprocess_input
from config import MODEL_INPUT_SIZE, MODEL_OUTPUT_PATH, MIN_CONFIDENCE_FOR_LOCAL


class DiseaseDetector:
    """Crop disease detector using TFLite model."""
    
    def __init__(self, tflite_model_path=MODEL_OUTPUT_PATH, class_names=None):
        """Initialize the disease detector.
        
        Args:
            tflite_model_path: Path to the TFLite model file
            class_names: List of class names (disease types)
        """
        self.interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
        self.interpreter.allocate_tensors()
        
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()
        self.class_names = class_names or ['Disease', 'Healthy']
        
        print(f"Loaded TFLite model from '{tflite_model_path}'")
    
    def predict(self, image_path):
        """Predict disease presence in an image.
        
        Args:
            image_path: Path to the image file
        
        Returns:
            dict: Contains prediction results and confidence
        """
        # Load and preprocess image
        img = load_img(image_path, target_size=MODEL_INPUT_SIZE)
        img_array = img_to_array(img)
        img_array = preprocess_input(img_array)
        img_array = np.expand_dims(img_array, axis=0).astype(np.float32)
        
        # Run inference
        self.interpreter.set_tensor(self.input_details[0]['index'], img_array)
        self.interpreter.invoke()
        
        # Get predictions
        predictions = self.interpreter.get_tensor(self.output_details[0]['index'])
        predictions = predictions[0]  # Get first (only) batch
        
        # Get top prediction
        predicted_class_idx = np.argmax(predictions)
        confidence = float(predictions[predicted_class_idx])
        predicted_class = self.class_names[predicted_class_idx]
        
        return {
            'predicted_class': predicted_class,
            'confidence': confidence,
            'all_predictions': {
                self.class_names[i]: float(predictions[i])
                for i in range(len(self.class_names))
            },
            'use_cloud_model': confidence < MIN_CONFIDENCE_FOR_LOCAL
        }
    
    def batch_predict(self, image_paths):
        """Predict disease presence for multiple images.
        
        Args:
            image_paths: List of image file paths
        
        Returns:
            List of prediction dictionaries
        """
        results = []
        for image_path in image_paths:
            try:
                result = self.predict(image_path)
                results.append({
                    'image': image_path,
                    'result': result
                })
            except Exception as e:
                results.append({
                    'image': image_path,
                    'error': str(e)
                })
        return results


def evaluate_model(model, X_test, y_test, class_names):
    """Evaluate model on test set and print detailed metrics.
    
    Args:
        model: Keras model
        X_test: Test images
        y_test: Test labels
        class_names: List of class names
    
    Returns:
        dict: Evaluation metrics
    """
    # Get predictions
    predictions = model.predict(X_test, verbose=0)
    predicted_labels = np.argmax(predictions, axis=1)
    
    # Calculate accuracy
    accuracy = np.mean(predicted_labels == y_test)
    
    # Calculate per-class metrics
    per_class_metrics = {}
    for class_idx, class_name in enumerate(class_names):
        mask = y_test == class_idx
        if np.sum(mask) > 0:
            class_accuracy = np.mean(predicted_labels[mask] == y_test[mask])
            per_class_metrics[class_name] = {
                'accuracy': float(class_accuracy),
                'samples': int(np.sum(mask))
            }
    
    results = {
        'overall_accuracy': float(accuracy),
        'per_class': per_class_metrics,
        'total_samples': len(X_test)
    }
    
    # Print results
    print("\n" + "="*50)
    print("MODEL EVALUATION RESULTS")
    print("="*50)
    print(f"Overall Accuracy: {accuracy:.4f}")
    print("\nPer-Class Performance:")
    for class_name, metrics in per_class_metrics.items():
        print(f"  {class_name}: {metrics['accuracy']:.4f} ({metrics['samples']} samples)")
    print("="*50 + "\n")
    
    return results
