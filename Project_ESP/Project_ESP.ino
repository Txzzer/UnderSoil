/*
  AgriGuard IoT Node - Capstone Demonstration Code (ESP-WROOM-32)
  
  This code reads an analog potentiometer to simulate fluctuating water flow,
  listens for a physical button press (simulating a mobile app override command),
  and controls a SERVO MOTOR to physically demonstrate a valve opening/closing.
*/

#include <ESP32Servo.h> // You must install the "ESP32Servo" library in Arduino IDE!
#include <WiFi.h>       // NEW: Required to connect to the network
#include <HTTPClient.h> // NEW: Required to send the HTTP POST request
#include <time.h>
#include <WiFiClientSecure.h>

// --- Wi-Fi & API Configuration (NEW) ---
// IMPORTANT: ESP32 only connects to 2.4GHz Wi-Fi. (Use a mobile hotspot for the easiest setup!)
const char* ssid = "YOUR-2.4GHZ-WIFI-SSID";
const char* password = "YOUR-2.4GHZ-WIFI-PASSWORD";

// Replace the IP with Student 2's computer IP address on the network!
const char* apiEndpoint = "YOUR-DEV-TUNNELS-LINK"; 

// --- Pin Definitions (Updated for ESP-WROOM-32) ---
const int POT_PIN = 34;    // ADC1 pin for Trimmer Potentiometer
const int BTN_PIN = 25;    // Digital pin for Push Button
const int SERVO_PIN = 26;  // PWM pin for the Servo Motor

// --- State Variables ---
bool isValveOpen = false;
float currentFlowRate = 0.0;
int lastButtonState = HIGH;
unsigned long lastDebounceTime = 0;
const unsigned long debounceDelay = 50;
unsigned long lastSyncTime = 0;
const unsigned long syncInterval = 5000;

Servo valveServo;

void setup() {
  Serial.begin(115200);
  Serial.println("Starting AgriGuard IoT Node (HTTPS Mode)...");

  pinMode(POT_PIN, INPUT);
  pinMode(BTN_PIN, INPUT_PULLUP);

  valveServo.attach(SERVO_PIN);
  valveServo.write(0);
  delay(500);

  Serial.print("Connecting to Wi-Fi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("Wi-Fi Connected successfully!");
  Serial.print("ESP32 IP Address: ");
  Serial.println(WiFi.localIP());

  configTime(7200, 0, "pool.ntp.org");
  Serial.println("Time synchronized with NTP server.");
}

void loop() {
  int reading = digitalRead(BTN_PIN);

  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }
  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (reading == LOW) {
      isValveOpen = !isValveOpen;

      if (isValveOpen) {
        valveServo.write(90);
        Serial.println("COMMAND RECEIVED: Valve OPENED.");
      } else {
        valveServo.write(0);
        Serial.println("COMMAND RECEIVED: Valve CLOSED.");
      }
      delay(300);
    }
  }
  lastButtonState = reading;

  if (isValveOpen) {
    int rawAnalogValue = analogRead(POT_PIN);
    currentFlowRate = (rawAnalogValue / 4095.0) * 120.0;
  } else {
    currentFlowRate = 0.0;
  }

  if (millis() - lastSyncTime >= syncInterval) {
    lastSyncTime = millis();
    sendTelemetryPayload();
  }
}

void sendTelemetryPayload() {
  struct tm timeinfo;
  String timestamp = "2026-08-06T00:00:00.000Z";
  if (getLocalTime(&timeinfo)) {
    char timeStringBuff[50];
    strftime(timeStringBuff, sizeof(timeStringBuff), "%Y-%m-%dT%H:%M:%S.000Z", &timeinfo);
    timestamp = String(timeStringBuff);
  }

  String jsonPayload = "{";
  jsonPayload += "\"id\": 0,";
  jsonPayload += "\"deviceId\": \"ESP32_WROOM_ZONE_1\",";
  jsonPayload += "\"flowRateLitersPerMin\": " + String(currentFlowRate, 2) + ",";
  jsonPayload += "\"valveIsOpen\": " + String(isValveOpen ? "true" : "false") + ",";
  jsonPayload += "\"timestamp\": \"" + timestamp + "\"";
  jsonPayload += "}";

  Serial.println("--- TRANSMITTING TO C# API ---");
  Serial.println(jsonPayload);

  if (WiFi.status() == WL_CONNECTED) {
    // --- CRITICAL SSL BYPASS ---
    WiFiClientSecure secureClient;
    secureClient.setInsecure(); // This tells the ESP32 to ignore the fake C# developer certificate

    HTTPClient http;
    // Pass the secureClient to HTTPClient
    http.begin(secureClient, apiEndpoint);

    http.addHeader("Content-Type", "application/json");

    int httpResponseCode = http.POST(jsonPayload);

    if (httpResponseCode > 0) {
      Serial.print("SUCCESS! HTTP Response code: ");
      Serial.println(httpResponseCode);
    } else {
      Serial.print("ERROR! Connection failed. Code: ");
      Serial.println(httpResponseCode);
      Serial.println("(Check C# launchSettings.json, firewall, or API URL)");
    }

    http.end();
  } else {
    Serial.println("Error: ESP32 disconnected from Wi-Fi.");
  }

  Serial.println("------------------------------");
}
