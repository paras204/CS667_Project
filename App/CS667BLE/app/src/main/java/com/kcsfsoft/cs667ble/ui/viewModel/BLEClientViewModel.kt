package com.kcsfsoft.cs667ble.ui.viewModel

////  AUTHOR: Nikhil Meena   /////

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kcsfsoft.cs667ble.ble.BLEDeviceConnection
import com.kcsfsoft.cs667ble.ble.BLEScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalCoroutinesApi::class)
class BLEClientViewModel(private val application: Application) : AndroidViewModel(application) {
    private val bleScanner = BLEScanner(application)
    private val activeConnection = MutableStateFlow<BLEDeviceConnection?>(null)

    private val isDeviceConnected = activeConnection.flatMapLatest { it?.isConnected ?: flowOf(false) }
    private val activeDeviceServices = activeConnection.flatMapLatest { it?.services ?: flowOf(emptyList()) }
    val dataRead = activeConnection.flatMapLatest { it?.dataRead ?: flowOf(null) }
//    val predRead = activeConnection.flatMapLatest { it?.predRead ?: flowOf(null) }
    val predRead = activeConnection.flatMapLatest { it?.predRead ?: flowOf(null) }

    private val _uiState = MutableStateFlow(BLEClientUIState())
    val uiState = combine(
        _uiState,
        isDeviceConnected,
        activeDeviceServices,
        dataRead,
        predRead
    ) { state, isDeviceConnected, services, dataRead, predRead ->

        state.copy(
            isDeviceConnected = isDeviceConnected,
            discoveredCharacteristics = services.associate { service ->
                Pair(service.uuid.toString(), service.characteristics.map { it.uuid.toString() })
            },
            dataRead = dataRead,
            predRead = predRead
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BLEClientUIState())

    init {
        viewModelScope.launch {
            bleScanner.foundDevices.collect { devices ->
                _uiState.update { it.copy(foundDevices = devices) }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    fun startScanning() {
        bleScanner.startScanning()
    }

    @RequiresPermission("android.permission.BLUETOOTH_SCAN")
    fun stopScanning() {
        bleScanner.stopScanning()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun setActiveDevice(device: BluetoothDevice?) {
        activeConnection.value = device?.let { BLEDeviceConnection(application, it) }
        _uiState.update { it.copy(activeDevice = device) }
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun connectActiveDevice() {
        activeConnection.value?.connect()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun disconnectActiveDevice() {
        activeConnection.value?.disconnect()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun discoverActiveDeviceServices() {
        activeConnection.value?.discoverServices()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun readDataFromActiveDevice() {
        activeConnection.value?.readDataCharacteristic()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun readPredictionFromActiveDevice() {
        activeConnection.value?.readPredictionCharacteristic()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    override fun onCleared() {
        super.onCleared()
        if (bleScanner.isScanning.value) {
            bleScanner.stopScanning()
        }
    }
}

data class BLEClientUIState(
    val isScanning: Boolean = false,
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val activeDevice: BluetoothDevice? = null,
    val isDeviceConnected: Boolean = false,
    val discoveredCharacteristics: Map<String, List<String>> = emptyMap(),
    val dataRead: String? = null,
    val predRead: Int? = null
)

data class DataEntry(val data: Map<String, Any>)