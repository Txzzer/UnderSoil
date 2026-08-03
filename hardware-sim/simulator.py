import time
import random
import requests # We imported this since you installed it, ready for when we connect to C#!
import json
from datetime import datetime

# --- NEW: Set Student 2's IP Address here ---
# (Ask Student 2 for their IPv4 address. Use "127.0.0.1" if you are testing on your own computer for now)
API_URL = "https://bkswk4fp-7124.inc1.devtunnels.ms/api/v1/telemetry/ingest"

print("--- AgriGuard IoT Simulator Started ---")
print(f"Aiming at Student 2's API: {API_URL}")
print("Generating simulated JSON water flow data...")
print("Press Ctrl+C to stop the script.")
print("-" * 40)

# We use a try/except block so if you press Ctrl+C, it shuts down cleanly
try:
    while True:
        # Generate a random number between 38 and 42 (rounded to 2 decimal places for realism)
        simulated_flow = round(random.uniform(38.0, 42.0), 2)
        
        # Create a Python dictionary (Updated keys to match Phase 2 instructions!)
        data_payload = {
            "deviceId": "Sim_01",
            "flowRateLitersPerMin": simulated_flow,
            "valveIsOpen": True,
            "timestamp": datetime.now().isoformat()
        }
        
        # Convert the Python dictionary into a JSON string
        json_output = json.dumps(data_payload)
        print(f"[\u2713] Transmitting: {json_output}")
        
        # --- NEW: Shoot the data over Wi-Fi to Student 2 ---
        try:
            # We use 'json=data_payload' so the requests library formats it perfectly for the C# API
            response = requests.post(API_URL, json=data_payload, timeout=3)
            # If successful, this will print "Response 200 OK"
            print(f"    -> Response {response.status_code} {response.reason}")
        except requests.exceptions.RequestException:
            # If Student 2's computer is off or the API isn't running, catch the error so we don't crash!
            print("    -> [!] Connection Failed: Is Student 2's server running?")
            
        # Pause the script for 5 seconds before looping again
        time.sleep(5)
        
except KeyboardInterrupt:
    print("\n--- Simulator Stopped ---")