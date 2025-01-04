package com.noisestream.bluetoothgame

import android.graphics.Canvas
//import android.util.Log
import android.view.SurfaceHolder

class DrawThread (surfaceHolder: SurfaceHolder, panel : GameSurface) : Thread() {
    private var surfaceHolder : SurfaceHolder?= null
    private var panel : GameSurface ?= null
    private var run = false

    init {
        //Log.i(Constants.TAG, "In DrawThread() constructor")
        this.surfaceHolder = surfaceHolder
        this.panel = panel
    }

    fun setRunning(run : Boolean){
        this.run = run
    }

    override fun run() {
        var c: Canvas?= null
        while (run){
            c = null
            try {
                c = surfaceHolder!!.lockCanvas(null)
                synchronized(surfaceHolder!!){
                    panel!!.draw(c)
                }
                if (c!= null){
                    surfaceHolder!!.unlockCanvasAndPost(c)
                }
            }catch(e: Exception){
                //Log.i(Constants.TAG, "Caught " + e.message)
                //Log.i(Constants.TAG, "Exiting draw thread!")
                return;
            }
        }
    }

}