 package com.ballofknives.bluetoothmeatball

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build

import android.os.Bundle
import android.os.Looper
import android.view.WindowInsets
//import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

 /**
  * Reference used for this:
  * https://o7planning.org/en/10521/android-2d-game-tutorial-for-beginners
  */
 class MeatballActivity : AppCompatActivity() {
    private var gameSurface : GameSurface ?= null
    private lateinit var handler: BTMsgHandler
    private lateinit var btServer: BluetoothGameServer

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameSurface = GameSurface(this)
        handler = BTMsgHandler(Looper.myLooper()!!, gameSurface)
        btServer = BluetoothGameServer(this.bluetoothAdapter(), handler)
        btServer.start()
        setContentView(gameSurface)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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
         gameSurface?.destroySurface()
         gameSurface = null
         gameSurface = GameSurface(this)
         setContentView(gameSurface)
         btServer?.start() // TODO not sure about lifecycle of server. Do I need to recreate it here? I think it might have been destroyed.
     }

     override fun onPause() {
         //Log.i(Constants.TAG, "onPause")
         super.onPause()
         gameSurface?.destroySurface()
         gameSurface = null
         btServer?.stop()
         finish()
     }

     override fun onDestroy() {
         //Log.i(Constants.TAG, "onDestroy")
         super.onDestroy()
         gameSurface?.destroySurface()
         gameSurface = null
         btServer?.stop() // TODO not sure about the lifecycle of the bluetooth server. When is it destroyed?
         finish()
     }


     @Deprecated("Deprecated in Java")
     override fun onBackPressed() {
         super.onBackPressed()
         try {
             gameSurface?.destroySurface()
             gameSurface = null
             finish()
         }
         catch(e: Exception){
             //Log.i(Constants.TAG, "failed to join thread!")
         }
     }
 }
