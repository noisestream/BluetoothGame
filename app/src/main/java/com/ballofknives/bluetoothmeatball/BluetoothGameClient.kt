package com.ballofknives.bluetoothmeatball

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
//import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

fun Context.bluetoothAdapter(): BluetoothAdapter? =
    (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter


const val ANDROID_BELOW_S_REQUEST_PERMISSION = 0
const val ANDROID_S_ABOVE_REQUEST_PERMISSION = 1

/**
 * BluetoothGameClient corresponds to the Meatball Driver. The Meatball is the server that is connected to by this
 * client.
 *
 * @todo - would like the states to be an enum rather than ints.
 */
class BluetoothGameClient(private val myContext: Context) {
    private val weakRef = WeakReference(this)
    private val handler = BTMsgHandler(weakRef)

    companion object {
        val GameUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        const val STATE_NONE: Int = 0
        const val STATE_LISTEN: Int = 1
        const val STATE_CONNECTING: Int = 2
        const val STATE_CONNECTED: Int = 3
    }
    var adapter: BluetoothAdapter? = null
    var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    var mState: Int = STATE_NONE // if this is not private I get a 'platform declaration clash error'

    init {
        adapter = myContext.bluetoothAdapter()
        mState = STATE_NONE
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

    @Synchronized fun connect(device: BluetoothDevice ){
        //Log.i(Constants.TAG, "Attempting to connect BluetoothGameService to " + device.name )
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

        connectThread = ConnectThread( device, myContext)
        connectThread?.start()
    }

    @Synchronized fun connected( socket: BluetoothSocket?, device: BluetoothDevice?){
        if( connectThread != null){
            connectThread?.cancel()
            connectThread = null
        }

        if( connectedThread != null){
            connectedThread?.cancel()
            connectedThread = null
        }

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
        var r : ConnectedThread? // TODO = null here says is redundant
        synchronized(this){
            if( mState != STATE_CONNECTED ) {
                return // TODO better error handling
            }
            r = connectedThread
        }

        r?.write(out) // TODO what if r is null? That would throw an uncaught exception!
    }

    private fun connectionFailed(){
        //Log.i(Constants.TAG, "Connection Failed!")
        mState = STATE_NONE
        this.start()
    }

    private fun connectionLost(){
        mState = STATE_NONE

        // TODO not sure if this is okay
        this.start() // BluetoothGameService.this.start()
    }

    inner class ConnectThread(private var localDevice: BluetoothDevice?, myContext: Context): Thread(){
        private var localSocket : BluetoothSocket? = null
        init {
            var tmp : BluetoothSocket? = null
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                if (ActivityCompat.checkSelfPermission(
                        myContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(myContext as Activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), ANDROID_S_ABOVE_REQUEST_PERMISSION)
                }
                else {
                    tmp = localDevice?.createInsecureRfcommSocketToServiceRecord(GameUUID)
                }
            }
            else{

            }
            localSocket = tmp
            if ( localSocket == null ) {
                Log.e(Constants.TAG, "NULL local socket!!!")
            }
            else {
                mState = STATE_CONNECTING
            }
        }


        override fun run(){
            name = "ConnectThread"
            try{
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    if (ActivityCompat.checkSelfPermission(
                            myContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(myContext as Activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), ANDROID_S_ABOVE_REQUEST_PERMISSION)
                    }
                    else {
                        localSocket?.connect()
                    }
                }
                else{
                    localSocket?.connect()
                }
            }
            catch(e: IOException){
                try{
                    localSocket?.close()
                }
                catch( e2: IOException){
                    //Log.e(Constants.TAG, "Unable to close() socket. Error during connection")
                }
                connectionFailed()
                return
            }

            synchronized(this){
                connectThread = null
            }
            connected( localSocket, localDevice)
        }


        fun cancel(){
            try{
                localSocket?.close()
            }
            catch( e: IOException){
                //Log.e(Constants.TAG, "Unable to close() socket in cancel()")
            }
        }
    }

    inner class ConnectedThread(socket: BluetoothSocket? ): Thread(){
        private var localSocket: BluetoothSocket? = null
        private var localInStream: InputStream? = null
        private var localOutStream: OutputStream? = null

        init{
            //Log.e(Constants.TAG, "Create ConnectedThread")
            localSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try{
                tmpIn = socket?.getInputStream()
                tmpOut = socket?.getOutputStream() // TODO dont use getters! Use property access instead?
            }
            catch( e: IOException ){
                //Log.e(Constants.TAG, "Error getting socket streams")
            }

            localInStream = tmpIn
            localOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run(){
            //Log.e(Constants.TAG, "beginning connectedthread")
            val VibrationMessageSize = 1
            val VibrationMessage : Byte = 0x01
            val buffer = ByteArray(VibrationMessageSize) // TODO here I differ from the sample code. The buffer is 1024 there, but I want to avoid buffering issues.
            var bytes = 0

            while(mState == STATE_CONNECTED){
                try{
                    bytes = localInStream?.read(buffer)!!
                    handler?.obtainMessage(GameGlobals.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
                }
                catch( e: IOException){
                    //Log.e(Constants.TAG, "disconnected!")
                    connectionLost()
                    break;
                }
            }
        }

        fun write(buffer: ByteArray){
           ////Log.e(Constants.TAG, "Entering BluetoothGameService.ConnectedThread.write()")
            try{
                localOutStream?.write(buffer)
                //handler?.obtainMessage(GameGlobals.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
            }
            catch( e: IOException){
                //Log.e(Constants.TAG, "Error during write() in connected thread")
            }
        }

        fun cancel(){
            try {
                localSocket?.close()
            }
            catch( e: IOException){
                //Log.e(Constants.TAG, "close() of connection socket failed!")
            }
        }
    }

    class BTMsgHandler(private val outerclass: WeakReference<BluetoothGameClient>): Handler() {
        override fun handleMessage(msg: Message) {
            when( msg.what ){
                GameGlobals.MESSAGE_WRITE-> {
                    ////Log.i(Constants.TAG,"Write")
                }
                else -> {
                    val pass: Unit = Unit
                }
            }
        }
    }
}