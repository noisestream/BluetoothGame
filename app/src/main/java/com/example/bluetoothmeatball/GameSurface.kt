package com.example.bluetoothmeatball

import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.os.Vibrator
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager

class GameSurface(context: Context?) : SurfaceView(context), SurfaceHolder.Callback{
    private val TAG = "GameSurface"

    // ball coordinates
    var cx : Float = 0.toFloat()
    var cy : Float = 0.toFloat()

    // graphic size of the ball
    var picHeight: Int = 0
    var picWidth : Int = 0

    var icon: Bitmap?= null

    // window size
    var Windowwidth : Int = 0
    var Windowheight : Int = 0
    var gameWidth: Int = 0
    var gameHeight: Int = 0

    var onBorderX = false
    var onBorderY = false

    //var vibratorService : Vibrator?= null
    var drawThread : DrawThread?= null
    var btServer: BluetoothGameServer? = null

    init {
        // This holder is a confusing thing. TODO
        holder.addCallback(this)

        //create a thread
        drawThread = DrawThread(holder, this)
        btServer = BluetoothGameServer(this)

        // get references and sizes of the objects
        val display: Display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size: Point = Point()
        display.getSize(size)
        Windowwidth = size.x
        Windowheight = size.y
        icon = BitmapFactory.decodeResource(resources,R.drawable.ball)
        picHeight = icon!!.height
        picWidth = icon!!.width
        //vibratorService = (getContext().getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
        gameWidth = Windowwidth - picWidth // todo more appropriate name here than gameWidth
        gameHeight = Windowheight - picHeight // todo more appropriate name here than gameHeight
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
       destroySurface()
    }

    fun destroySurface()
    {
        holder?.removeCallback(this)
        Log.i(TAG, "Starting surrfaceDestroyed...")
        var retry = true
        while ( retry ){
            try{
                drawThread?.setRunning(false)
                drawThread?.join() // TODO can I join since I have a return in the run() method?
                retry = false;
            }
            catch(e: Exception){
                e.printStackTrace()
            }
        }
        btServer?.stop()
        Log.i(TAG, "surface destroyed!")
    }


    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.i(TAG,"surfaceCreated")
        drawThread!!.setRunning(true)
        drawThread!!.start()

        btServer?.start()
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (canvas != null){
            canvas.drawColor(0xFFAAAAA)
            canvas.drawBitmap(icon,cx,cy,null)
        }
    }

    override public fun onDraw(canvas: Canvas?) {

        if (canvas != null){
            canvas.drawColor(0xFFAAAAA)
            canvas.drawBitmap(icon,cx,cy,null)
        }
    }

    /**
     * @param inx - the acceleration felt in the x direction on the driver phone
     * @param iny - the acceleration felt in the y direction on the driver phone
     * @TODO hard to maintain cx = 0. For some reason the accelerometer is
     * often reporting a positive x Gravitation even when the axis is pointed down.
     * Could be an accelerometer (hardware) bug, could be android os, could be my code.
     */
    fun updateMe(inx : Float , iny : Float){
        cx += inx
        cy += iny
        if(cx > gameWidth ){
            cx = gameWidth.toFloat()
            if (onBorderX){
                //vibratorService!!.vibrate(100) // deprecated
                onBorderX = false
            }
        }
        else if(cx < (0)){
            cx = 0F
            if(onBorderX){
                //vibratorService!!.vibrate(100)
                onBorderX = false
            }
        }
        else{
            onBorderX = true
        }

        if (cy > (gameHeight)){
            cy = (gameHeight).toFloat()
            if (onBorderY){
                //vibratorService!!.vibrate(100)
                onBorderY = false
            }
        }

        else if(cy < (0)){
            cy = 0F
            if (onBorderY){
                //vibratorService!!.vibrate(100)
                onBorderY= false
            }
        }
        else{
            onBorderY = true
        }

        invalidate()
    }


}
