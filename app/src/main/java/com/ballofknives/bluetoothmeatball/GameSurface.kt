package com.ballofknives.bluetoothmeatball

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
    var cx : Float = 0.toFloat()
    var cy : Float = 0.toFloat()

    var prevX: ArrayDeque<Float> = ArrayDeque()
    var prevY: ArrayDeque<Float> = ArrayDeque()

    val MAX_CACHED_X_Y = 30

    // graphic size of the ball
    var picHeight: Int = 0
    var picWidth : Int = 0

    private var icon: Bitmap
    private var icons: List<Bitmap>

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

    private fun addXYVals(x: Float, y: Float){
        prevX.addLast(x)
        if(prevX.size > MAX_CACHED_X_Y){
            prevX.removeFirst()
        }
        prevY.addLast(y)
        if(prevY.size > MAX_CACHED_X_Y){
            prevY.removeFirst()
        }
    }

    private fun computeAverage(deque: ArrayDeque<Float>) : Float{
        var total = 0.0f
        deque.forEach{
            total += it
        }
        return total / deque.size
    }


    init {
        // This holder is a confusing thing. TODO
        holder.addCallback(this)

        //create a thread
        drawThread = DrawThread(holder, this)
        btServer = BluetoothGameServer(context.bluetoothAdapter(),this)

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
        btServer?.stop()
        btServer = null
        Log.i(Constants.TAG, "surface destroyed!")
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(Constants.TAG,"surfaceCreated")
        drawThread!!.setRunning(true)
        drawThread!!.start()

        btServer?.start()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (canvas != null){
            //Log.i("draw", "Called draw!")
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(icon,cx,cy,null)
            //canvas.drawBitmap(icons!!.shuffled().take(1)[0], cx, cy, null)
        }
    }

    override fun onDraw(canvas: Canvas) {

        //Log.i("onDraw", "Called onDraw!")
        canvas.drawColor(Color.WHITE) // TODO why does setting the color to 0xFFAAAAAA just produce a black background? I tried some other hex colors too with the same result.
        canvas.drawBitmap(icon,cx,cy,null)
        //canvas.drawBitmap(icons!!.shuffled().take(1)[0], cx, cy, null)
    }

    fun diff(x:Float, y: Float, x2:Float, y2:Float) : Float {
        val two : Int = 2
        val delX : Float = x - x2
        val delY : Float = y - y2
        return sqrt( delX.pow(two) + delY.pow( two ) )
    }

    /**
     * @param inx - the acceleration felt in the x direction on the driver phone
     * @param iny - the acceleration felt in the y direction on the driver phone
     * @TODO hard to maintain cx = 0. For some reason the accelerometer is
     * often reporting a positive x Gravitation even when the axis is pointed down.
     * Could be an accelerometer (hardware) bug, could be android os, could be my code.
     */
    @Synchronized fun updateMe(inx : Float , iny : Float){
        val oldCx = cx
        val oldCy = cy
        val thresh = 1
        addXYVals(inx, iny)
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
        Log.i(TAG, "(gameWidth, gameHeight) : (" + "%7d".format(gameWidth) + "," + "%7d".format(gameHeight) + ") (dx,dy) : (" + "%5.2f".format(deltaX) + "," + "%5.2f".format(deltaY) + ") ( cx,cy ) : " + "(" + "%5.2f".format(cx) + "," + "%5.2f".format(cy) + ")")

        invalidate()
    }
}
