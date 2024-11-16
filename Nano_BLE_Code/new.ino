#include <Arduino_LSM9DS1.h>
#include <Arduino_HTS221.h>
#include <ArduinoBLE.h>
#define HEART_SENSOR_PIN A0
#define USE_ARDUINO_INTERRUPTS true

#define SERVICE_UUID "12345678-1234-5678-1234-56789abcdef0"
#define DATA_CHAR_UUID "23456781-1234-5678-1234-56789abcdef1"
#define PREDICTION_UUID "32456781-1234-5678-1234-56789abcdef1"

#include "model.h"
Eloquent::ML::Port::RandomForest clf;

BLEService sensorService(SERVICE_UUID);
BLECharacteristic dataCharacteristic(DATA_CHAR_UUID, BLERead | BLENotify, 300);
BLEIntCharacteristic predictionCharacteristic(PREDICTION_UUID, BLERead | BLENotify);

int sensorValue = 0;
float heartRate = 0;
unsigned long lastBeat = 0;
int beatCounter = 0;

void setup() {
  Serial.begin(9600);
  while (!Serial)
    ;

  if (!IMU.begin()) {
    Serial.println("Failed to initialize IMU!");
    while (1)
      ;
  }
  Serial.println("IMU initialized.");

  if (!HTS.begin()) {
    Serial.println("Failed to initialize temperature and humidity sensor!");
    while (1)
      ;
  }
  Serial.println("Temperature and humidity sensor initialized.");

  pinMode(HEART_SENSOR_PIN, INPUT);

  if (!BLE.begin()) {
    Serial.println("Starting Bluetooth failed!");
    while (1)
      ;
  }

  BLE.setLocalName("Nano33_BLE_Sensor");
  BLE.setAdvertisedService(sensorService);
  sensorService.addCharacteristic(dataCharacteristic);
  sensorService.addCharacteristic(predictionCharacteristic);
  BLE.addService(sensorService);

  BLE.advertise();
  Serial.println("Bluetooth device is now advertising...");
}

void loop() {
  BLEDevice central = BLE.central();
  if (central) {
    Serial.print("Connected to central device: ");
    Serial.println(central.address());

    while (central.connected()) {
      float temperature = HTS.readTemperature();
      float humidity = HTS.readHumidity();

      float x, y, z;
      if (IMU.accelerationAvailable()) {
        IMU.readAcceleration(x, y, z);
      }

      // Heart rate processing
      sensorValue = analogRead(HEART_SENSOR_PIN);
      if (sensorValue > 512) {
        if (millis() - lastBeat > 300) {
          lastBeat = millis();
          beatCounter++;
        }
      }

      if (millis() % 500 < 50) {
        heartRate = (beatCounter * 60.0 * 2);
        beatCounter = 0;
      }

      uint8_t accelData[12];
      memcpy(&accelData[0], &x, sizeof(float));
      memcpy(&accelData[4], &y, sizeof(float));
      memcpy(&accelData[8], &z, sizeof(float));

      float sample[4] = { 1.8 * temperature + 32, humidity, 3 * (x + y + z), 96.2 };
      float prediction = clf.predict(sample);

      char key = 0x20;
      
      String dataString = "{\"temperature\":";
      dataString += xorString(String(temperature), key);
      dataString += ",\"humidity\":";
      dataString += xorString(String(humidity), key);
      dataString += ",\"ax\":";
      dataString += xorString(String(x), key);
      dataString += ",\"ay\":";
      dataString += xorString(String(y), key);
      dataString += ",\"az\":";
      dataString += xorString(String(z), key);
      dataString += ",\"heartRate\":";
      dataString += xorString(String(heartRate), key);
      dataString += "\"}";

      const char* dataCStr = dataString.c_str();
      size_t dataLength = strlen(dataCStr);

      String dec_dataString = "{\"temperature\":";
      dec_dataString += String(temperature);
      dec_dataString += ",\"humidity\":";
      dec_dataString += String(humidity);
      dec_dataString += ",\"ax\":";
      dec_dataString += String(x);
      dec_dataString += ",\"ay\":";
      dec_dataString += String(y);
      dec_dataString += ",\"az\":";
      dec_dataString += String(z);
      dec_dataString += ",\"heartRate\":";
      dec_dataString += String(heartRate);
      dec_dataString += "\"}";
      
      dataCharacteristic.writeValue((uint8_t*)dataCStr, dataLength);
      // dataCharacteristic.writeValue(dataString);
      predictionCharacteristic.writeValue(int(prediction));
      Serial.println(dataString);
      Serial.println(dec_dataString);
      Serial.println(prediction);
      delay(1000);
    }

    Serial.print("Disconnected from central device: ");
    Serial.println(central.address());
  }
}

String xorString(String input, char key) {
  String output = input;

  for (int i = 0; i < input.length(); i++) {
    output[i] = input[i] ^ key;
  }

  return output;
}
