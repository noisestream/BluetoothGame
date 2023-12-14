
/**
 * BluetoothGameClient corresponds to the Meatball Driver. The Meatball is the server that is connected to by this
 * client.
 */
package com.ballofknives.bluetoothmeatball

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.*
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

class BluetoothGameClient(var adapter: BluetoothAdapter? = null) {
    private val weakRef = WeakReference(this)
    private val handler = BTMsgHandler(weakRef)

    companion object {
        val GameUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        const val STATE_NONE: Int = 0
        const val STATE_LISTEN: Int = 1
        const val STATE_CONNECTING: Int = 2
        const val STATE_CONNECTED: Int = 3
    }

    var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    var mState: Int = STATE_NONE // if this is not private I get a 'platform declaration clash error'

    init {
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

        connectThread = ConnectThread( device )
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
        synchronized(this){
            if( mState != STATE_CONNECTED ) {
                return // TODO better error handling
            }
            connectedThread?.write(out)
        }
    }

    private fun connectionFailed(){
        //Log.i(Constants.TAG, "Connection Failed!")
        mState = STATE_NONE
        this.start()
    }

    private fun connectionLost(){
        mState = STATE_NONE
        this.start()
    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread(private var localDevice: BluetoothDevice?): Thread(){
        private var localSocket : BluetoothSocket? = null

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
                tmpIn = socket?.inputStream
                tmpOut = socket?.outputStream
            }
            catch( e: IOException ){
                //Log.e(Constants.TAG, "Error getting socket streams")
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
                    handler?.obtainMessage(GameGlobals.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
                }
                catch( e: IOException){
                    if(e.message != null)
                        Log.e(Constants.TAG, e.message!!)
                    connectionLost()
                    break;
                }
            }
        }

        fun write(buffer: ByteArray){
           ////Log.e(Constants.TAG, "Entering BluetoothGameService.ConnectedThread.write()")
            try{
                Log.i(TAG, "sending bytes: ${buffer.toHex()}")
                localOutStream?.write(buffer)
                //handler?.obtainMessage(GameGlobals.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
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
                //Log.e(Constants.TAG, "close() of connection socket failed!")
            }
        }
    }

    class BTMsgHandler(private val outerclass: WeakReference<BluetoothGameClient>): Handler() {
        override fun handleMessage(msg: Message) {
            when( msg.what ){
                GameGlobals.MESSAGE_WRITE-> {
                    Log.i(Constants.TAG,"Write")
                }
                else -> {
                    val pass: Unit = Unit
                }
            }
        }
    }
}