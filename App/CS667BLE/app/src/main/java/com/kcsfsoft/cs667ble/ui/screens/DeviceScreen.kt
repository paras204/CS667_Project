package com.kcsfsoft.cs667ble.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kcsfsoft.cs667ble.ble.SERVICE_UUID
import com.kcsfsoft.cs667ble.ui.viewModel.DataEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

fun xorString(input: String, key: Char): String {
    val output = StringBuilder()
    for (char in input) {
        output.append((char.code xor key.code).toChar())
    }
    return output.toString()
}

fun splitTopLevel(jsonString: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var level = 0
    var current = StringBuilder()

    for (char in jsonString) {
        when (char) {
            '{', '[' -> level++
            '}', ']' -> level--
            delimiter -> {
                if (level == 0) {
                    result.add(current.toString())
                    current = StringBuilder()
                    continue
                }
            }
        }
        current.append(char)
    }
    if (current.isNotEmpty()) {
        result.add(current.toString())
    }

    return result
}

fun parseJson(json: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val jsonString = json.trim().removeSurrounding("{", "}")
    val keyValuePairs = splitTopLevel(jsonString, ',')

    for (pair in keyValuePairs) {
        val (key, value) = pair.split(":").map { it.trim().removeSurrounding("\"") }
        result[key] = parseValue(xorString(value, 0x20.toChar()))
    }
    return result
}

fun parseValue(value: String): Any {
    val cleanedValue = value.replace("\"", "")
    return when {
        cleanedValue.startsWith("{") && cleanedValue.endsWith("}") -> parseJson(cleanedValue) // Handle nested JSON
        else -> cleanedValue
    }
}

@Composable
fun LineGraph(parsedData: List<DataEntry>, key: String = "sensor_value") {
    val graphPadding = 16.dp
    val dataPoints =
        parsedData.mapNotNull { it.data[key] as? Double } // Replace `it.data[key]` with actual key to plot

    val maxDataPoint = dataPoints.maxOrNull() ?: 0.0
    val minDataPoint = dataPoints.minOrNull() ?: 0.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = graphPadding)
    ) {
        if (dataPoints.isNotEmpty()) {
            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                val x =
                    (index * (size.width / (dataPoints.size - 1))).toFloat() // Convert x to Float
                val normalizedValue =
                    ((value - minDataPoint) / (maxDataPoint - minDataPoint)).toFloat() // Convert normalizedValue to Float
                val y =
                    (size.height - (normalizedValue * size.height)).toFloat() // Convert y to Float

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path, color = Color.Blue, style = Stroke(width = 4f))
        }
    }
}

@Composable
fun DeviceScreen(
    unselectDevice: () -> Unit,
    isDeviceConnected: Boolean,
    discoveredCharacteristics: Map<String, List<String>>,
    dataRead: String?,
    predRead: Int?,
    connect: () -> Unit,
    discoverServices: () -> Unit,
    readData: () -> Unit,
    readPrediction: () -> Unit
) {
    val _parsedData = MutableStateFlow<List<DataEntry>>(emptyList())
    val parsedData: StateFlow<List<DataEntry>> = _parsedData.asStateFlow()
    val foundTargetService = discoveredCharacteristics.contains(SERVICE_UUID.toString())
    dataRead?.let {
        try {
            val parsedJson = parseJson(it)

            val entry = DataEntry(parsedJson)
            _parsedData.update { currentList ->
                currentList + entry
            }
        } catch (e: Exception) {
            // Handle parsing error
            Log.e("error", "JSON Parsing Failed !!")
        }
    }

    Column(
        Modifier
            .scrollable(rememberScrollState(), Orientation.Vertical)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = connect) {
                Text("Connect")
            }

            OutlinedButton(onClick = unselectDevice) {
                Text("Disconnect")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Connection Status: ${if (isDeviceConnected) "Connected" else "Disconnected"}")
        }

        // Trigger service discovery and data reading after connecting
        LaunchedEffect(isDeviceConnected) {
            if (isDeviceConnected) {
                discoverServices()
                readData()
                readPrediction()
            }
        }

        // Display parsed data
        val currentParsedData = parsedData.collectAsState().value
        if (currentParsedData.isNotEmpty()) {
            currentParsedData.forEach { entry ->
                Text(
                    text = "Temperature: ${entry.data["temperature"]}°C",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Humidity: ${entry.data["humidity"]}%",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Acceleration - X: ${entry.data["ax"]}, Y: ${entry.data["ay"]}, Z: ${entry.data["az"]}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Heart Rate: ${entry.data["heartRate"]} BPM",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(
                text = "No data available",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Display prediction data
        Text(
            text = "Stress Level: ${predRead ?: "No prediction"}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Graph Section { Not working yet !! }
        // Text("Data Graph", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        // LineGraph(currentParsedData)
    }
}