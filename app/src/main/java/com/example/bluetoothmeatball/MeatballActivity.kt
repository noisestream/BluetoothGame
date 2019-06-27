 package com.example.bluetoothmeatball

import android.app.Service
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorManager
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
    var service : BluetoothGameService? = null

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
                    Toast.makeText(this@MeatballActivity, "Read" + msg?.obj, Toast.LENGTH_SHORT).show()
                    val data = msg.obj as ByteArray
                    val xCoord = ByteBuffer.wrap(data).getFloat(0);
                    val yCoord = ByteBuffer.wrap(data).getFloat(4);
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
        service = BluetoothGameService(this, handler)
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

    // ball coordinates
    var cx : Float = 0.toFloat()
    var cy : Float = 0.toFloat()

    // last position increment

    var lastGx : Float = 0.toFloat()
    var lastGy : Float = 0.toFloat()

    // graphic size of the ball

    var picHeight: Int = 0
    var picWidth : Int = 0

    var icon: Bitmap?= null

    // window size

    var Windowwidth : Int = 0
    var Windowheight : Int = 0

    // is touching the edge ?

    var noBorderX = false
    var noBorderY = false

    var vibratorService : Vibrator?= null
    var thread : MeatballActivity.DrawThread?= null



    init {
        holder.addCallback(this)
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

    fun updateMe(inx : Float , iny : Float){
        lastGx += inx
        lastGy += iny

        cx += lastGx
        cy += lastGy

        if(cx > (Windowwidth - picWidth)){
            cx = (Windowwidth - picWidth).toFloat()
            lastGx = 0F
            if (noBorderX){
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        }
        else if(cx < (0)){
            cx = 0F
            lastGx = 0F
            if(noBorderX){
                vibratorService!!.vibrate(100)
                noBorderX = false
            }
        }
        else{ noBorderX = true }

        if (cy > (Windowheight - picHeight)){
            cy = (Windowheight - picHeight).toFloat()
            lastGy = 0F
            if (noBorderY){
                vibratorService!!.vibrate(100)
                noBorderY = false
            }
        }

        else if(cy < (0)){
            cy = 0F
            lastGy = 0F
            if (noBorderY){
                vibratorService!!.vibrate(100)
                noBorderY= false
            }
        }
        else{ noBorderY = true }

        invalidate()
    }
}
