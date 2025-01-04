package com.noisestream.bluetoothgame

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import java.lang.ref.WeakReference
import java.nio.ByteBuffer


//class BTMsgHandler(looper: Looper, private var surface: WeakReference<GameSurface>?): Handler(looper) {
class BTMsgHandler(looper: Looper, private var surface: GameSurface?, private val viewForNavigation: View? = null): Handler(looper) {

    @SuppressLint("MissingPermission")
    override fun handleMessage(msg: Message) {
        when( msg.what ){
            GameGlobals.CONNECTED ->{
                val device: BluetoothDevice = msg.obj as BluetoothDevice
                val action = SelectAGameFragmentDirections.actionSelectAGameFragmentToDriverConnectedFragment( device.name )
                viewForNavigation?.findNavController()?.navigate(action)
            }
            GameGlobals.MESSAGE_WRITE-> {
                //Log.i(Constants.TAG,"Write")
            }
            GameGlobals.MESSAGE_READ -> {
                val data = msg.obj as ByteArray
                val xCoord = ByteBuffer.wrap(data).getFloat(0);
                val yCoord = ByteBuffer.wrap(data).getFloat(4);
                Log.i(TAG, "Read (x,y) from socket: ($xCoord,$yCoord)")
                surface?.updateMe(xCoord, yCoord)
            }
            else -> {
                val pass: Unit = Unit
            }
        }
    }
}