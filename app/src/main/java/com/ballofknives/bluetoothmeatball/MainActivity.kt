package com.ballofknives.bluetoothmeatball

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

const val TAG = "BluetoothMeatball"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        @SuppressLint("SourceLockedOrientationActivity")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        requestBluetoothPermission()
    }
    private val persistentStorage = PersistentStorage(this)

    private val bluetoothPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ map ->
            val userAcknowledgement = map.values.all{ it -> it } // if all permissions are granted, then we can clear the acknowledgement flag.
            if(userAcknowledgement) {
                persistentStorage.userHasAcknowledgedBluetoothPermissionRationale = false
            }
        }

    private fun isBluetoothPermissionGranted() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT ) == PackageManager.PERMISSION_GRANTED
        } else{
            ((ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED ) &&
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ))
        }
    }


    private fun shouldShowBluetoothPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)
        } else{
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || shouldShowRequestPermissionRationale(
                Manifest.permission.BLUETOOTH)
        }

    }

    private fun userHasPreviouslyAcknowledgedBluetoothPermissionRationale(): Boolean {
        return persistentStorage.userHasAcknowledgedBluetoothPermissionRationale
    }

    private fun requestBluetoothPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT
            )
            bluetoothPermissionRequest.launch(permissions)
        }
        else{
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            bluetoothPermissionRequest.launch(permissions)
        }
    }

    private fun showRationaleDialog(){
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.bluetooth_rationale_1))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                persistentStorage.userHasAcknowledgedBluetoothPermissionRationale = true
                requestBluetoothPermission()
            }
            .show()
    }

    private fun showPreviouslyAcknowledgedRationaleDialog(){
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.bluetooth_rationale_2))
            .setNegativeButton(getString(R.string.no_thanks), null)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> showApplicationDetailsSettingsScreen() }
            .show()
    }

    private fun showApplicationDetailsSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .setData(Uri.fromParts("package", packageName, null))

        startActivity(intent)
    }

    override fun onResume(){
        super.onResume()
        //Log.i(Constants.TAG, "onResume")
    }

    fun chooseDriver( view: View) {

        if(isBluetoothPermissionGranted()){
            val driverIntent = Intent( this, DriverActivity::class.java)
            startActivity( driverIntent )
        }
        else{
            if(shouldShowBluetoothPermissionRationale()){
                showRationaleDialog()
            }
            else if(userHasPreviouslyAcknowledgedBluetoothPermissionRationale()){
                showPreviouslyAcknowledgedRationaleDialog()
            }
            else{
                requestBluetoothPermission()
            }
        }

    }

    fun chooseMeatball(view: View) {
        if(isBluetoothPermissionGranted()){
            val meatballIntent = Intent( this, MeatballActivity::class.java)
            meatballIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity( meatballIntent )
        }
        else{
            if(shouldShowBluetoothPermissionRationale()){
                showRationaleDialog()
            }
            else if(userHasPreviouslyAcknowledgedBluetoothPermissionRationale()){
                showPreviouslyAcknowledgedRationaleDialog()
            }
            else{
                requestBluetoothPermission()
            }
        }
    }
}
