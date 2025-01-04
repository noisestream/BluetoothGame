package com.noisestream.bluetoothgame

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import kotlin.math.pow
import kotlin.math.sqrt

class GameSurface(context: Context) : SurfaceView(context), SurfaceHolder.Callback{
    // ball coordinates
    private var cx : Float = 0.toFloat()
    var cy : Float = 0.toFloat()

    // graphic size of the ball
    var picHeight: Int = 0
    var picWidth : Int = 0

    private var icon: Bitmap
    private var icons: List<Bitmap>

    // window size
    private var Windowwidth : Int = 0
    private var Windowheight : Int = 0
    private var gameWidth: Int = 0
    private var gameHeight: Int = 0

    private var onBorderX = false
    private var onBorderY = false

    //var vibratorService : Vibrator?= null
    private var drawThread : DrawThread?= null


    init {
        // This holder is a confusing thing. TODO
        holder.addCallback(this)

        //create a thread
        drawThread = DrawThread(holder, this)

        // get references and sizes of the objects
        val display: Display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size: Point = Point()
        display.getSize(size)
        Windowwidth = size.x
        Windowheight = size.y
        icon = BitmapFactory.decodeResource(resources,R.drawable.ball)
        icons = listOf<Bitmap>(BitmapFactory.decodeResource(resources,R.drawable.ball),
            BitmapFactory.decodeResource(resources,R.drawable.ball),
            BitmapFactory.decodeResource(resources,R.drawable.ball),
            BitmapFactory.decodeResource(resources,R.drawable.ball))
        picHeight = icon!!.height
        picWidth = icon!!.width
        //vibratorService = (getContext().getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
        gameWidth = Windowwidth - picWidth // todo more appropriate name here than gameWidth
        gameHeight = Windowheight - picHeight // todo more appropriate name here than gameHeight
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
       destroySurface()
    }

    fun destroySurface()
    {
        /*
        holder?.removeCallback(this)
        Log.i(Constants.TAG, "Starting surfaceDestroyed...")
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

         */
        Log.i(Constants.TAG, "surface destroyed!")
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(Constants.TAG,"surfaceCreated")
        drawThread!!.setRunning(true)
        drawThread!!.start()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        //Log.i("draw", "Called draw!")
        if (canvas != null){
            //Log.i("draw", "non null canvas ${cx} ${cy}")
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(icon,cx,cy,null)
            //canvas.drawBitmap(icons!!.shuffled().take(1)[0], cx, cy, null)
        }
        else{
            Log.i("draw", "null canvas")
        }
    }

    override fun onDraw(canvas: Canvas) {

        //Log.i("onDraw", "Called onDraw!")
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(icon,cx,cy,null)
        //canvas.drawBitmap(icons!!.shuffled().take(1)[0], cx, cy, null)
    }



    /**
     * @param inx - the acceleration felt in the x direction on the driver phone
     * @param iny - the acceleration felt in the y direction on the driver phone
     * @TODO hard to maintain cx = 0. For some reason the accelerometer is
     * often reporting a positive x Gravitation even when the axis is pointed down.
     * Could be an accelerometer (hardware) bug, could be android os, could be my code.
     */
    @Synchronized fun updateMe(inx : Float , iny : Float){
        val scale=5
        val deltaX = scale*inx//computeAverage(prevX)
        val deltaY = scale*iny//computeAverage(prevY)
        cx += deltaX
        cy += deltaY

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
        //Log.i(TAG, "(gameWidth, gameHeight) : (" + "%7d".format(gameWidth) + "," + "%7d".format(gameHeight) + ") (dx,dy) : (" + "%5.2f".format(deltaX) + "," + "%5.2f".format(deltaY) + ") ( cx,cy ) : " + "(" + "%5.2f".format(cx) + "," + "%5.2f".format(cy) + ")")
        Log.i(TAG,"( cx,cy ) : " + "(" + "%5.2f".format(cx) + "," + "%5.2f".format(cy) + ")")

        invalidate()
    }
}
