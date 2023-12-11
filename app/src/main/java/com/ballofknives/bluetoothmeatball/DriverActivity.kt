package com.ballofknives.bluetoothmeatball

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer

/**
 * Connect to a meatball and send accelerometer data to it to control the motion.
 */
class DriverActivity : AppCompatActivity(), SensorEventListener {
    private var mSensorManager : SensorManager?= null
    private var mAccelerometer : Sensor?= null
    private var eventCount = 0

    private var xEvent : Float = 0.0f
    private var yEvent : Float = 0.0f
    private var zEvent : Float = 0.0f

    private var service : BluetoothGameClient? = null

    /**
     * @todo Enable the bit of code for checking if the bluetooth adapter is on!
     */
    @SuppressLint("SourceLockedOrientationActivity")
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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.i("permissions", Build.VERSION.SDK_INT.toString())
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            Log.i("permissions", Build.VERSION.SDK_INT.toString())
            //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //requestBluetooth.launch(enableBtIntent)
        }

    }

/*
    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }
    */


    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.d("test006", "${it.key} = ${it.value}")
        }
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

    @RequiresApi(Build.VERSION_CODES.S)
    fun updatePairedDeviceList(view: View){
        Log.i("permissions", ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ).toString())
        val pairedDevices: MutableSet<BluetoothDevice>? = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //var service = service
            //var adapter = service!!.adapter
            //adapter!!.bondedDevices
            HashSet<BluetoothDevice>()

        }
        else{
            service!!.adapter!!.bondedDevices

        }

        val pairedDeviceList : MutableList<BluetoothDevice>? = pairedDevices?.toMutableList()
        val pairedDeviceNames : MutableList<String>? = pairedDeviceList?.map{ device : BluetoothDevice -> device.name + "-" + device.address }?.toMutableList()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDeviceNames!! )
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
