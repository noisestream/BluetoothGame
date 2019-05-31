package com.example.bluetoothmeatball

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Connect to a meatball and send accelerometer data to it to control the motion.
 */
class DriverActivity : AppCompatActivity(), SensorEventListener {
    private var mSensorManager : SensorManager?= null
    private var mAccelerometer : Sensor?= null

    private var REMOTE_BT_DEVICE = "80:4E:70:D9:74:BB"

    var xEvent : Float = 0.0f
    var yEvent : Float = 0.0f

    val TAG = "DriverActivity"

    private val handler = object: Handler() {
        override fun handleMessage(msg: Message?) {
            // TODO what the hell is this@DriverActivity?? It was suggested to me by the IDE to eliminate the error.
            when( msg?.what ){
                GameGlobals.MESSAGE_READ -> {
                    Toast.makeText(this@DriverActivity, "Read" + msg?.obj, Toast.LENGTH_SHORT).show()
                }
                else -> Toast.makeText(this@DriverActivity, "toast!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    var service : BluetoothGameService? = null

    /**
     * @todo Enable the bit of code for checking if the bluetooth adapter is on!
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        service = BluetoothGameService( this, handler )
        service?.start()

        //val pairedDevices: Set<BluetoothDevice>? = service?.adapter?.bondedDevices

        //val REMOTE_BT_DEVICE = pairedDevices?.filter{ device: BluetoothDevice -> device.address ==  }?.elementAt(0 )
        //Log.i(TAG, "Found remote BT device:" + REMOTE_BT_DEVICE?.name )

        //service?.connect( REMOTE_BT_DEVICE!! );

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // setup the window
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        //updatePairedDeviceList()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            //Log.i(TAG, "Sensor changed!")
            // When there is a sensor event, send out the data over bluetooth.
            //ground!!.updateMe(event.values[1] , event.values[0])
            synchronized(this){
                xEvent = event.values[1]
                yEvent = event.values[2]
                val shortX = java.lang.Float.floatToIntBits(xEvent);
                val xBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortX);
                val shortY = java.lang.Float.floatToIntBits(yEvent);
                val yBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortY);
                service?.write( xBytes.array() );
                service?.write( yBytes.array() );
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer,
            SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
    }

    /**
     * Update ListView
     *
     *  @todo Don't understand the <String!> type expected for the MutableList here.
     *        Would like the warning to go away.
     *  @reference: https://www.dev2qa.com/android-listview-example/
     *  @reference: http://android-er.blogspot.com/2014/12/list-paired-bluetooth-devices-and-read.html
     */

    fun updatePairedDeviceList(view: View){
        val pairedDevices: Set<BluetoothDevice>? = service?.adapter?.bondedDevices
        val pairedDeviceList : MutableList<BluetoothDevice>? = pairedDevices?.toMutableList()
        val pairedDeviceNames = pairedDeviceList?.map{ device -> device.name + "-" + device.address }?.toMutableList()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, pairedDeviceNames )
        val listView = findViewById<ListView>(R.id.textView)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _ , index, _ ->
            //val clickItemObj = adapterView.adapter.getItem(index)
            //Toast.makeText(this, "You clicked $clickItemObj" + " UUID is " + BluetoothGameService.GameUUID, Toast.LENGTH_SHORT).show()
            val iter = pairedDevices?.iterator()
            iter?.forEach {
                Log.i(TAG, it.address)
            }

            val toConnect = pairedDevices?.filter{ device: BluetoothDevice -> device.address == REMOTE_BT_DEVICE }?.elementAt(0 )

            if( toConnect != null) {
                Log.i(TAG, "Trying to connect a device!")
                try {
                    service?.connect(toConnect)
                }
                catch(e: Exception){
                    Log.e(TAG, "Error during connect in DriverActivity")
                }
            }

        }
    }


}
