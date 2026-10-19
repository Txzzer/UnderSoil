import os
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import tensorflow as tf
from tensorflow.keras import layers
from sklearn.metrics import confusion_matrix
from sklearn.utils.class_weight import compute_class_weight

# settings
DATASET_PATH = "dataset"   # one crop at a time, each subfolder is a class
MODEL_NAME = "Tomato"
OUTPUT_DIR = os.path.join("outputs", MODEL_NAME)

IMG_SIZE = (224, 224)
BATCH_SIZE = 32
SEED = 123

HEAD_EPOCHS = 15
FINE_TUNE_EPOCHS = 10
FREEZE_UNTIL = 100         # mobilenet layers below this stay frozen when fine tuning

USE_CLASS_WEIGHTS = True
IMBALANCE_THRESHOLD = 3.0  # only use weights if biggest/smallest class > this
LABEL_SMOOTHING = 0.0      # try 0.1 if some diseases look really similar

# AUTOTUNE kept eating all my RAM so these are fixed now (and no .cache())
MAP_PARALLEL_CALLS = 4
PREFETCH_BUFFER = 2


tf.keras.utils.set_random_seed(SEED)
os.makedirs(OUTPUT_DIR, exist_ok=True)

# --- classes ---
class_names = sorted(d for d in os.listdir(DATASET_PATH)
                     if os.path.isdir(os.path.join(DATASET_PATH, d)))
num_classes = len(class_names)
if num_classes < 2:
    raise SystemExit(f"need at least 2 class folders in {DATASET_PATH}, found {num_classes}")

print(f"{MODEL_NAME}: {num_classes} classes")
for i, name in enumerate(class_names):
    print(f"  {i}: {name}")

with open(os.path.join(OUTPUT_DIR, "labels.txt"), "w") as f:
    f.write("\n".join(class_names))

# --- split 80/10/10 ---
# done per class so the small classes still show up in val and test
rng = np.random.default_rng(SEED)
train_paths, train_labels = [], []
val_paths, val_labels = [], []
test_paths, test_labels = [], []

for label, name in enumerate(class_names):
    folder = os.path.join(DATASET_PATH, name)
    files = sorted(os.path.join(folder, f) for f in os.listdir(folder)
                   if f.lower().endswith((".jpg", ".jpeg", ".png")))
    rng.shuffle(files)

    # classes with fewer than 10 images just go to train
    n_hold = max(1, round(len(files) * 0.1)) if len(files) >= 10 else 0
    test = files[:n_hold]
    val = files[n_hold:2 * n_hold]
    train = files[2 * n_hold:]

    test_paths += test
    test_labels += [label] * len(test)
    val_paths += val
    val_labels += [label] * len(val)
    train_paths += train
    train_labels += [label] * len(train)

print(f"\ntrain={len(train_paths)}  val={len(val_paths)}  test={len(test_paths)}")

# --- class weights (from train split only) ---
train_counts = np.bincount(train_labels, minlength=num_classes)
print("\nimages per class (train):")
for name, count in zip(class_names, train_counts):
    print(f"  {name}: {count}")

ratio = train_counts.max() / train_counts.min()
print(f"imbalance ratio: {ratio:.1f}x")

class_weight_dict = None
if USE_CLASS_WEIGHTS and ratio > IMBALANCE_THRESHOLD:
    weights = compute_class_weight("balanced",
                                   classes=np.arange(num_classes),
                                   y=np.repeat(np.arange(num_classes), train_counts))
    class_weight_dict = {i: float(w) for i, w in enumerate(weights)}
    print("using class weights")
else:
    print("no class weights")


# --- data pipeline ---
def load_image(path, label):
    img = tf.io.decode_image(tf.io.read_file(path), channels=3, expand_animations=False)
    img = tf.image.resize(img, IMG_SIZE)
    img.set_shape(IMG_SIZE + (3,))
    return tf.cast(img, tf.float32), label   # still 0-255, rescaling happens in the model


# training images only, new random version every epoch. not saved with the model
augment = tf.keras.Sequential([
    layers.RandomFlip("horizontal_and_vertical"),
    layers.RandomRotation(0.15),
    layers.RandomZoom(0.1),
    layers.RandomContrast(0.15),
    layers.RandomBrightness(0.15),
], name="augmentation")


def make_dataset(paths, labels, training):
    ds = tf.data.Dataset.from_tensor_slices((paths, labels))
    if training:
        # shuffling the file paths is cheap, images get loaded after
        ds = ds.shuffle(len(paths), seed=SEED, reshuffle_each_iteration=True)
    ds = ds.map(load_image, num_parallel_calls=MAP_PARALLEL_CALLS)
    ds = ds.batch(BATCH_SIZE)
    if training:
        ds = ds.map(lambda x, y: (augment(x, training=True), y),
                    num_parallel_calls=MAP_PARALLEL_CALLS)
    return ds.prefetch(PREFETCH_BUFFER)


train_data = make_dataset(train_paths, train_labels, training=True)
val_data = make_dataset(val_paths, val_labels, training=False)
test_data = make_dataset(test_paths, test_labels, training=False)

# --- model ---
base_model = tf.keras.applications.MobileNetV2(input_shape=IMG_SIZE + (3,),
                                               include_top=False, weights="imagenet")
base_model.trainable = False

model = tf.keras.Sequential([
    tf.keras.Input(shape=IMG_SIZE + (3,)),
    layers.Rescaling(1. / 127.5, offset=-1),   # 0-255 -> -1..1 so the app can send raw pixels
    base_model,
    layers.GlobalAveragePooling2D(),
    layers.Dropout(0.2),
    layers.Dense(num_classes, activation="softmax"),
])


def make_loss():
    if LABEL_SMOOTHING > 0:
        # sparse CCE doesn't take label_smoothing, so one-hot the labels here
        cce = tf.keras.losses.CategoricalCrossentropy(label_smoothing=LABEL_SMOOTHING)

        def smoothed_loss(y_true, y_pred):
            return cce(tf.one_hot(tf.cast(y_true, tf.int32), depth=num_classes), y_pred)
        return smoothed_loss
    return "sparse_categorical_crossentropy"


def make_callbacks():
    # fresh ones for each fit() call
    return [
        tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=3,
                                         restore_best_weights=True, verbose=1),
        tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.2, patience=2,
                                             min_lr=1e-7, verbose=1),
    ]


# --- stage 1: just the head ---
print("\nStage 1 - training head (base frozen, lr 1e-3)")
model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
              loss=make_loss(), metrics=["accuracy"])
history1 = model.fit(train_data, validation_data=val_data, epochs=HEAD_EPOCHS,
                     callbacks=make_callbacks(), class_weight=class_weight_dict)
print(f"stage 1 done after {len(history1.epoch)} epochs")

# --- stage 2: fine tune top of mobilenet ---
base_model.trainable = True
for layer in base_model.layers[:FREEZE_UNTIL]:
    layer.trainable = False
# keep batchnorm frozen, updating it on a small dataset made val worse
for layer in base_model.layers:
    if isinstance(layer, layers.BatchNormalization):
        layer.trainable = False

n_trainable = sum(1 for l in base_model.layers if l.trainable)
print(f"\nStage 2 - fine tuning {n_trainable}/{len(base_model.layers)} layers (lr 1e-5)")

# have to recompile after changing trainable
model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
              loss=make_loss(), metrics=["accuracy"])
start = history1.epoch[-1] + 1
history2 = model.fit(train_data, validation_data=val_data,
                     epochs=start + FINE_TUNE_EPOCHS, initial_epoch=start,
                     callbacks=make_callbacks(), class_weight=class_weight_dict)
print(f"stage 2 done after {len(history2.epoch)} epochs")

# --- training curves ---
fig, axes = plt.subplots(1, 2, figsize=(12, 5))
for ax, key in zip(axes, ["accuracy", "loss"]):
    ax.plot(history1.history[key] + history2.history[key], label="train")
    ax.plot(history1.history["val_" + key] + history2.history["val_" + key], label="val")
    ax.axvline(len(history1.epoch), linestyle="--", color="gray", label="fine tuning")
    ax.set_title(key)
    ax.set_xlabel("epoch")
    ax.legend()
fig.tight_layout()
fig.savefig(os.path.join(OUTPUT_DIR, "training_curves.png"), dpi=150)
plt.close(fig)

# --- test set ---
y_true, y_pred = [], []
for images, labels in test_data:
    y_pred.extend(np.argmax(model.predict(images, verbose=0), axis=1))
    y_true.extend(labels.numpy())

accuracy = float(np.mean(np.array(y_true) == np.array(y_pred)))
print(f"\ntest accuracy: {accuracy:.4f} ({len(y_true)} images)")

cm = confusion_matrix(y_true, y_pred, labels=list(range(num_classes)))
np.savetxt(os.path.join(OUTPUT_DIR, "confusion_matrix.csv"), cm, fmt="%d", delimiter=",")

# normalise each row so it shows where each true class ended up
cm_norm = cm.astype(float) / cm.sum(axis=1, keepdims=True).clip(min=1)
size = max(6, num_classes * 0.7)
fig, ax = plt.subplots(figsize=(size, size))
im = ax.imshow(cm_norm, cmap="Blues", vmin=0, vmax=1)
ax.set_xticks(range(num_classes))
ax.set_yticks(range(num_classes))
ax.set_xticklabels(class_names, rotation=45, ha="right", fontsize=8)
ax.set_yticklabels(class_names, fontsize=8)
ax.set_xlabel("Predicted")
ax.set_ylabel("True")
ax.set_title(f"{MODEL_NAME} confusion matrix (acc {accuracy:.3f})")
fig.colorbar(im, ax=ax, fraction=0.046, pad=0.04)
fig.tight_layout()
fig.savefig(os.path.join(OUTPUT_DIR, "confusion_matrix.png"), dpi=150)
plt.close(fig)

# --- save + tflite (float16) ---
model.save(os.path.join(OUTPUT_DIR, f"{MODEL_NAME}.keras"))

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]
tflite_model = converter.convert()

with open(os.path.join(OUTPUT_DIR, f"{MODEL_NAME}.tflite"), "wb") as f:
    f.write(tflite_model)

print(f"\nsaved everything to {OUTPUT_DIR}/ (tflite is {len(tflite_model) / 1e6:.2f} MB)")
print("android: float32 input (1, 224, 224, 3), raw 0-255 pixels, no normalising needed")