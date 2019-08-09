package com.ballofknives.bluetoothmeatball

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import java.nio.ByteBuffer

/**
 * Connect to a meatball and send accelerometer data to it to control the motion.
 */
class DriverActivity : AppCompatActivity(), SensorEventListener {
    private var mSensorManager : SensorManager?= null
    private var mAccelerometer : Sensor?= null
    private var eventCount = 0

    var xEvent : Float = 0.0f
    var yEvent : Float = 0.0f
    var zEvent : Float = 0.0f

    var service : BluetoothGameClient? = null

    /**
     * @todo Enable the bit of code for checking if the bluetooth adapter is on!
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = BluetoothGameClient() // prob need a reference to this so that we can vibrate  the context. TODO
        setContentView(R.layout.activity_driver)


        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // setup the window
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }


    /**
     * @TODO verify that this is okay:
     * I am going to send only the Nth sensor update? Will that work?
     * Need to verify how the receiver is processing the update, maybe that is the error.
     * I find some directions very difficult to move in using the current implementation.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        eventCount++
        if ((event != null) && (eventCount == 1)){
            //Log.i(Constants.TAG, "Sensor changed!")
            eventCount=0
            // When there is a sensor event, send out the data over bluetooth.
            //ground!!.updateMe(event.values[1] , event.values[0])
            synchronized(this){
                xEvent = -1 * event.values[0]
                yEvent = event.values[1]
                zEvent = event.values[2]
                //Log.i(Constants.TAG,xEvent.toString() + " " + yEvent.toString() )
                val shortX = java.lang.Float.floatToIntBits(xEvent)
                val xBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortX)
                val shortY = java.lang.Float.floatToIntBits(yEvent)
                val yBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortY)
                service?.write( xBytes.array() )
                service?.write( yBytes.array() )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME)
        service?.start()


    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
        service?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Log.i(Constants.TAG, "onDestroy()")
    }

    /**
     * Update ListView
     *
     *  @todo Don't understand the <String!> type expected for the MutableList here.
     *        Would like the warning to go away.
     *        @todo Make sure bluetooth is on.
     *  @reference: https://www.dev2qa.com/android-listview-example/
     *  @reference: http://android-er.blogspot.com/2014/12/list-paired-bluetooth-devices-and-read.html
     */

    fun updatePairedDeviceList(view: View){
        val pairedDevices: Set<BluetoothDevice>? = service?.adapter?.bondedDevices
        val pairedDeviceList : MutableList<BluetoothDevice>? = pairedDevices?.toMutableList()
        val pairedDeviceNames = pairedDeviceList?.map{ device -> device.name + "-" + device.address }?.toMutableList()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDeviceNames )
        val listView = findViewById<ListView>(R.id.textView)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _ , index, _ ->
            val iter = pairedDevices?.iterator()
            //iter?.forEach {
             //   Log.i(Constants.TAG, it.address)
            //}

            val toConnect = pairedDevices?.elementAt(index)

            if( toConnect != null) {
                //Log.i(Constants.TAG, "Trying to connect a device!")
                try {
                    //Log.i(Constants.TAG, "Connecting")
                    Toast.makeText(this@DriverActivity, "Trying to connect.", Toast.LENGTH_SHORT).show()
                    service?.connect(toConnect)
                    //TODO navigate to the connected screen.
                }
                catch(e: Exception){
                    //Log.e(Constants.TAG, "Error during connect in DriverActivity")
                }
            }

        }
    }


}
