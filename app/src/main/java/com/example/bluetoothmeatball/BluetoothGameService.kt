package com.example.bluetoothmeatball

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Bundle
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*


/**
 *
 * @todo - just wanted static gameUUID - not sure how to do that so I put it in a companion object.
 * @todo - would like the states to be an enum rather than ints.
 * @todo maybe move the companion object stuff out to GameGlobals.kt
 */
class BluetoothGameService(context: Context, h: Handler) {
    val TAG = "BluetoothGameService"
    val NAME = "BluetoothGame"

    var hasData = true

    var xCoord = 0.0f
    var yCoord = 0.0f

    companion object {

        //val GameUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val GameUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        const val STATE_NONE: Int = 0
        const val STATE_LISTEN: Int = 1
        const val STATE_CONNECTING: Int = 2
        const val STATE_CONNECTED: Int = 3
    }

    var adapter: BluetoothAdapter? = null
    var handler: Handler? = null
    var connectThread: ConnectThread? = null
    var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private var mState: Int = STATE_NONE // if this is not private I get a 'platform declaration clash error'
    var newState: Int = STATE_NONE

    init {
        adapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        newState = mState
        handler = h
    }

    @Synchronized
    fun updateUserInterfaceTitle() {
        mState = getState()
        newState = mState
        handler?.obtainMessage(GameGlobals.MESSAGE_STATE_CHANGE, newState, -1)?.sendToTarget()
    }


    /**
     * This method is stupid. No need for this I dont think, just make mState a public member.
     */
    @Synchronized fun getState() :Int {
        return mState
    }

    @Synchronized fun start(){
        Log.i(TAG, "Starting BluetoothGameService")
        if( connectThread != null ){
            connectThread?.cancel()
            connectThread = null
        }

        if( connectedThread != null ){
            connectedThread?.cancel() // TODO Warning! Throughout here I use the ? to make android studio shoosh. But what if the thread is null?
            connectedThread = null
        }

        if( acceptThread == null ){
            Log.e(TAG, "Accept thread was null")
            acceptThread = AcceptThread()
            if( acceptThread == null )
                Log.e(TAG, "Accept Thread still null")
            else
                Log.e(TAG, "Accept thread no longer null")
            acceptThread?.start()
        }
        updateUserInterfaceTitle()
    }

    @Synchronized fun connect(device: BluetoothDevice ){
        Log.i(TAG, "Attempting to connect BluetoothGameService to " + device.name )
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
        updateUserInterfaceTitle()
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

        if( acceptThread != null ){
            acceptThread?.cancel()
            acceptThread = null
        }

        connectedThread = ConnectedThread( socket )
        connectedThread?.start()

        val msg = handler?.obtainMessage( GameGlobals.MESSAGE_DEVICE_NAME )
        val bundle = Bundle()
        bundle.putString(GameGlobals.DEVICE_NAME, device?.getName())
        msg?.setData(bundle)
        handler?.sendMessage(msg)
        updateUserInterfaceTitle()
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
        if ( acceptThread != null ) {
            acceptThread?.cancel()
            acceptThread = null
        }
        mState = STATE_NONE
        updateUserInterfaceTitle()
    }

    fun write( out : ByteArray ){
        var r : ConnectedThread? // TODO = null here says is redundant
        synchronized(this){
            if( mState != STATE_CONNECTED ) {
                //Log.i(TAG, "Cant write data - socket disconnected.")
                return
            }
            r = connectedThread
        }

        r?.write(out) // TODO what if r is null? That would throw an uncaught exception!
    }

    private fun connectionFailed(){
        Log.i(TAG, "Connection Failed!")
        val msg = handler?.obtainMessage(GameGlobals.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(GameGlobals.TOAST, "Unable to connect device")
        msg?.setData(bundle) //TODO could probable say msg.data =
        handler?.sendMessage(msg)

        mState = STATE_NONE
        updateUserInterfaceTitle()
        //BluetoothGameService.this.start() // TODO probably just say "this"
        this.start()
    }

    private fun connectionLost(){
        val msg = handler?.obtainMessage(GameGlobals.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(GameGlobals.TOAST, "Device Connection Lost")
        msg?.setData(bundle)
        handler?.sendMessage(msg)
        mState = STATE_NONE
        updateUserInterfaceTitle()

        // TODO not sure if this is okay
        this.start() // BluetoothGameService.this.start()
    }

    //  NOTE needs to be marked "inner class" because it needs access to the parent class' members
    // TODO is this okay? The base class must be initialized here! Get rid of parens to see
    inner class AcceptThread : Thread() {
        private var serverSocket : BluetoothServerSocket? = null

        // TODO what is the difference between a secure and an insecure socket?
        init{
            Log.e(TAG, "Constructing acceptThread")
            var tmp: BluetoothServerSocket? = null

            try{
                //tmp = adapter?.listenUsingRfcommWithServiceRecord(NAME, GameUUID)
                tmp = adapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, GameUUID)
            }
            catch( e: IOException ){
                Log.e(TAG, "Socket listen() failed", e)
                // TODO serverSocket might be null here!
            }
            Log.i(TAG, "nonnull socket for  GameUUID!")
            serverSocket = tmp
            mState = STATE_LISTEN
        }

        // TODO needs override keyword?
        override fun run(){
            Log.e(TAG, "Starting run() in accept thread")
            setName("Accept Thread")
            var localSocket: BluetoothSocket? = null
            while ( mState != STATE_CONNECTED ){
                try{
                    localSocket = serverSocket?.accept() // TODO check if socket is null?
                }
                catch( e: IOException){
                    Log.e(TAG, "Socket accept() failed!", e)
                    break
                }

                if ( localSocket != null){
                    // TODO hmmmm does this refer to the outer class here?
                    //synchronized(BluetoothGameService.this){
                    synchronized(this){
                        when(mState){
                            STATE_LISTEN, STATE_CONNECTING -> connected( localSocket, localSocket.getRemoteDevice())
                            STATE_NONE, STATE_CONNECTED -> try{
                                    localSocket.close()
                                }
                                catch( e: IOException ){
                                    Log.e(TAG, "Could not close unwanted socket")
                                }
                            else -> Log.e(TAG, "Error, you shouldnt be here!")
                        }

                    }
                }
            }
            Log.e(TAG,"End of acceptThread")
        }

        fun cancel(){
            Log.e(TAG, "cancel socket")
            try{
                serverSocket?.close()
            }
            catch(e: IOException){
                Log.e(TAG, "server socket close() failed")
            }
        }
    }

    inner class ConnectThread(device: BluetoothDevice?): Thread(){
        var localSocket : BluetoothSocket? = null
        var localDevice: BluetoothDevice? = null

        //TODO there is a way in kotlin to assign member vars in the constructor param list I think
        init {
            localDevice = device
            var tmp : BluetoothSocket? = null

            try{
                //tmp = localDevice?.createRfcommSocketToServiceRecord(GameUUID)
                tmp = localDevice?.createInsecureRfcommSocketToServiceRecord(GameUUID)
            }
            catch(e: IOException){
                Log.e(TAG, "Error in ConnectThread.create()")
            }
            localSocket = tmp
            if ( localSocket == null )
                Log.e( TAG, "NULL local socket!!!")
            mState = STATE_CONNECTING
        }

        override fun run(){
            Log.i(TAG, "BEGIN ConnectThread()")
            setName("ConnectThread")

            // TODO adapter could be null here!
            adapter?.cancelDiscovery()

            try{
                localSocket?.connect()
            }
            catch(e: IOException){
                try{
                    localSocket?.close()
                }
                catch( e2: IOException){
                    Log.e(TAG, "Unable to close() socket. Error during connection")
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
                Log.e(TAG, "Unable to close() socket in cancel()")
            }
        }
    }

    inner class ConnectedThread(socket: BluetoothSocket? ): Thread(){
        var localSocket: BluetoothSocket? = null
        var localInStream: InputStream? = null
        var localOutStream: OutputStream? = null

        init{
            Log.e(TAG, "Create ConnectedThread")
            localSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try{
                tmpIn = socket?.getInputStream()
                tmpOut = socket?.getOutputStream() // TODO dont use getters! Use property access instead?
            }
            catch( e: IOException ){
                Log.e(TAG, "Error getting socket streams")
            }

            localInStream = tmpIn
            localOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run(){
            Log.e(TAG, "beginning connectedthread")
            // TODO make sure a float is 4 bytes in Kotlin.
            var buffer = ByteArray(java.lang.Float.BYTES * 2) // TODO here I differ from the sample code. The buffer is 1024 there, but I want to avoid buffering issues.
            var bytes = 0

            while(mState == STATE_CONNECTED){
                try{
                    //bytes = localInStream?.read(buffer)
                    bytes = localInStream?.read(buffer)!! // TODO what is going on here with the !!
                    // TODO about the aforementioned !! ->  https://discuss.kotlinlang.org/t/automatic-coercion-from-nullable-to-non-null/543
                    handler?.obtainMessage(GameGlobals.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
                }
                catch( e: IOException){
                    Log.e(TAG, "disconnected!")
                    connectionLost()
                    break;
                }
            }
        }

        fun write(buffer: ByteArray){
            Log.e(TAG, "Entering BluetoothGameService.ConnectedThread.write()")
            try{
                localOutStream?.write(buffer)
                handler?.obtainMessage(GameGlobals.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
            }
            catch( e: IOException){
                Log.e(TAG, "Error during write() in conencted thread")
            }
        }

        fun cancel(){
            try {
                localSocket?.close()
            }
            catch( e: IOException){
                Log.e(TAG, "close() of connection socket failed!")
            }
        }
    }
}