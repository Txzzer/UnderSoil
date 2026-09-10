"""Model training and conversion utilities for MobileNetV2."""

import tensorflow as tf
from keras.applications.mobilenet_v2 import MobileNetV2
from keras.layers import Dense, GlobalAveragePooling2D, Dropout
from keras.models import Model
from keras.optimizers import Adam
from keras.callbacks import EarlyStopping, ModelCheckpoint
import numpy as np
from config import (
    MODEL_INPUT_SIZE, MODEL_OUTPUT_PATH, MODEL_CHECKPOINT_PATH,
    BATCH_SIZE, EPOCHS, LEARNING_RATE, RANDOM_SEED
)


def build_model(num_classes):
    """Build MobileNetV2 model with custom classification head.
    
    Args:
        num_classes: Number of disease classes
    
    Returns:
        Compiled Keras model
    """
    # Load pre-trained MobileNetV2
    base_model = MobileNetV2(
        input_shape=(*MODEL_INPUT_SIZE, 3),
        include_top=False,
        weights='imagenet'
    )
    
    # Freeze base model weights for transfer learning
    base_model.trainable = False
    
    # Add custom classification head
    inputs = tf.keras.Input(shape=(*MODEL_INPUT_SIZE, 3))
    x = base_model(inputs, training=False)
    x = GlobalAveragePooling2D()(x)
    x = Dropout(0.2)(x)
    outputs = Dense(num_classes, activation='softmax')(x)
    
    model = Model(inputs, outputs)
    
    # Compile model
    model.compile(
        optimizer=Adam(learning_rate=LEARNING_RATE),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    print(f"Model built with {num_classes} output classes")
    return model


def train_model(model, X_train, y_train, X_val, y_val):
    """Train the model.
    
    Args:
        model: Compiled Keras model
        X_train: Training images
        y_train: Training labels
        X_val: Validation images
        y_val: Validation labels
    
    Returns:
        Training history
    """
    callbacks = [
        EarlyStopping(
            monitor='val_loss',
            patience=3,
            restore_best_weights=True,
            verbose=1
        ),
        ModelCheckpoint(
            MODEL_CHECKPOINT_PATH,
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        )
    ]
    
    history = model.fit(
        X_train, y_train,
        batch_size=BATCH_SIZE,
        epochs=EPOCHS,
        validation_data=(X_val, y_val),
        callbacks=callbacks,
        verbose=1
    )
    
    return history


def convert_to_tflite(model):
    """Convert Keras model to TensorFlow Lite format.
    
    Args:
        model: Trained Keras model
    
    Returns:
        TFLite model bytes
    """
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    tflite_model = converter.convert()
    
    # Save model
    with open(MODEL_OUTPUT_PATH, 'wb') as f:
        f.write(tflite_model)
    
    print(f"TFLite model saved to '{MODEL_OUTPUT_PATH}'")
    return tflite_model


def fine_tune_model(model, num_layers_to_unfreeze=50):
    """Unfreeze some base model layers and fine-tune.
    
    Args:
        model: Model to fine-tune
        num_layers_to_unfreeze: Number of layers from the end to unfreeze
    """
    # Get base model from the model
    base_model = model.layers[1]  # Assuming structure: Input -> base_model -> layers
    
    # Unfreeze last N layers
    total_layers = len(base_model.layers)
    for layer in base_model.layers[:-num_layers_to_unfreeze]:
        layer.trainable = False
    for layer in base_model.layers[-num_layers_to_unfreeze:]:
        layer.trainable = True
    
    # Recompile with lower learning rate
    model.compile(
        optimizer=Adam(learning_rate=LEARNING_RATE / 10),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    print(f"Unfroze {num_layers_to_unfreeze} layers for fine-tuning")
