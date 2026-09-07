/*
  AgriGuard IoT Node - Capstone Demonstration Code (ESP32 Classic)
  UPDATED: Added DHT Sensor, Hybrid Control (Button + MQTT Cloud)
*/

#include <ESP32Servo.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>
#include <DHT.h> // NEW: Adafruit DHT Library

// --- Wi-Fi & MQTT Configuration ---
const char* ssid = "Chris_2.4GHz";
const char* password = "Travi$304";

const char* mqtt_server = "fe7c1a0013c74b819748355010956172.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;

const char* mqtt_user = "esp32-device"; // From your HiveMQ Cloud Access Management
const char* mqtt_pass = "Travi$304"; // From your HiveMQ Cloud Access Management

const char* telemetryTopic = "undersoil/telemetry";
const char* commandTopic = "undersoil/commands";

// --- Pin Definitions (Matched to generic ESP-WROOM-32) ---
const int POT_PIN = 34;      
const int SERVO_PIN = 26;    
const int BUTTON_PIN = 25;   
const int DHT_PIN = 4;       
#define DHTTYPE DHT11        // Change to DHT22 if necessary

// --- State Variables ---
bool isValveOpen = false;
float currentFlowRate = 0.0;
float currentTemp = 0.0;
float currentHumidity = 0.0;
bool lastButtonState = HIGH; 

unsigned long lastSyncTime = 0;
const unsigned long syncInterval = 5000; 

Servo valveServo;
// CRITICAL: Must use WiFiClientSecure for port 8883
WiFiClientSecure espClient; 
PubSubClient mqttClient(espClient);
DHT dht(DHT_PIN, DHTTYPE);

// --- CLOUD COMMAND LISTENER ---
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("☁️ CLOUD COMMAND ARRIVED: ");
  String messageTemp;
  for (int i = 0; i < length; i++) {
    messageTemp += (char)payload[i];
  }
  
  JsonDocument doc;
  DeserializationError error = deserializeJson(doc, messageTemp);

  if (!error) {
    bool cloudWantsValveOpen = doc["valveIsOpen"];
    if (cloudWantsValveOpen != isValveOpen) {
      isValveOpen = cloudWantsValveOpen;
      updatePhysicalValve();
      Serial.println("📱 Valve state changed via CLOUD!");
      sendTelemetryPayload();
    }
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println("Starting AgriGuard IoT Node (Secure MQTT)...");

  pinMode(POT_PIN, INPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP); 
  dht.begin();
  
  valveServo.attach(SERVO_PIN);
  updatePhysicalValve(); 

  Serial.print("Connecting to Wi-Fi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWi-Fi Connected!");

  // CRITICAL: Tells the ESP32 to skip checking the server's SSL certificate 
  // to save memory and prevent connection rejections.
  espClient.setInsecure();

  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(mqttCallback);
}

void reconnect() {
  while (!mqttClient.connected()) {
    Serial.print("Attempting Secure MQTT connection... ");
    
    // Generate a unique ID to prevent HiveMQ from kicking us off
    String clientId = "AgriGuard-ESP32-";
    clientId += String(random(0, 0xffff), HEX);
    
    // Connect WITH the HiveMQ Cloud Username and Password
    if (mqttClient.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
      Serial.println("[\u2713] Connected to Private Broker!");
      mqttClient.subscribe(commandTopic);
    } else {
      Serial.print("FAILED, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" (Trying again in 5 seconds)");
      delay(5000);
    }
  }
}

void updatePhysicalValve() {
  if (isValveOpen) {
    valveServo.write(90); 
  } else {
    valveServo.write(0);  
  }
}

void loop() {
  if (!mqttClient.connected()) {
    reconnect();
  }
  mqttClient.loop(); 

  // 1. LOCAL BUTTON OVERRIDE
  bool buttonState = digitalRead(BUTTON_PIN);
  if (buttonState == LOW && lastButtonState == HIGH) {
    delay(50); 
    isValveOpen = !isValveOpen; 
    updatePhysicalValve();
    Serial.println("🖐️ Valve state changed via PHYSICAL BUTTON!");
    sendTelemetryPayload(); 
  }
  lastButtonState = buttonState;

  // 2. GATHER SENSOR DATA
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  if (!isnan(h) && !isnan(t)) {
    currentHumidity = h;
    currentTemp = t;
  }

 // 3. FAULT INJECTION LOGIC (Baseline + Anomaly)
  float baseFlow = 0.0;
  if (isValveOpen) {
      // Baseline flow of ~40 LPM when valve is open
      baseFlow = 40.0; 
  }

  // Read the raw potentiometer value (0 to 4095)
  int rawAnalogValue = analogRead(POT_PIN);

  // AGGRESSIVE DEADZONE: Ignore electrical static under 800
  float anomalyFlow = 0.0;
  if (rawAnalogValue > 800) { 
      // Map the remaining knob movement (800 - 4095) to an extra 0 - 80 LPM leak
      anomalyFlow = map(rawAnalogValue, 800, 4095, 0, 80);
  }

// Calculate the final flow rate with the deadzone applied
  currentFlowRate = baseFlow + anomalyFlow;
  // 4. TRANSMIT TELEMETRY
  if (millis() - lastSyncTime >= syncInterval) {
    lastSyncTime = millis();
    sendTelemetryPayload();
    }
  }

void sendTelemetryPayload() {
  String jsonPayload = "{";
  jsonPayload += "\"id\": 0,";
  jsonPayload += "\"deviceId\": \"ESP32_WROOM_ZONE_1\",";
  jsonPayload += "\"flowRateLitersPerMin\": " + String(currentFlowRate, 2) + ",";
  jsonPayload += "\"valveIsOpen\": " + String(isValveOpen ? "true" : "false") + ",";
  jsonPayload += "\"temperatureCelsius\": " + String(currentTemp, 2) + ",";
  jsonPayload += "\"humidityPercentage\": " + String(currentHumidity, 2);
  jsonPayload += "}"; 

  if (mqttClient.connected()) {
    mqttClient.publish(telemetryTopic, jsonPayload.c_str());
    Serial.println("[\u2713] MQTT Published: " + jsonPayload);
  }
}