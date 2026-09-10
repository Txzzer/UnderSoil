"""Configuration settings for the crop disease detection model."""

# Dataset configuration
DATASET_PATH = "Dummy Dataset"
TRAIN_SPLIT = 0.8
VALIDATION_SPLIT = 0.1
TEST_SPLIT = 0.1

# Model configuration
MODEL_INPUT_SIZE = (256, 256)
MODEL_OUTPUT_PATH = "model.tflite"
MODEL_CHECKPOINT_PATH = "model_checkpoint.h5"

# Training configuration
BATCH_SIZE = 32
EPOCHS = 10
LEARNING_RATE = 0.001

# Inference configuration
CONFIDENCE_THRESHOLD = 0.7  # Threshold for recommending cloud model
MIN_CONFIDENCE_FOR_LOCAL = 0.7

# Random seed for reproducibility
RANDOM_SEED = 42
