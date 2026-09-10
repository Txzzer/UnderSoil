import paho.mqtt.client as mqtt
import time
import json
import random
import ssl
from datetime import datetime, timezone

# 1. HiveMQ Connection Settings
BROKER = "fe7c1a0013c74b819748355010956172.s1.eu.hivemq.cloud"
PORT = 8883
TOPIC_TELEMETRY = "undersoil/telemetry"
TOPIC_COMMANDS = "undersoil/commands"
USERNAME = "esp32-device"
PASSWORD = "Travi$304"
CLIENT_ID = f"AgriGuard-Simulator-{random.randint(1000, 9999)}"

# Track the mocked hardware state locally
valve_state = False

def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        print(f"✅ Connected to HiveMQ as {CLIENT_ID}")
        # Subscribe to the commands topic so the simulator reacts to the mobile app
        client.subscribe(TOPIC_COMMANDS)
        print(f"🎧 Listening for Android commands on: {TOPIC_COMMANDS}\n")
    else:
        print(f"❌ Connection failed, code {reason_code}")

def on_message(client, userdata, msg):
    global valve_state
    try:
        command = json.loads(msg.payload.decode())
        if "valveIsOpen" in command:
            # Update the local state based on the Android API request
            valve_state = command["valveIsOpen"]
            state_str = "OPEN" if valve_state else "CLOSED"
            print(f"\n⚡ COMMAND RECEIVED: Servo motor physically turned. Valve is now {state_str}!\n")
    except Exception as e:
        print(f"Failed to parse command: {e}")

# 2. Client Setup (Using modern paho-mqtt v2.0+ architecture)
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=CLIENT_ID)
client.username_pw_set(USERNAME, PASSWORD)
client.tls_set(tls_version=ssl.PROTOCOL_TLS) # Required for HiveMQ port 8883
client.on_connect = on_connect
client.on_message = on_message

# 3. Connect to Broker in the background
client.connect(BROKER, PORT)
client.loop_start()

print("🌱 Starting AgriGuard ESP32 Simulator... (Press Ctrl+C to stop)")
try:
    while True:
        # Simulate realistic logic (flow rate drops to 0 when the valve is closed)
        flow = round(random.uniform(4.0, 5.5), 2) if valve_state else 0.0
        
        payload = {
            "deviceId": "ESP32-Simulator",
            "flowRateLitersPerMin": flow,
            "valveIsOpen": valve_state,
            "temperatureCelsius": round(random.uniform(18.0, 26.0), 1),
            "humidityPercentage": round(random.uniform(40.0, 60.0), 1),
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
        json_payload = json.dumps(payload)
        client.publish(TOPIC_TELEMETRY, json_payload, qos=1)
        
        print(f"📤 Published: {json_payload}")
        time.sleep(5) # Matches your physical ESP32 millis() timer

except KeyboardInterrupt:
    print("\n🛑 Simulator stopped.")
    client.loop_stop()
    client.disconnect()