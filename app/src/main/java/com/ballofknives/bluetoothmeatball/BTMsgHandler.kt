package com.ballofknives.bluetoothmeatball

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.ref.WeakReference
import java.nio.ByteBuffer


//class BTMsgHandler(looper: Looper, private var surface: WeakReference<GameSurface>?): Handler(looper) {
class BTMsgHandler(looper: Looper, private var surface: GameSurface?): Handler(looper) {
    override fun handleMessage(msg: Message) {
        when( msg.what ){
            GameGlobals.MESSAGE_WRITE-> {
                Log.i(Constants.TAG,"Write")
            }
            GameGlobals.MESSAGE_READ -> {
                val data = msg.obj as ByteArray
                val xCoord = ByteBuffer.wrap(data).getFloat(0);
                val yCoord = ByteBuffer.wrap(data).getFloat(4);
                Log.i(TAG, "Read (x,y) from socket: ($xCoord,$yCoord)")
                //surface?.get()?.updateMe(xCoord, yCoord)
                surface?.updateMe(xCoord, yCoord)
            }
            else -> {
                val pass: Unit = Unit
            }
        }
    }
}