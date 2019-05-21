package com.example.bluetoothmeatball

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class DriverActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        //TODO implement me
        //if (bluetoothAdapter?.isEnabled == false) {
        //    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        //    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        //}
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
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val pairedDeviceList : MutableList<BluetoothDevice>? = pairedDevices?.toMutableList()
        val pairedDeviceNames = pairedDeviceList?.map{ device -> device.name + "-" + device.address }?.toMutableList()
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, pairedDeviceNames )
        val listView = findViewById<ListView>(R.id.textView)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, _ , index, _ ->
            val clickItemObj = adapterView.adapter.getItem(index)
            Toast.makeText(this, "You clicked $clickItemObj" + " UUID is " + BluetoothGameService.GameUUID, Toast.LENGTH_SHORT).show()
        }
    }

}
