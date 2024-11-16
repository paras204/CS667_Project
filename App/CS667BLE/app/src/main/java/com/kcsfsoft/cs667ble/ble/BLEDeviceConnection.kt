package com.kcsfsoft.cs667ble.ble

////  AUTHOR: Nikhil Meena   /////

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

val DATA_CHAR_UUID = UUID.fromString("23456781-1234-5678-1234-56789abcdef1")
val PREDICTION_UUID = UUID.fromString("32456781-1234-5678-1234-56789abcdef1")
val SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

class BLEDeviceConnection @RequiresPermission("android.permission.BLUETOOTH_CONNECT") constructor(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice
) {
    val isConnected = MutableStateFlow(false)
    val dataRead = MutableStateFlow<String?>(null)
    val predRead = MutableStateFlow<Int?>(null) // Changed to Int?
    val services = MutableStateFlow<List<BluetoothGattService>>(emptyList())

    private val notificationQueue: Queue<BluetoothGattDescriptor> = LinkedList()
    private var gatt: BluetoothGatt? = null

    private val callback = object : BluetoothGattCallback() {
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val connected = newState == BluetoothGatt.STATE_CONNECTED
            if (connected) {
                gatt.requestMtu(517) // Request a larger MTU size
            }
            isConnected.value = connected
        }

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices()
            } else {
                gatt.discoverServices()
                // Currently Not logging anything, let it go on...
                // Handle MTU change failure if necessary
            }
        }

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            services.value = gatt.services

            // Enqueue notifications for both characteristics
            gatt.getService(SERVICE_UUID)?.getCharacteristic(PREDICTION_UUID)
                ?.let { characteristic ->
                    enqueueNotification(characteristic)
                }
            gatt.getService(SERVICE_UUID)?.getCharacteristic(DATA_CHAR_UUID)
                ?.let { characteristic ->
                    enqueueNotification(characteristic)
                }

            // Start processing the notification queue
            processNextNotification()
        }

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processNextNotification()
            } else {
                Log.e("BLEDeviceConnection", "Failed to write descriptor: ${descriptor.uuid}")
                processNextNotification()  // Continue even on failure, for now...
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            when (characteristic.uuid) {
                DATA_CHAR_UUID -> dataRead.value = characteristic.getStringValue(0)
                PREDICTION_UUID -> predRead.value =
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            when (characteristic.uuid) {
                DATA_CHAR_UUID -> dataRead.value = characteristic.getStringValue(0)
                PREDICTION_UUID -> predRead.value =
                    characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
            }
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun connect() {
        gatt = bluetoothDevice.connectGatt(context, false, callback)
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun readDataCharacteristic() {
        gatt?.getService(SERVICE_UUID)?.getCharacteristic(DATA_CHAR_UUID)?.let { characteristic ->
            gatt?.readCharacteristic(characteristic)
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun readPredictionCharacteristic() {
        gatt?.getService(SERVICE_UUID)?.getCharacteristic(PREDICTION_UUID)?.let { characteristic ->
            gatt?.readCharacteristic(characteristic)
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    fun discoverServices() {
        gatt?.discoverServices()
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    private fun enqueueNotification(characteristic: BluetoothGattCharacteristic) {
        gatt?.setCharacteristicNotification(characteristic, true)
        characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            ?.let { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                notificationQueue.add(descriptor)
            }
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    private fun processNextNotification() {
        if (notificationQueue.isNotEmpty()) {
            gatt?.writeDescriptor(notificationQueue.poll())
        }
    }
}