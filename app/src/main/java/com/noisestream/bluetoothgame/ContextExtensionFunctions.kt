package com.noisestream.bluetoothgame

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

fun Context.bluetoothAdapter(): BluetoothAdapter? =
    (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }