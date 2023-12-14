package com.ballofknives.bluetoothmeatball

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.core.app.ActivityCompat

//import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs


/**
 *
 * @todo - just wanted static gameUUID - not sure how to do that so I put it in a companion object.
 * @todo - would like the states to be an enum rather than ints.
 * @todo maybe move the companion object stuff out to GameGlobals.kt
 */
class BluetoothGameServer(private var adapter: BluetoothAdapter?, private var gameSurface: GameSurface?) {
    private val NAME = "BluetoothGame"

    private val weakRef = WeakReference<BluetoothGameServer>(this)

    private val handler = BTMsgHandler(weakRef)

    companion object {
        val GameUUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
        const val STATE_NONE: Int = 0
        const val STATE_LISTEN: Int = 1
        const val STATE_CONNECTING: Int = 2
        const val STATE_CONNECTED: Int = 3
    }

    var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private var mState: Int = STATE_NONE // if this is not private I get a 'platform declaration clash error'
    var newState: Int = STATE_NONE

    init {
        mState = STATE_NONE
        newState = mState
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
        //Log.i(Constants.TAG, "Starting BluetoothGameService")

        if( connectedThread != null ){
            connectedThread?.cancel() // TODO Warning! Throughout here I use the ? to make android studio shoosh. But what if the thread is null?
            connectedThread = null
        }

        if( acceptThread == null ){
            //Log.e(Constants.TAG, "Accept thread was null")
            acceptThread = AcceptThread()
            //if( acceptThread == null )
                //Log.e(Constants.TAG, "Accept Thread still null")
            //else
                //Log.e(Constants.TAG, "Accept thread no longer null")
            acceptThread?.start()
        }
        updateUserInterfaceTitle()
    }


    @SuppressLint("MissingPermission")
    @Synchronized fun connected(socket: BluetoothSocket, device: BluetoothDevice){
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

        val msg = handler.obtainMessage( GameGlobals.MESSAGE_DEVICE_NAME )
        val bundle = Bundle()

        bundle.putString(GameGlobals.DEVICE_NAME, device.name)
        msg?.data = bundle
        handler.sendMessage(msg)
        updateUserInterfaceTitle()
    }

    @Synchronized fun stop(){
        //Log.i(Constants.TAG, "Stopping bluetooth server.")
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
                //Log.i(Constants.TAG, "Cant write data - socket disconnected.")
                return
            }
            r = connectedThread
        }

        r?.write(out) // TODO what if r is null? That would throw an uncaught exception!
    }

    private fun connectionLost(){
        val msg = handler.obtainMessage(GameGlobals.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(GameGlobals.TOAST, "Device Connection Lost")
        msg.data = bundle
        handler.sendMessage(msg)
        mState = STATE_NONE
        updateUserInterfaceTitle()

        // TODO not sure if this is okay
        this.start()
    }

    //  NOTE needs to be marked "inner class" because it needs access to the parent class' members
    // TODO is this okay? The base class must be initialized here! Get rid of parens to see
    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {
        private var serverSocket : BluetoothServerSocket? = null

        // TODO what is the difference between a secure and an insecure socket?
        init{
            //Log.e(Constants.TAG, "Constructing acceptThread")
            var tmp: BluetoothServerSocket? = null

            try{
                tmp = adapter!!.listenUsingRfcommWithServiceRecord(NAME, GameUUID)
            }
            catch( e: IOException ){
                //Log.e(Constants.TAG, "Socket listen() failed", e)
                // TODO serverSocket might be null here!
            }
            //Log.i(Constants.TAG, "nonnull socket for  GameUUID!")
            serverSocket = tmp
            mState = STATE_LISTEN
        }

        override fun run(){
            //Log.e(Constants.TAG, "Starting run() in accept thread")
            name = "Accept Thread"
            var localSocket: BluetoothSocket? = null
            while ( mState != STATE_CONNECTED ){
                try{
                    localSocket = serverSocket?.accept() // TODO check if socket is null?


                }
                catch( e: IOException){
                    //Log.e(Constants.TAG, "Socket accept() failed!", e)
                    break
                }

                if ( localSocket != null){
                    synchronized(this){
                        when(mState){
                            STATE_LISTEN, STATE_CONNECTING -> connected( localSocket, localSocket.remoteDevice)
                            else ->try {
                            //STATE_NONE, STATE_CONNECTED -> try{
                                    localSocket.close()
                            }
                                catch( e: IOException ){
                                    //Log.e(Constants.TAG, "Could not close unwanted socket")
                            }
                            //else -> //Log.e(Constants.TAG, "Error, you shouldnt be here!")
                        }

                    }
                }
            }
            //Log.e(Constants.TAG,"End of acceptThread")
        }

        fun cancel(){
            //Log.e(Constants.TAG, "cancel socket")
            try{
                serverSocket?.close()
            }
            catch(e: IOException){
                //Log.e(Constants.TAG, "server socket close() failed")
            }
        }
    }

    inner class ConnectedThread(socket: BluetoothSocket? ): Thread(){
        var localSocket: BluetoothSocket? = null
        var localInStream: InputStream? = null
        var localOutStream: OutputStream? = null

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
            // TODO make sure a float is 4 bytes in Kotlin.
            var buffer = ByteArray(java.lang.Float.BYTES * 2)
            var bytes = 0

            while(mState == STATE_CONNECTED){
                try{
                    bytes = localInStream?.read(buffer)!!
                    Log.i(TAG, "read ${bytes} bytes from socket")
                    val xCoord = ByteBuffer.wrap(buffer).getFloat(0);
                    val yCoord = ByteBuffer.wrap(buffer).getFloat(4);
                    Log.i(TAG, "Read (x,y) from socket: ($xCoord,$yCoord)")

                        gameSurface!!.updateMe(xCoord, yCoord) // TODO !!. or ?.

                    //handler.obtainMessage(GameGlobals.MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
                }
                catch( e: IOException){
                    //Log.e(Constants.TAG, "disconnected!")
                    connectionLost()
                    break;
                }
            }
        }

        fun write(buffer: ByteArray){
            //Log.e(Constants.TAG, "Entering BluetoothGameService.ConnectedThread.write()")
            try{
                localOutStream?.write(buffer)
                handler.obtainMessage(GameGlobals.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
            }
            catch( e: IOException){
                //Log.e(Constants.TAG, "Error during write() in conencted thread")
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

    class BTMsgHandler(private val btGameServer: WeakReference<BluetoothGameServer>): Handler() {
        override fun handleMessage(msg: Message) {
            when( msg.what ){
                GameGlobals.MESSAGE_READ -> {
                    /*
                    val data = msg.obj as ByteArray
                    val xCoord = ByteBuffer.wrap(data).getFloat(0);
                    val yCoord = ByteBuffer.wrap(data).getFloat(4);
                    Log.i(TAG, "Read (x,y) from socket: ($xCoord,$yCoord)")
                    if((abs(yCoord) > 2) && (abs(xCoord) > 2)) {
                        //Log.i(Constants.TAG, xCoord.toString() + " " + yCoord.toString())
                        btGameServer.get()?.gameSurface!!.updateMe(xCoord, yCoord) // TODO !!. or ?.
                    }
                    else if(abs(yCoord) > 2)  {
                        //Log.i(Constants.TAG, xCoord.toString() + " " + yCoord.toString())
                        btGameServer.get()?.gameSurface!!.updateMe(0.0f, yCoord) // TODO !!. or ?.
                    }
                    else if(abs(xCoord) > 2) {
                        //Log.i(Constants.TAG, xCoord.toString() + " " + yCoord.toString())
                        btGameServer.get()?.gameSurface!!.updateMe(xCoord, 0.0f) // TODO !!. or ?.
                    }


                     */
                }
                else ->{
                    val pass: Unit = Unit
                }
            }
        }
    }
}