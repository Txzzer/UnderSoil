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
const char* ssid = "YOUR-2.4GHZ-WIFI-SSID";
const char* password = "YOUR-2.4GHZ-WIFI-PASSWORD";

const char* mqtt_server = "YOUR-HIVEMQ-SERVER-LINK";
const int mqtt_port = 8883;

const char* mqtt_user = "YOUR-HIVEMQ-USERNAME"; // From your HiveMQ Cloud Access Management
const char* mqtt_pass = "YOUR-HIVEMQ-PASSWORD"; // From your HiveMQ Cloud Access Management

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

// --- Timed Watering (scheduled by the backend: {"valveIsOpen": true, "duration": <minutes>}) ---
// The ESP32 closes the valve itself when the timer ends, so watering stops even if Wi-Fi/MQTT drops.
const unsigned long MAX_WATERING_MINUTES = 180; // Safety cap on any single timed run
bool wateringTimerActive = false;
unsigned long wateringStartMs = 0;
unsigned long wateringDurationMs = 0;

unsigned long lastReconnectAttempt = 0;
const unsigned long reconnectInterval = 5000;

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
  
  Serial.println(messageTemp);

  JsonDocument doc;
  DeserializationError error = deserializeJson(doc, messageTemp);

  // Ignore malformed messages instead of treating a missing key as "close"
  if (error || !doc["valveIsOpen"].is<bool>()) {
    Serial.println("⚠️ Ignored command: missing or invalid 'valveIsOpen'");
    return;
  }

  bool cloudWantsValveOpen = doc["valveIsOpen"];

  if (cloudWantsValveOpen && doc["duration"].is<int>() && doc["duration"].as<int>() > 0) {
    startWateringTimer(doc["duration"].as<unsigned long>());
  } else if (!cloudWantsValveOpen) {
    cancelWateringTimer();
  }

  if (cloudWantsValveOpen != isValveOpen) {
    isValveOpen = cloudWantsValveOpen;
    updatePhysicalValve();
    Serial.println("📱 Valve state changed via CLOUD!");
    sendTelemetryPayload();
  }
}

// --- WATERING TIMER ---
void startWateringTimer(unsigned long minutes) {
  if (minutes > MAX_WATERING_MINUTES) minutes = MAX_WATERING_MINUTES;
  unsigned long requestedMs = minutes * 60UL * 1000UL;
  unsigned long now = millis();

  // Overlapping schedules: keep whichever run ends later instead of cutting the current one short
  if (wateringTimerActive) {
    unsigned long remainingMs = wateringDurationMs - (now - wateringStartMs);
    if (remainingMs >= requestedMs) {
      Serial.println("⏱️ Timer already runs longer; keeping current end time.");
      return;
    }
  }

  wateringTimerActive = true;
  wateringStartMs = now;
  wateringDurationMs = requestedMs;
  Serial.println("⏱️ Timed watering: auto-close in " + String(minutes) + " min");
}

void cancelWateringTimer() {
  if (wateringTimerActive) {
    wateringTimerActive = false;
    Serial.println("⏱️ Watering timer cancelled.");
  }
}

void checkWateringTimer() {
  // Unsigned subtraction stays correct across the ~49-day millis() rollover
  if (wateringTimerActive && millis() - wateringStartMs >= wateringDurationMs) {
    wateringTimerActive = false;
    if (isValveOpen) {
      isValveOpen = false;
      updatePhysicalValve();
      Serial.println("⏱️ Watering duration finished: valve CLOSED automatically.");
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

// Non-blocking: makes one attempt at most every 5 seconds so loop() (button, sensors and the
// watering timer) keeps running while the broker is unreachable.
void reconnect() {
  unsigned long now = millis();
  if (lastReconnectAttempt != 0 && now - lastReconnectAttempt < reconnectInterval) return;
  lastReconnectAttempt = now;

  Serial.print("Attempting Secure MQTT connection... ");

  // Generate a unique ID to prevent HiveMQ from kicking us off
  String clientId = "AgriGuard-ESP32-";
  clientId += String(random(0, 0xffff), HEX);

  // Connect WITH the HiveMQ Cloud Username and Password
  if (mqttClient.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
    Serial.println("[✓] Connected to Private Broker!");
    mqttClient.subscribe(commandTopic);
  } else {
    Serial.print("FAILED, rc=");
    Serial.print(mqttClient.state());
    Serial.println(" (Trying again in 5 seconds)");
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
  } else {
    mqttClient.loop();
  }

  // 0. TIMED WATERING AUTO-CLOSE (runs even while offline)
  checkWateringTimer();

  // 1. LOCAL BUTTON OVERRIDE
  bool buttonState = digitalRead(BUTTON_PIN);
  if (buttonState == LOW && lastButtonState == HIGH) {
    delay(50); 
    isValveOpen = !isValveOpen; 
    cancelWateringTimer(); // Manual override takes over from any scheduled run
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
      baseFlow = random(400, 551) / 100.0; 
  }else {
      // Valve is closed. Base flow is strictly zero.
      baseFlow = 0.00;
  }

  // Read the raw potentiometer value (0 to 4095)
  int rawAnalogValue = analogRead(POT_PIN);

  // AGGRESSIVE DEADZONE: Ignore electrical static under 800
  float anomalyFlow = 0.0;
  if (rawAnalogValue > 800) { 
      // Map the remaining knob movement (800 - 4095) to an extra 0 - 80 LPM leak
      anomalyFlow = map(rawAnalogValue, 800, 4095, 0, 80);
  }

  // Calculate the final flow rate
  // If valve is closed (0.00) but pot is turned, it simulates a burst pipe!
  currentFlowRate = baseFlow + anomalyFlow;
  // 4. TRANSMIT TELEMETRY
  if (millis() - lastSyncTime >= syncInterval) {
    lastSyncTime = millis();
    sendTelemetryPayload();
    }
  }

void sendTelemetryPayload() {
  String jsonPayload = "{";
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
