package com.example.bluetoothmeatball

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun chooseDriver( view: View) {
        val driverIntent = Intent( this, DriverActivity::class.java)
        startActivity( driverIntent )
    }

    fun chooseMeatball(view: View) {
        val meatballIntent = Intent( this, MeatballActivity::class.java)
        startActivity( meatballIntent )
    }
}
