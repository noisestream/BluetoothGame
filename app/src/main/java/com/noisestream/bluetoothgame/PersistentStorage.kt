/**
 * Class taken from adil-hussain-84 on github.
 * https://github.com/adil-hussain-84/android-runtime-permission-experiments/blob/master/app2/src/main/java/com/tazkiyatech/app/PersistentStorage.kt
 */
package com.noisestream.bluetoothgame

import android.content.Context
import android.content.SharedPreferences

class PersistentStorage(private val context: Context) {
    var userHasAcknowledgedBluetoothPermissionRationale: Boolean
        get() = getSharedPreferences()
            .getBoolean(USER_HAS_ACKNOWLEDGED_BLUETOOTH_PERMISSION_RATIONALE, false)
        set(value) = getSharedPreferences()
            .edit()
            .putBoolean(USER_HAS_ACKNOWLEDGED_BLUETOOTH_PERMISSION_RATIONALE, value)
            .apply()

    private fun getSharedPreferences() : SharedPreferences{
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
    }

    private companion object{
        private const val USER_HAS_ACKNOWLEDGED_BLUETOOTH_PERMISSION_RATIONALE =
            "USER_HAS_ACKNOWLEDGED_BLUETOOTH_PERMISSION_RATIONALE"

        private const val SHARED_PREFS =
            "SHARED_PREFS"
    }
}