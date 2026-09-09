# UnderSoil
Phase 1: The "Don't Panic" Step-by-Step Guide 

The Main Rule of Phase 1: You are NOT allowed to connect your tools together yet. You must prove your specific tool works on your specific laptop first. 

🐙 Step 0: The GitHub Repository (Whole Team) 

Before anyone writes a single line of code, you need a shared place to put it. 

Create the Repo: Student 2 (C# Architect) or Student 1 (Android Lead) needs to go to GitHub.com and create a new, private repository called AgriGuard-Capstone. 

Add Collaborators: Go to Settings -> Collaborators and invite the other three team members. 

Clone it: Every student must download GitHub Desktop (or use the command line if they are comfortable) and "clone" this empty repository to their desktop. 

Create Folders: Inside the main AgriGuard-Capstone folder on your computer, create four sub-folders: 

/backend (For Student 2) 

/android (For Student 1) 

/ai-model (For Student 3) 

/hardware-sim (For Student 4) 

 

🛠️ Master Software (IDE) Checklist 

Before doing anything, make sure you download the exact correct program for your role. Do not guess! 

Student 1 (Android): Download Android Studio (The green Android robot icon). 

Student 2 (C# Backend): Download Visual Studio 2022 Community (The PURPLE infinity icon. Do NOT use the blue one). 

Student 3 (Python / AI): Download Visual Studio Code / VS Code (The BLUE infinity icon) and install Python. 

Student 4 (Hardware): Download the Arduino IDE (The teal infinity/minus-plus icon). 

👩‍💻 Student 2: The C# Backend (Visual Studio Purple) 

Your goal: Get a fake webpage (Swagger) to load on your screen showing that your API is alive. 

Download the right software: Go to Microsoft and download Visual Studio 2022 Community Edition. (Make sure it is the PURPLE icon. Do NOT use the blue Visual Studio Code icon). 

Install the right pieces: When installing, it will ask what "Workloads" you want. Check the box that says "ASP.NET and web development". 

Make the project: 

Open Visual Studio. Click Create a new project. 

Search for "ASP.NET Core Web API" (Make sure it has C# tags, not F# or Visual Basic). 

Name it AgriGuardAPI. Keep clicking Next. Leave the defaults (usually .NET 8). 

The "Hello World" Test: 

Do not write any code yet. 

Look at the top middle of the screen. There is a green "Play" triangle button (it might say "https" or "AgriGuardAPI"). Click it. 

A black terminal window will pop up, and then your web browser will open a page called Swagger. It will have a fake endpoint called /WeatherForecast. 

If you see this page, you win Phase 1. Your backend is alive. 

📱 Student 1: The Android App 

Your goal: Get a fake phone to appear on your computer screen with a blank app. 

Download the software: Download and install Android Studio. It is huge. It will take a long time. 

Make the project: 

Open Android Studio, click New Project. 

Choose "Empty Views Activity" (or just Empty Activity). 

Name it AgriGuard. 

Set Language to Kotlin (Do not use Java, it's 2026, Kotlin is way easier). 

WAIT. When the project opens, look at the very bottom right of the screen. There is a loading bar saying "Gradle Syncing". DO NOT CLICK ANYTHING UNTIL THIS BAR DISAPPEARS. If you interrupt Gradle, Android Studio will break and you will be very sad. 

The "Hello World" Test: 

Look at the top menu. Click the Device Manager icon (looks like a little phone). 

Click Create Device. Pick a Pixel phone. Download the recommended Android system image. 

Once created, click the Green "Play" button at the top of Android Studio. 

A fake phone (emulator) will pop up on your screen, and an app saying "Hello Android" will open. 

If you see this, you win Phase 1. 

🐍 Student 3: The Python / AI Lead 

https://www.kaggle.com/code/saurabhjejurkar/crop-disease-detection 

Your goal: Download the pictures of the sick leaves and prove you can open them in Python. 

Download the software: You do use the blue one. Download Visual Studio Code (VS Code) and install Python on your laptop. 

Get the Dataset: 

Go to Kaggle.com and create a free account. 

Search for "PlantVillage Dataset". 

Download it. It will be a massive ZIP file. Extract it into a folder on your Desktop called AgriGuard_AI. 

The "Hello World" Test: 

Open VS Code, open that AgriGuard_AI folder. 

Open your terminal in VS Code and type: pip install tensorflow opencv-python matplotlib. Press Enter and wait for it to download. 

Make a file called test.py. Write 3 lines of code to open one of the leaf pictures and show it on the screen. 

If a picture of a leaf pops up on your screen when you run the script, you win Phase 1. 

🔌 Student 4: The Hardware / Digital Dummy Lead 

Your goal: Make a simple Python or C# script talk to your computer by sending a fake payload. 

The Pivot: Since you are building a digital dummy, you no longer need the Arduino IDE. You will build a simulator. 

Download the software: You will use Visual Studio Code (VS Code) (the blue one) with Python installed, or Visual Studio 2022 (the purple one) to build a quick C# Console App. Let's use Python for speed. 

Setup the script: 

Open VS Code and create a new folder called IoT_Simulator. 

Create a file named simulator.py. 

Open the terminal and run pip install requests (this allows you to send data over the internet). 

The "Hello World" Test: 

Write a simple script that generates a random number between 38 and 42 (your fake water flow) and prints it to the screen every 5 seconds. 

If you press play and see numbers printing out in your terminal every 5 seconds, you win Phase 1. (In Phase 2, we will make it send those numbers to Student 2's C# API). 

 

 

Phase 2: The "Happy Path" Connectivity (Weeks 3-4) 

The Main Rule of Phase 2: We are forcing the different laptops to talk to each other over your Wi-Fi network. No complex logic yet—just prove data can travel from A to B. 

🛑 THE MOST IMPORTANT STEP: The Networking Rule 

Everyone must be on the SAME Wi-Fi network. 

Student 2 (The C# Backend Lead), you are the Server. You must find your laptop's IP address and give it to the rest of the team. 

Open your Windows Start Menu, type cmd, and hit Enter. 

Type ipconfig and hit Enter. 

Look for IPv4 Address. It will look something like 192.168.1.15 or 10.0.0.4. 

Write this number on a piece of paper and put it on the table. Rule: The rest of the team will NEVER use localhost. They will use this IP address. 

👩‍💻 Student 2: The C# Backend (Visual Studio Purple) 

Your goal: Receive fake data from Student 4 and prove it arrived. 

Open the API to the network: 

By default, Visual Studio hides your API from other computers. 

Open a file called Properties/launchSettings.json. 

Find the applicationUrl line. Change it from http://localhost:5000 to http://0.0.0.0:5000 (this tells your laptop to accept connections from any other laptop on the Wi-Fi). 

Make the Endpoint: 

Create a simple POST endpoint that accepts Student 4's IoT JSON data and just uses Console.WriteLine() to print it to the screen. (Don't worry about saving to the database just yet). 

The Test: 

Press the Green Play button. 

Tell Student 4: "Hit me." 

If you see Student 4's fake water data print out in your black terminal window, you win Phase 2. 

🔌 Student 4: The Hardware / Simulator (VS Code Blue) 

Your goal: Shoot your fake water data across the Wi-Fi into Student 2's computer. 

Prepare the Payload: 

Open your Python script from Phase 1. 

Make sure it is generating a JSON dictionary that looks like this: {"deviceId": "Sim_01", "flowRateLitersPerMin": 42.5}. 

Aim at Student 2: 

Use the requests library to send an HTTP POST. 

The URL you aim at is: http://<Student_2_IP_Address>:5000/api/v1/telemetry/ingest 

The Test: 

Press play on your script. 

It should say Response 200 OK. 

If Student 2 yells "I got it!", you win Phase 2. 

📱 Student 1: The Android App (Android Studio) 

Your goal: Press a button on your phone and send a "Hello" message to Student 2's computer. 

The Classic Trap (Internet Permission): 

Open AndroidManifest.xml. You MUST add this line at the top, or Android will block all internet traffic: <uses-permission android:name="android.permission.INTERNET" /> 

Also, because you are testing on local HTTP (not HTTPS), add android:usesCleartextTraffic="true" to your <application> tag. 

Build the Test Button: 

Put a big button on your app screen that says "Test API". 

Use a library called Retrofit (or even just OkHttp) to send a basic GET or POST request to http://<Student_2_IP_Address>:5000/api/v1/... when clicked. 

The Test: 

Click the button on your Android emulator/phone. 

If the app shows a Toast message saying "Success" and Student 2 sees your request on their screen, you win Phase 2. 

🐍 Student 3: The Python / AI Lead (VS Code Blue) 

Your goal: Actually create the first version of the "Brain" and hand it over to Student 1. 

Train a "Dummy" Model: 

Don't worry about training a perfect 99% accuracy model yet. That takes weeks. 

Just write a Python script that takes a tiny subset of the PlantVillage dataset (e.g., 50 healthy leaves, 50 sick leaves). 

Train a basic MobileNetV2 model using TensorFlow/Keras. 

The Export: 

The most important step for you in Phase 2 is proving you can convert your massive Python model into a tiny mobile file. 

Run the TensorFlow Lite Converter code in your script. It will spit out a file called model.tflite. 

The Test: 

Take that model.tflite file and physically hand it (via Slack/Email/Flash drive) to Student 1 (Android). 

If you successfully generate this file and give it to Android, you win Phase 2. (In Phase 3, Student 1 will actually plug it into the app). 

-------------New things....

👩‍💻 Student 2: The C# Backend
Your goal is to connect your existing API controllers to your MQTT client so the web server can send commands outward.

Inject the MQTT Client: Ensure the IMqttClient you configured in your setup is injected into your controllers (like ValveController and TelemetryController) via their constructors.

Build the Command Endpoint: Create an HTTP POST endpoint (e.g., /api/v1/telemetry/request-read). When the Android app hits this, use the MQTT client to publish a simple message (like "READ_NOW") to a brand new topic, such as undersoil/commands.

Build the Fetch Endpoint: Create an HTTP GET endpoint (e.g., /api/v1/telemetry/latest) that queries your SQLite database, pulls the single most recent row from your telemetry table, and returns it to the phone as JSON.

🔌 Student 4: The Hardware Lead
Your ESP needs to stop just talking and start listening for instructions from the server.

Subscribe to Commands: Update the C++ or MicroPython code on the ESP to subscribe to the new undersoil/commands topic on HiveMQ.

Listen and React: Write a callback function on the ESP so that when it receives the "READ_NOW" payload, it immediately wakes up and polls the physical sensors.

Return the Data: Package that brand-new reading into your standard JSON format and publish it immediately back to your original undersoil/telemetry topic (which your C# app is already perfectly configured to catch and save).

📱 Student 1: The Android App
Your goal is to orchestrate this flow from the user's hand and display the fresh results.

The Trigger Button: Create a "Get Live Reading" button. When pressed, use Retrofit to send an HTTP POST to the backend's /request-read endpoint.

The Delay: Because MQTT takes a split second to travel to the hardware and back to the database, have the app show a loading spinner and wait about 1 to 2 seconds.

Fetch and Display: Send an HTTP GET to the /latest endpoint, parse the fresh JSON data, stop the loading spinner, and update the text views on the screen.

 

 
