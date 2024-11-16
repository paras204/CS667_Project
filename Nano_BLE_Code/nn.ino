// #include <Arduino.h>
// #include "tensorflow/lite/micro/micro_mutable.h"
// #include "tensorflow/lite/micro/micro_interpreter.h"
// #include "tensorflow/lite/schema/schema_generated.h"
// #include "tensorflow/lite/version.h"
// #include "stress_model_hex.h" // The model file

// // Constants
// constexpr int kTensorArenaSize = 10 * 1024;
// uint8_t tensor_arena[kTensorArenaSize];

// // TensorFlow Lite globals
// const tflite::Model* model = nullptr;
// tflite::MicroInterpreter* interpreter = nullptr;
// TfLiteTensor* input = nullptr;
// TfLiteTensor* output = nullptr;

// // Function to initialize the model and interpreter
// void setupModel() {
//     // Load the model
//     model = tflite::GetModel(stress_model_tflite);
//     if (model->version() != TFLITE_SCHEMA_VERSION) {
//         Serial.println("Model schema version does not match!");
//         while (1) delay(100); // Infinite loop if model is incompatible
//     }

//     // Set up the interpreter
//     static tflite::AllOpsResolver resolver;
//     static tflite::MicroInterpreter static_interpreter(
//         model, resolver, tensor_arena, kTensorArenaSize);
//     interpreter = &static_interpreter;

//     // Allocate memory from the tensor_arena for the model's tensors
//     if (interpreter->AllocateTensors() != kTfLiteOk) {
//         Serial.println("Tensor allocation failed!");
//         while (1) delay(100);
//     }

//     // Get pointers to input and output tensors
//     input = interpreter->input(0);
//     output = interpreter->output(0);
// }

// // Function to run inference
// int predictStressLevel(float blood_oxygen, float limb_movement, float temp, float heart_rate) {
//     // Load input data into the model
//     input->data.f[0] = blood_oxygen;
//     input->data.f[1] = limb_movement;
//     input->data.f[2] = temp;
//     input->data.f[3] = heart_rate;

//     // Run the model
//     if (interpreter->Invoke() != kTfLiteOk) {
//         Serial.println("Inference failed!");
//         return -1;
//     }

//     // Find the output with the highest probability
//     float max_prob = output->data.f[0];
//     int stress_level = 0;
//     for (int i = 1; i < 5; i++) { // Assuming stress levels from 0 to 4
//         if (output->data.f[i] > max_prob) {
//             max_prob = output->data.f[i];
//             stress_level = i;
//         }
//     }

//     return stress_level;
// }

// void setup() {
//     Serial.begin(115200);
//     delay(1000);

//     setupModel();
//     Serial.println("Model is ready for inference.");
// }

// void loop() {
//     // Replace these with actual sensor values
//     float blood_oxygen = 98.0;
//     float limb_movement = 0.2;
//     float temp = 36.5;
//     float heart_rate = 72.0;

//     int stress_level = predictStressLevel(blood_oxygen, limb_movement, temp, heart_rate);
//     Serial.print("Predicted stress level: ");
//     Serial.println(stress_level);

//     delay(2000); // Run inference every 2 seconds
// }