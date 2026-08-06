import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications.mobilenet_v2 import MobileNetV2

data = tf.keras.utils.image_dataset_from_directory(
    "C:/Users/Zhawn/Desktop/University/2026/2nd Semester/ITPV302/Dummy Dataset",
    labels = 'inferred',
    image_size = (256, 256),
    batch_size = 16,
    label_mode = 'categorical'
)

base_model = MobileNetV2(
    weights="imagenet",
    include_top=False,
    input_shape=(256, 256, 3)
)

base_model.trainable = False

model = models.Sequential([
    base_model,
    layers.GlobalAveragePooling2D(),
    layers.Dense(128, activation="relu"),
    layers.Dropout(0.3),
    layers.Dense(len(data.class_names), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="categorical_crossentropy",
    metrics=["accuracy"]
)


model.summary()

converter = tf.lite.TFLiteConverter.from_keras_model(model)

tflite_model = converter.convert()

with open('dummy_model.tflite', 'wb') as f:
    f.write(tflite_model)

'''
history = model.fit(
    data,
    epochs=5
)
'''