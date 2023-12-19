package com.ballofknives.bluetoothmeatball

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothSharedViewModel(application: Application) : AndroidViewModel(application) {
    private val adapter = (application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _connected = MutableLiveData<Boolean>(false)
    val connected: LiveData<Boolean> = _connected

    val _bondedDevices = MutableLiveData<List<BluetoothDevice>>()
    val bondedDevices: LiveData<List<BluetoothDevice>> = _bondedDevices


    var connectThread: ConnectThread? = null

    private var connectedThread: ConnectedThread? = null

    var mState: Int = STATE_NONE

    private var localSocket: BluetoothSocket? = null
    private var localInStream: InputStream? = null
    private var localOutStream: OutputStream? = null

    companion object {
        val GameUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        const val STATE_NONE: Int = 0
        const val STATE_LISTEN: Int = 1
        const val STATE_CONNECTING: Int = 2
        const val STATE_CONNECTED: Int = 3
    }

    init {
        mState = STATE_NONE
        refreshBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices(){
        _bondedDevices.value = adapter.bondedDevices.toList()
    }


    @Synchronized fun start(){
        //Log.i(Constants.TAG, "Starting BluetoothGameService")
        if( connectThread != null ){
            connectThread?.cancel()
            connectThread = null
        }

        if( connectedThread != null ){
            connectedThread?.cancel()
            connectedThread = null
        }
    }

    @Synchronized fun connect(device: BluetoothDevice){
        if( mState == STATE_CONNECTING){
            if( connectThread != null ){
                connectThread?.cancel()
                connectThread = null
            }
        }

        if( connectedThread != null ){
            connectedThread?.cancel()
            connectedThread = null
        }

        connectThread = ConnectThread( device )
        connectThread?.start()
    }

    @Synchronized fun connected(socket: BluetoothSocket?){
        if( connectThread != null){
            connectThread?.cancel()
            connectThread = null
        }

        if( connectedThread != null){
            connectedThread?.cancel()
            connectedThread = null
        }

        _connected.postValue(true)

        connectedThread = ConnectedThread( socket )
        connectedThread?.start()

    }

    @Synchronized fun stop(){
        if ( connectThread != null ){
            connectThread?.cancel() // TODO wont be null but the ? implies it might be null. What to do?
            connectThread = null
        }
        if ( connectedThread != null ){
            connectedThread?.cancel()
            connectedThread = null
        }
        mState = STATE_NONE
    }

    fun write( out : ByteArray ){
        synchronized(this){
            if( mState != STATE_CONNECTED ) {
                return // TODO better error handling
            }
            connectedThread?.write(out)
        }
    }

    private fun connectionFailed(){
        Log.i(Constants.TAG, "Connection Failed!")
        mState = STATE_NONE
        this.start()
    }

    private fun connectionLost(){
        mState = STATE_NONE
        this.start()
    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread(private var localDevice: BluetoothDevice?): Thread(){
        //private var localSocket : BluetoothSocket? = null

        init {
            try {
                localSocket = localDevice?.createRfcommSocketToServiceRecord(GameUUID)
                if (localSocket == null) {
                    Log.e(Constants.TAG, "NULL local socket!!!")
                } else {
                    mState = STATE_CONNECTING
                }
            } catch ( e: Exception) {
                Log.e(TAG, "Error creating socket to service record")
                if (e.message != null) {
                    Log.e(TAG, e.message!!)
                }
            }
        }


        @SuppressLint("MissingPermission")
        override fun run(){
            name = "ConnectThread"
            try{
                localSocket?.connect()
            }
            catch(e: IOException){
                try{
                    localSocket?.close()
                }
                catch( e2: IOException){
                    Log.e(Constants.TAG, "Unable to close() socket. Error during connection")
                }
                connectionFailed()
                return
            }

            synchronized(this){
                connectThread = null
            }
            connected( localSocket)
        }


        fun cancel(){
            try{
                localSocket?.close()
            }
            catch( e: IOException){
                Log.e(Constants.TAG, "Unable to close() socket in cancel()")
            }
        }
    }

    inner class ConnectedThread(socket: BluetoothSocket? ): Thread(){

        //private var localSocket: BluetoothSocket? = null
        //private var localInStream: InputStream? = null
        //private var localOutStream: OutputStream? = null
        init{
            //Log.e(Constants.TAG, "Create ConnectedThread")
            localSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try{
                tmpIn = socket?.inputStream
                tmpOut = socket?.outputStream
            }
            catch( e: IOException){
                Log.e(Constants.TAG, "Error getting socket streams")
            }

            localInStream = tmpIn
            localOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run(){

            val VibrationMessageSize = 1
            val VibrationMessage : Byte = 0x01
            val buffer = ByteArray(VibrationMessageSize)
            var bytes = 0

            while(mState == STATE_CONNECTED){
                try{
                    bytes = localInStream?.read(buffer)!!
                }
                catch( e: IOException){
                    if(e.message != null) {
                        Log.e(TAG, "before run error")
                        Log.e(Constants.TAG, e.message!!)
                        Log.e(TAG, "after run error")
                    }
                    connectionLost()
                    break;
                }
            }
        }

        fun write(buffer: ByteArray){
            try{
                //Log.i(TAG, "sending bytes: ${buffer.toHex()}")
                localOutStream?.write(buffer)
            }
            catch( e: IOException){
                Log.e(Constants.TAG, "Error during write() in connected thread")
            }
        }

        fun cancel(){
            try {
                localSocket?.close()
            }
            catch( e: IOException){
                Log.e(Constants.TAG, "close() of connection socket failed!")
            }
        }
    }
}