# Context

An Android, agricultural mobile application uses an Edge AI, image recognition model to diagonise South African crop diseases without the use of an internet connection. The model is a TensorFlow Lite model using MobileNetV2.
The model will use a confidence score to determine its certainty on the diagonised crop (If it contains a certain disease or not). If a diagonise has a low enough confidence score, it will recommend the user to connected to the internet to use a cloud-based, AI model.
The model only have to worry on the probabilty that a certain crop contains a disease. It's unnessecary to train the model to identify what kind of crop is AND if it contains diseases. The user will already predetermine what crop to scan in the Android app.

# Requirements

- Model must be able to be utilised in an Android application (Android Studio)
- Requesting a test bed to create, train and test a MobileNetV2/TensorFlow Lite model
- Must be trained on dummy data in the folder 'Dummy Dataset'
- Uses a confidence score to determine the probabilty of a crop containing diseases.

# Constraits

All generated must be contained in the 'ai-model' folder and no generated content must be outside this boundary.