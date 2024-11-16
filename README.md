# CS667_Project

1. Generating Model.h
Requirements

pandas
sklearn
joblib
micromlgen

pip install pandas scikit-learn joblib micromlgen
Run the CS667_Model_2.ipynb file

2. Uploading Arduino Code
Libraries

Ensure that the following libraries are installed in the Arduino IDE:

Arduino_LSM9DS1
Arduino_HTS221
ArduinoBLE
EloquentML (For Random Forest model)
You can install these libraries through the Arduino Library Manager.

Hardware Setup

Heart Rate Sensor: Connect the heart rate sensor to analog pin A0.
Open the Arduino IDE and load the new.ino code.
Select the correct board (Arduino Nano 33 BLE Sense) and port.
Upload the code to the device.

3. Running App
Install the .apk file and scan to connect with BLE. the data will be displayed after an initial delay of 2-3 seconds then it will be real time.