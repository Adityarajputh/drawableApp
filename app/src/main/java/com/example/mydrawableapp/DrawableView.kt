package com.example.mydrawableapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View



class DrawableView(context: Context,attrs: AttributeSet) : View(context,attrs){

    private var mDrawPath : CustomPath? = null
    private var mBitmap : Bitmap? = null
    private var mDrawPaint : Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushSize : Float = 0.0F
    private var color = Color.BLACK
    private var canvas : Canvas? = null
    private val paths = ArrayList<CustomPath>()
    private val undoPaths = ArrayList<CustomPath>()


    init{
        setUpDrawing()
    }

    fun onUndoClicked(){
        if(paths.size > 0){
            undoPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun onRedoClicked(){

        if(undoPaths.size > 0){
            paths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
//        mBrushSize = 20.toFloat()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!,0f,0f,mCanvasPaint)

        for(path in paths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        var touchX = event?.x
        var touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()

                mDrawPath!!.moveTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_MOVE ->{
                mDrawPath!!.lineTo(touchX!!,touchY!!)
            }
            MotionEvent.ACTION_UP ->{
                paths.add(mDrawPath!!)
                mDrawPath = CustomPath(color,mBrushSize)
            }
            else -> {
                return false
            }
        }
        invalidate()

        return true
    }

    fun setBrushSize(size: Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                size,resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float): Path(){

    }



}