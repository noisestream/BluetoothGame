package com.ballofknives.bluetoothmeatball

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

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
    }


    override fun onResume(){
        super.onResume()
        //Log.i(Constants.TAG, "onResume")
    }

    fun chooseDriver( view: View) {
        val driverIntent = Intent( this, DriverActivity::class.java)
        startActivity( driverIntent )
    }

    fun chooseMeatball(view: View) {
        //Log.i(Constants.TAG, "chooseMeatball!")
        val meatballIntent = Intent( this, MeatballActivity::class.java)
        meatballIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity( meatballIntent )
    }
}
