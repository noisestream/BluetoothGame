package com.example.bluetoothmeatball

import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onResume(){
        super.onResume()
        Log.i("MainActivity", "onResume")
    }

    fun chooseDriver( view: View) {
        val driverIntent = Intent( this, DriverActivity::class.java)
        startActivity( driverIntent )
    }

    fun chooseMeatball(view: View) {
        Log.i("MainActivity", "chooseMeatball!")
        val meatballIntent = Intent( this, MeatballActivity::class.java)
        meatballIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        startActivity( meatballIntent )
    }
}
