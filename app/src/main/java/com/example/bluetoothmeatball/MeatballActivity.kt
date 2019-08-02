 package com.example.bluetoothmeatball

import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

 /**
  * Reference used for this:
  * https://o7planning.org/en/10521/android-2d-game-tutorial-for-beginners
  */
 class MeatballActivity : AppCompatActivity() {

    var gameSurface : GameSurface ?= null
    val TAG = "MeatballActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate() Meatball Activity");

        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        gameSurface = GameSurface(this)

        setContentView(R.layout.activity_meatball)
        setContentView(gameSurface)
     }

     override fun onPause() {
         Log.i("GameSurface", "onPause")
         super.onPause()
         gameSurface = null
     }

     override fun onDestroy() {
         Log.i("GameSurface", "onDestroy")
         super.onDestroy()
         gameSurface = null
     }


     override fun onBackPressed() {
         super.onBackPressed()
         Log.i(TAG, "Back button pressed!")
         try {
             gameSurface?.destroySurface()
         }
         catch(e: Exception){
             Log.i(TAG, "failed to join thread!")
         }
     }
 }
