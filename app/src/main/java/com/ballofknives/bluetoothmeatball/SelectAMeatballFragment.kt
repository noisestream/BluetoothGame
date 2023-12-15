package com.ballofknives.bluetoothmeatball

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ballofknives.bluetoothmeatball.databinding.FragmentSelectAMeatballBinding
import java.nio.ByteBuffer
import kotlin.math.abs

class SelectAMeatballFragment : Fragment() , SensorEventListener {
    private var _binding : FragmentSelectAMeatballBinding? = null
    private val binding get() = _binding!!
    private lateinit var listView: ListView
    private lateinit var letterId: String

    private var mSensorManager : SensorManager?= null
    private var mAccelerometer : Sensor?= null
    private var eventCount = 0

    private var xEvent : Float = 0.0f
    private var yEvent : Float = 0.0f

    private var service : BluetoothGameClient? = null

    private lateinit var persistentStorage : PersistentStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectAMeatballBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        service = BluetoothGameClient(requireContext().bluetoothAdapter()) // prob need a reference to this so that we can vibrate  the context. TODO

        mSensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        persistentStorage = PersistentStorage(requireContext())

        binding.btnGet.setOnClickListener{
                v -> onUpdatePairedDeviceListClicked(v)
        }

        requestBluetoothPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private val bluetoothPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ map ->
            val userAcknowledgement = map.values.all{ it -> it } // if all permissions are granted, then we can clear the acknowledgement flag.
            if(userAcknowledgement) {
                persistentStorage.userHasAcknowledgedBluetoothPermissionRationale = false
            }
        }

    private fun isBluetoothPermissionGranted() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT ) == PackageManager.PERMISSION_GRANTED
        } else{
            ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED ) &&
                    (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ))
        }
    }


    private fun shouldShowBluetoothPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)
        } else{
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH)
        }

    }

    private fun userHasPreviouslyAcknowledgedBluetoothPermissionRationale(): Boolean {
        return persistentStorage.userHasAcknowledgedBluetoothPermissionRationale
    }

    private fun requestBluetoothPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT
            )
            bluetoothPermissionRequest.launch(permissions)
        }
        else{
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            bluetoothPermissionRequest.launch(permissions)
        }
    }

    private fun showRationaleDialog(){
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.bluetooth_rationale_1))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                persistentStorage.userHasAcknowledgedBluetoothPermissionRationale = true
                requestBluetoothPermission()
            }
            .show()
    }

    private fun showPreviouslyAcknowledgedRationaleDialog(){
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.bluetooth_rationale_2))
            .setNegativeButton(getString(R.string.no_thanks), null)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> showApplicationDetailsSettingsScreen() }
            .show()
    }

    private fun showApplicationDetailsSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .setData(Uri.fromParts("package", requireContext().packageName, null))

        startActivity(intent)
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
        val cutoff = 2
        if ((event != null) && (eventCount == 1)){
            //Log.i(Constants.TAG, "Sensor changed!")
            eventCount=0
            synchronized(this) {
                xEvent = if (abs(event.values[0]) > cutoff){
                    -1.0f * event.values[0]
                } else {
                    0.0f
                }

                yEvent = if(abs(event.values[1]) > cutoff){
                    event.values[1]
                } else{
                    0.0f
                }
                val shortX = java.lang.Float.floatToIntBits(xEvent)
                val xBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortX)
                val shortY = java.lang.Float.floatToIntBits(yEvent)
                val yBytes = ByteBuffer.allocate(java.lang.Float.BYTES).putInt(shortY)
                val allBytes = ByteBuffer.allocate(2*java.lang.Float.BYTES).putInt(shortX).putInt(shortY)
                if(abs(xEvent) > 0.5 || abs(yEvent) > cutoff) {
//                    Log.i(
                    //                      TAG,
                    //                    "(dx,dy) : (" + "%5.2f".format(xEvent) + "," + "%5.2f".format(yEvent) + ")"
                    //              )
                    synchronized(this) {
                        Log.i(TAG, "sending bytes: %${allBytes.array().toHex()}")
                        //service?.write(xBytes.array())
                        //service?.write(yBytes.array())
                        service?.write(allBytes.array())
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun updatePairedDeviceList(){
        val pairedDevices: MutableSet<BluetoothDevice>? = service?.adapter?.bondedDevices

        val pairedDeviceList : MutableList<BluetoothDevice>? = pairedDevices?.toMutableList()
        val pairedDeviceNames : MutableList<String>? = pairedDeviceList?.map{ device : BluetoothDevice -> device.name + "-" + device.address }?.toMutableList()
        if (pairedDevices == null)
            return
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, pairedDeviceNames!! )
        val listView = binding.deviceListView
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            val iter = pairedDevices.iterator()
            //iter?.forEach {
            //   Log.i(Constants.TAG, it.address)
            //}

            val toConnect = pairedDevices?.elementAt(index)

            if( toConnect != null) {
                //Log.i(Constants.TAG, "Trying to connect a device!")
                try {
                    //Log.i(Constants.TAG, "Connecting")
                    Toast.makeText(requireContext(), "Trying to connect.", Toast.LENGTH_SHORT).show()
                    service?.connect(toConnect)
                    //TODO navigate to the connected screen.
                }
                catch(e: Exception){
                    //Log.e(Constants.TAG, "Error during connect in DriverActivity")
                }
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



    fun onUpdatePairedDeviceListClicked(view: View){
        if(isBluetoothPermissionGranted()){
            updatePairedDeviceList()
        }
        else{
            if(shouldShowBluetoothPermissionRationale()){
                showRationaleDialog()
            }
            else if(userHasPreviouslyAcknowledgedBluetoothPermissionRationale()){
                showPreviouslyAcknowledgedRationaleDialog()
            }
            else{
                requestBluetoothPermission()
            }
        }
    }
}








