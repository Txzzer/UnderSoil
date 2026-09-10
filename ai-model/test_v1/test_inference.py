"""Testing script for the disease detection model inference."""

import os
import json
from pathlib import Path
from inference import DiseaseDetector, evaluate_model
from data_loader import load_dataset, split_dataset
from config import MODEL_OUTPUT_PATH


def test_tflite_inference():
    """Test TFLite model inference on sample images."""
    print("\n" + "="*60)
    print("TESTING TFLITE INFERENCE")
    print("="*60 + "\n")
    
    # Check if model exists
    if not Path(MODEL_OUTPUT_PATH).exists():
        print(f"Error: TFLite model not found at '{MODEL_OUTPUT_PATH}'")
        print("Please run train.py first to train and convert the model.")
        return None
    
    # Load class names from dataset
    _, _, class_names = load_dataset()
    
    # Initialize detector
    detector = DiseaseDetector(tflite_model_path=MODEL_OUTPUT_PATH, class_names=class_names)
    print()
    
    # Test on sample images from dataset
    print("Testing on sample images from each class...\n")
    results_summary = {}
    
    for class_name in class_names:
        class_path = Path("Dummy Dataset") / class_name
        image_files = sorted([f for f in class_path.iterdir() if f.suffix.lower() in ['.jpg', '.jpeg', '.png']])
        
        if image_files:
            # Test on first image of this class
            test_image = image_files[0]
            result = detector.predict(str(test_image))
            
            results_summary[class_name] = result
            
            print(f"Class: {class_name}")
            print(f"  Test image: {test_image.name}")
            print(f"  Predicted: {result['predicted_class']}")
            print(f"  Confidence: {result['confidence']:.4f}")
            print(f"  Use cloud model: {result['use_cloud_model']}")
            print(f"  All predictions:")
            for pred_class, score in result['all_predictions'].items():
                print(f"    - {pred_class}: {score:.4f}")
            print()
    
    print("="*60)
    print("INFERENCE TESTING COMPLETE")
    print("="*60 + "\n")
    
    return results_summary


def test_batch_prediction():
    """Test batch prediction on multiple images."""
    print("\n" + "="*60)
    print("TESTING BATCH PREDICTION")
    print("="*60 + "\n")
    
    if not Path(MODEL_OUTPUT_PATH).exists():
        print(f"Error: TFLite model not found at '{MODEL_OUTPUT_PATH}'")
        return None
    
    # Collect sample images
    _, _, class_names = load_dataset()
    detector = DiseaseDetector(tflite_model_path=MODEL_OUTPUT_PATH, class_names=class_names)
    
    test_images = []
    for class_name in class_names:
        class_path = Path("Dummy Dataset") / class_name
        image_files = [f for f in class_path.iterdir() if f.suffix.lower() in ['.jpg', '.jpeg', '.png']]
        if image_files:
            test_images.append(str(image_files[0]))
    
    if not test_images:
        print("No test images found.")
        return None
    
    print(f"Predicting on {len(test_images)} images...\n")
    batch_results = detector.batch_predict(test_images)
    
    correct_predictions = 0
    for item in batch_results:
        if 'error' not in item:
            image_path = Path(item['image'])
            true_class = image_path.parent.name
            predicted_class = item['result']['predicted_class']
            is_correct = true_class == predicted_class
            
            if is_correct:
                correct_predictions += 1
            
            print(f"Image: {image_path.name}")
            print(f"  True class: {true_class}")
            print(f"  Predicted: {predicted_class} (Confidence: {item['result']['confidence']:.4f})")
            print(f"  Correct: {is_correct}")
            print()
    
    accuracy = correct_predictions / len([b for b in batch_results if 'error' not in b]) if batch_results else 0
    print("="*60)
    print(f"Batch Prediction Accuracy: {accuracy:.4f} ({correct_predictions}/{len(test_images)})")
    print("="*60 + "\n")
    
    return batch_results


def run_all_tests():
    """Run all inference tests."""
    print("\n" + "="*70)
    print(" "*15 + "DISEASE DETECTION MODEL - TEST SUITE")
    print("="*70)
    
    # Test 1: TFLite inference
    inference_results = test_tflite_inference()
    
    # Test 2: Batch prediction
    batch_results = test_batch_prediction()
    
    print("\n" + "="*70)
    print("ALL TESTS COMPLETED")
    print("="*70 + "\n")


if __name__ == "__main__":
    run_all_tests()
