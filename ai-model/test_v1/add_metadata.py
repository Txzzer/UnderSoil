from tflite_support.metadata_writers import image_classifier
from tflite_support.metadata_writers import metadata_info
from config import MODEL_OUTPUT_PATH, MODEL_METADATA_OUTPUT_PATH, LABEL_PATH

def add_metadata_to_tflite_model():
    writer = image_classifier.MetadataWriter.create_for_inference(
        writer_utils.load_file(MODEL_OUTPUT_PATH),
        input_norm_mean=[127.5], 
        input_norm_std=[127.5],
        label_file_paths=[LABEL_PATH]
    )

    # Save the newly packed model
    with open(MODEL_METADATA_OUTPUT_PATH, "wb") as f:
        f.write(writer.populate())