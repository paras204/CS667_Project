// #include <Arduino_LSM9DS1.h>
// #include <Arduino_HTS221.h>
// #include <MAX30100_PulseOximeter.h>
// #include <ArduinoBLE.h>

// #define REPORTING_PERIOD_MS 1000  // Reporting interval for MAX30100 sensor
// #define INT_PIN 2                 // Connect the INT pin of the MAX30100 to D2 on Arduino

// PulseOximeter pox;
// uint32_t tsLastReport = 0;

// // BLE Service and Characteristics
// BLEService sensorService("180D"); // UUID for the sensor service
// BLEFloatCharacteristic tempChar("2A6E", BLERead | BLENotify);      // Temperature
// BLEFloatCharacteristic humidChar("2A6F", BLERead | BLENotify);     // Humidity
// BLEFloatCharacteristic accelXChar("2A77", BLERead | BLENotify);    // Accelerometer X
// BLEFloatCharacteristic accelYChar("2A78", BLERead | BLENotify);    // Accelerometer Y
// BLEFloatCharacteristic accelZChar("2A79", BLERead | BLENotify);    // Accelerometer Z
// BLEFloatCharacteristic heartRateChar("2A37", BLERead | BLENotify); // Heart rate
// BLEFloatCharacteristic spo2Char("2A5F", BLERead | BLENotify);      // SpO2

// void onBeatDetected() {
//   Serial.println("Beat detected!");
// }

// void handleNewData() {
//   // This function will be called whenever INT_PIN goes HIGH, indicating new data is available
//   Serial.println("New data available from MAX30100");
// }

// void setup() {
//   Serial.begin(9600);
//   while (!Serial);

//   // Initialize sensors
//   if (!IMU.begin()) {
//     Serial.println("Failed to initialize IMU!");
//     while (1);
//   }

//   if (!HTS.begin()) {
//     Serial.println("Failed to initialize temperature and humidity sensor!");
//     while (1);
//   }

//   if (!pox.begin()) {
//     Serial.println("Failed to initialize MAX30100 sensor!");
//     while (1);
//   } else {
//     Serial.println("MAX30100 initialized.");
//   }
//   pox.setOnBeatDetectedCallback(onBeatDetected);

//   // Initialize INT_PIN as an input and attach interrupt
//   pinMode(INT_PIN, INPUT);
//   attachInterrupt(digitalPinToInterrupt(INT_PIN), handleNewData, RISING);

//   // Initialize BLE
//   if (!BLE.begin()) {
//     Serial.println("Starting BLE failed!");
//     while (1);
//   }

//   // Configure BLE service and characteristics
//   BLE.setLocalName("Nano33_Sensor");      // Name of the BLE device
//   BLE.setAdvertisedService(sensorService); // Advertise the service
//   sensorService.addCharacteristic(tempChar);
//   sensorService.addCharacteristic(humidChar);
//   sensorService.addCharacteristic(accelXChar);
//   sensorService.addCharacteristic(accelYChar);
//   sensorService.addCharacteristic(accelZChar);
//   sensorService.addCharacteristic(heartRateChar);
//   sensorService.addCharacteristic(spo2Char);
//   BLE.addService(sensorService);

//   BLE.advertise();
//   Serial.println("BLE device active, waiting for connections...");
// }

// void loop() {
//   float spo2 = pox.getSpO2();
//   Serial.println(pox.getHeartRate());
//   Serial.println(spo2);
//   delay(1000);
//   BLEDevice central = BLE.central();

//   // If a central device is connected
//   if (central) {
//     Serial.print("Connected to central: ");
//     Serial.println(central.address());

//     while (central.connected()) {
//       float temperature = HTS.readTemperature();
//       float humidity = HTS.readHumidity();

//       float x, y, z;
//       if (IMU.accelerationAvailable()) {
//         IMU.readAcceleration(x, y, z);
//       }

//       pox.update();
//       float heartRate = pox.getHeartRate();
//       float spo2 = pox.getSpO2();

//       // Print values to Serial Monitor
//       Serial.print("Heart Rate: ");
//       Serial.println(heartRate);
//       Serial.print("SpO2: ");
//       Serial.println(spo2);

//       // Update BLE characteristics
//       tempChar.writeValue(temperature);
//       humidChar.writeValue(humidity);
//       accelXChar.writeValue(x);
//       accelYChar.writeValue(y);
//       accelZChar.writeValue(z);
//       heartRateChar.writeValue(heartRate);
//       spo2Char.writeValue(spo2);

//       delay(1000); // Delay for readability and to match the reporting period
//     }

//     Serial.println("Disconnected from central.");
//   }
//   delay(1000);
// }

// #define USE_ARDUINO_INTERRUPTS true    // Set-up low-level interrupts for accurate BPM measurement
// #include <PulseSensorPlayground.h>     // Include the PulseSensorPlayground Library
// #include "model.h"

// Eloquent::ML::Port::RandomForest clf;
// const int PulseWire = A2;       // 'S' Signal pin connected to A0 on Arduino Nano 33 BLE Sense
// const int LED_PIN = LED_BUILTIN;  // Use the built-in LED on the Nano 33 BLE Sense
// int Threshold = 550;             // Threshold to detect beats

// PulseSensorPlayground pulseSensor;  // Create a PulseSensor object

// void setup() {
//   Serial.begin(9600);

//   // Configure the PulseSensor object, by assigning our variables to it
//   pulseSensor.analogInput(PulseWire);   
//   pulseSensor.blinkOnPulse(LED_PIN);    // Blink the built-in LED with heartbeat
//   pulseSensor.setThreshold(Threshold);  // Set threshold for beat detection

//   // Double-check that the "pulseSensor" object was created successfully
//   if (pulseSensor.begin()) {
//     Serial.println("PulseSensor object created and initialized!");
//   }
//   else {
//     Serial.println("PulseSensor object lol!");
//   }
// }

// void loop() {
//   float irisSample[4] = {89.232 ,15.080,91.232,71.16};
//   Serial.println(clf.predict(irisSample));

//   int myBPM = pulseSensor.getBeatsPerMinute();  // Calculate BPM

//   if (pulseSensor.sawStartOfBeat()) {           // Check if a beat was detected
//     Serial.println("♥ A HeartBeat Happened!");   // Print message when a beat is detected
//     Serial.print("BPM: ");
//     Serial.println(myBPM);                       // Print BPM value
//   }

//   delay(200);  // Short delay to stabilize readings
// }