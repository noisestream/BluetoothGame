 package com.ballofknives.bluetoothmeatball

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build

import android.os.Bundle
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

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.i(Constants.TAG, "onCreate() Meatball Activity");

        super.onCreate(savedInstanceState)

        gameSurface = GameSurface(this)

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

     /*override fun onResume(){
         Log.i(Constants.TAG, "onResume() Meatball Activity!")
         super.onResume()
         gameSurface?.destroySurface()
         gameSurface = null
         gameSurface = GameSurface(this)
         setContentView(gameSurface)
     }*/

     override fun onPause() {
         //Log.i(Constants.TAG, "onPause")
         super.onPause()
         gameSurface?.destroySurface()
         gameSurface = null
         finish()
     }

     override fun onDestroy() {
         //Log.i(Constants.TAG, "onDestroy")
         super.onDestroy()
         gameSurface?.destroySurface()
         gameSurface = null
         finish()
     }


     override fun onBackPressed() {
         super.onBackPressed()
         //Log.i(Constants.TAG, "Back button pressed!")
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
