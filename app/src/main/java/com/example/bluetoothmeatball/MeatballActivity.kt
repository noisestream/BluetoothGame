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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(Constants.TAG, "onCreate() Meatball Activity");

        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        gameSurface = GameSurface(this)

        //setContentView(R.layout.activity_meatball)
        setContentView(gameSurface)
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
         Log.i(Constants.TAG, "onPause")
         super.onPause()
         gameSurface?.destroySurface()
         gameSurface = null
         finish()
     }

     override fun onDestroy() {
         Log.i(Constants.TAG, "onDestroy")
         super.onDestroy()
         gameSurface?.destroySurface()
         gameSurface = null
         finish()
     }


     override fun onBackPressed() {
         super.onBackPressed()
         Log.i(Constants.TAG, "Back button pressed!")
         try {
             gameSurface?.destroySurface()
             gameSurface = null
             finish()
         }
         catch(e: Exception){
             Log.i(Constants.TAG, "failed to join thread!")
         }
     }
 }
