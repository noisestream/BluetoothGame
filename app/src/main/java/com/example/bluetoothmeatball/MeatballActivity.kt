 package com.example.bluetoothmeatball

import android.app.Service
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import java.nio.ByteBuffer

 class MeatballActivity : AppCompatActivity() {

    var ground : GroundView ?= null
    var service : BluetoothGameServer? = null

    val TAG = "MeatballActivity"

    private val handler = object: Handler() {
        override fun handleMessage(msg: Message?) {
            // TODO what the hell is this@DriverActivity?? It was suggested to me by the IDE to eliminate the error.
            when( msg?.what ){
                GameGlobals.MESSAGE_WRITE -> {
                    Toast.makeText(this@MeatballActivity, "Read" + msg?.obj, Toast.LENGTH_SHORT).show()
                    val data = msg.obj as ByteArray
                    val xCoord = ByteBuffer.wrap(data).getFloat(0);
                    val yCoord = ByteBuffer.wrap(data).getFloat(4);
                    ground!!.updateMe(xCoord, yCoord)
                }
                GameGlobals.MESSAGE_READ -> {
                    //Toast.makeText(this@MeatballActivity, "Read" + msg?.obj, Toast.LENGTH_SHORT).show()
                    val data = msg.obj as ByteArray
                    val xCoord = ByteBuffer.wrap(data).getFloat(0);
                    val yCoord = ByteBuffer.wrap(data).getFloat(4);
                    Log.i(TAG, "x="+xCoord.toString() + " y=" + yCoord.toString())
                    ground!!.updateMe(xCoord, yCoord)
                }
                GameGlobals.MESSAGE_DEVICE_NAME, GameGlobals.MESSAGE_TOAST->
                    Toast.makeText(this@MeatballActivity, msg!!.what.toString(), Toast.LENGTH_SHORT).show()
                GameGlobals.MESSAGE_STATE_CHANGE->
                    Toast.makeText(this@MeatballActivity, msg!!.what.toString(), Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this@MeatballActivity, "unexpected msg.what", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate() Meatball Activity");
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meatball)
        service = BluetoothGameServer(this, handler)
        service!!.start()
        Log.i(TAG, "MeatballActivity started BluetoothGameService")

        ground = GroundView(this)
        setContentView(ground)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    class DrawThread (surfaceHolder: SurfaceHolder, panel : GroundView) : Thread() {
        private var surfaceHolder : SurfaceHolder?= null
        private var panel : GroundView ?= null
        private var run = false

        init {
            Log.i("DrawThead", "In DrawThread() constructor")
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
                }finally {
                    if (c!= null){
                        surfaceHolder!!.unlockCanvasAndPost(c)
                    }
                }
            }
        }

    }
}

class GroundView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback{
    private val TAG = "GroundView"
    var buzzNow : Boolean = false
    var myContext = context
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

    var vibratorService : Vibrator?= null
    var thread : MeatballActivity.DrawThread?= null



    init {
        holder.addCallback(this) // TODO how do I know this is the member holder and not a disposable copy tjhat disappears after the init{} ends??
        //create a thread
        thread = MeatballActivity.DrawThread(holder, this)
        // get references and sizes of the objects
        val display: Display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size: Point = Point()
        display.getSize(size)
        Windowwidth = size.x
        Windowheight = size.y
        icon = BitmapFactory.decodeResource(resources,R.drawable.ball)
        picHeight = icon!!.height
        picWidth = icon!!.width
        vibratorService = (getContext().getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
        gameWidth = Windowwidth - picWidth // todo more appropriate name here than gameWidth
        gameHeight = Windowheight - picHeight // todo more appropriate name here than gameHeight
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        thread!!.setRunning(true)
        thread!!.start()
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
                vibratorService!!.vibrate(100) // deprecated
                onBorderX = false
                buzzNow = true
            }
        }
        else if(cx < (0)){
            cx = 0F
            if(onBorderX){
                vibratorService!!.vibrate(100)
                onBorderX = false
                buzzNow = true
            }
        }
        else{
            onBorderX = true
            buzzNow = false
        }

        if (cy > (gameHeight)){
            cy = (gameHeight).toFloat()
            if (onBorderY){
                vibratorService!!.vibrate(100)
                onBorderY = false
                buzzNow = true
            }
        }

        else if(cy < (0)){
            cy = 0F
            if (onBorderY){
                vibratorService!!.vibrate(100)
                onBorderY= false
                buzzNow = true
            }
        }
        else{
            onBorderY = true
            buzzNow = false
        }

        invalidate()
    }
}
