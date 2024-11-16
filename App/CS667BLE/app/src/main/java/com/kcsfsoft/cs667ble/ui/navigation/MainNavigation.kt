package com.kcsfsoft.cs667ble.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kcsfsoft.cs667ble.ui.screens.DeviceScreen
import com.kcsfsoft.cs667ble.ui.screens.PermissionsRequiredScreen
import com.kcsfsoft.cs667ble.ui.screens.ScanningScreen
import com.kcsfsoft.cs667ble.ui.screens.haveAllPermissions
import com.kcsfsoft.cs667ble.ui.viewModel.BLEClientViewModel

@SuppressLint("MissingPermission")
@Composable
fun MainNavigation(viewModel: BLEClientViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var allPermissionsGranted by remember {
        mutableStateOf(haveAllPermissions(context))
    }

    if (!allPermissionsGranted) {
        PermissionsRequiredScreen { allPermissionsGranted = true }
    } else if (uiState.activeDevice == null) {
        ScanningScreen(isScanning = uiState.isScanning,
            foundDevices = uiState.foundDevices,
            startScanning = viewModel::startScanning,
            stopScanning = viewModel::stopScanning,
            selectDevice = { device ->
                viewModel.stopScanning()
                viewModel.setActiveDevice(device)
            })
    } else {
        val dataReadValue by viewModel.dataRead.collectAsState("")
        val predReadValue by viewModel.predRead.collectAsState(null)

        DeviceScreen(
            unselectDevice = {
                viewModel.disconnectActiveDevice()
                viewModel.setActiveDevice(null)
            },
            isDeviceConnected = uiState.isDeviceConnected,
            discoveredCharacteristics = uiState.discoveredCharacteristics,
            dataRead = dataReadValue,
            predRead = predReadValue,
            connect = viewModel::connectActiveDevice,
            discoverServices = viewModel::discoverActiveDeviceServices,
            readData = viewModel::readDataFromActiveDevice,
            readPrediction = viewModel::readPredictionFromActiveDevice
        )
    }
}