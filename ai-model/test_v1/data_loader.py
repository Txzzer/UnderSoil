"""Data loading and preprocessing utilities."""

import os
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split
from keras.preprocessing.image import load_img, img_to_array
from keras.applications.mobilenet_v2 import preprocess_input
from config import DATASET_PATH, TRAIN_SPLIT, VALIDATION_SPLIT, TEST_SPLIT, MODEL_INPUT_SIZE, RANDOM_SEED


def load_dataset():
    """Load images and labels from the dummy dataset directory.
    
    Expected directory structure:
        dummy_dataset/
        ├── Class_1/
        │   ├── image1.jpg
        │   └── image2.jpg
        └── Class_2/
            ├── image3.jpg
            └── image4.jpg
    
    Returns:
        tuple: (images array, labels array, class_names list)
    """
    dataset_path = Path(DATASET_PATH).resolve()
    
    if not dataset_path.exists():
        raise FileNotFoundError(f"Dataset path '{DATASET_PATH}' not found.")
    
    images = []
    labels = []
    class_names = sorted([d.name for d in dataset_path.iterdir() if d.is_dir()])
    
    if not class_names:
        raise ValueError(f"No subdirectories found in '{DATASET_PATH}'. Expected disease class folders.")
    
    print(f"Found classes: {class_names}")
    
    for label_idx, class_name in enumerate(class_names):
        class_path = dataset_path / class_name
        image_files = sorted([f for f in class_path.iterdir() if f.suffix.lower() in ['.jpg', '.jpeg', '.png']])
        
        print(f"Loading {len(image_files)} images from class '{class_name}'...")
        
        for image_file in image_files:
            try:
                # Load and preprocess image
                img = load_img(image_file, target_size=MODEL_INPUT_SIZE)
                img_array = img_to_array(img)
                img_array = preprocess_input(img_array)  # MobileNetV2 preprocessing
                
                images.append(img_array)
                labels.append(label_idx)
            except Exception as e:
                print(f"Warning: Failed to load {image_file}: {e}")
    
    if not images:
        raise ValueError(f"No images found in '{DATASET_PATH}'.")
    
    images = np.array(images, dtype=np.float32)
    labels = np.array(labels, dtype=np.int32)
    
    print(f"Loaded {len(images)} images from {len(class_names)} classes.")
    return images, labels, class_names


def split_dataset(images, labels, test_size=0.2, val_size=0.1):
    """Split dataset into train, validation, and test sets.
    
    Args:
        images: Input images array
        labels: Labels array
        test_size: Fraction of data for testing
        val_size: Fraction of training data for validation
    
    Returns:
        tuple: (X_train, X_val, X_test, y_train, y_val, y_test)
    """
    # First split: separate test set
    X_temp, X_test, y_temp, y_test = train_test_split(
        images, labels,
        test_size=test_size,
        random_state=RANDOM_SEED,
        stratify=labels
    )
    
    # Second split: separate validation from training
    val_size_adjusted = val_size / (1 - test_size)
    X_train, X_val, y_train, y_val = train_test_split(
        X_temp, y_temp,
        test_size=val_size_adjusted,
        random_state=RANDOM_SEED,
        stratify=y_temp
    )
    
    print(f"Dataset split - Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")
    return X_train, X_val, X_test, y_train, y_val, y_test
