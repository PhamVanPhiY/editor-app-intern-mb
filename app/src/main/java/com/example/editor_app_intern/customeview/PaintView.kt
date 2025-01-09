package com.example.editor_app_intern.customeview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs


class PaintView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private var mX = 0f
    private var mY = 0f
    private var borderColor: Int = Color.BLACK
    private var mPath: Path? = null
    private val mPaint = Paint()
    private val paths = ArrayList<DrawingPath>()
    private val undoPaths = ArrayList<DrawingPath>()
    private var brushColor: Int
    private var backgroundBitmap: Bitmap? = null
    private val backgroundPaint = Paint()
    private var backgroundColor: Int
    private var brushSize: Int = 10
    var touchTolerance: Float
    var canvasBitmap: Bitmap? = null
    private var userText: String? = null // Lưu văn bản người dùng nhập
    private var textX = 100f // Vị trí X mặc định
    private var textY = 100f // Vị trí Y mặc định
    private var isMovingText = false // Kiểm tra có đang di chuyển không
        private set
    private var mCanvas: Canvas? = null
    var isDrawingEnabled = false
    private val mBitmapPaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private var drawingChangeListener: DrawingChangeListener? = null
    private var tempBrushColor = 0
    private var drawText: String? = null

    init {
        mPaint.isAntiAlias = true
        mPaint.isDither = true
        mPaint.color = DEFAULT_BRUSH_COLOR
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.setXfermode(null)
        mPaint.alpha = 0xff
        brushColor = DEFAULT_BRUSH_COLOR
        backgroundColor = DEFAULT_BG_COLOR
        brushSize = DEFAULT_BRUSH_SIZE
        touchTolerance = DEFAULT_TOUCH_TOLERANCE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(canvasBitmap!!)
    }
    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }
//
//    fun showTextInputDialog(context: Context) {
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle("Nhập văn bản")
//
//        val input = EditText(context).apply {
//            hint = "Nhập văn bản..."
//        }
//        builder.setView(input)
//
//        builder.setPositiveButton("OK") { _, _ ->
//            userText = input.text.toString()
//            drawText = userText
//            invalidate()
//        }
//
//        builder.setNegativeButton("Hủy") { dialog, _ ->
//            dialog.dismiss()
//        }
//
//        builder.show()
//    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    fun clearCanvas() {
        paths.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        userText?.let {
            canvas.drawText(it, textX, textY, paint)
        }
        if (backgroundBitmap != null) {
            backgroundBitmap?.let {

                val srcRect = Rect(0, 0, it.width, it.height)
                val destRect = Rect(0, 0, width, height)
                mCanvas!!.drawBitmap(it, srcRect, destRect, backgroundPaint)
            }
        } else {
            mCanvas!!.drawColor(backgroundColor)
        }

        for (drawingPath in paths) {
            mPaint.color = drawingPath.color
            mPaint.strokeWidth = drawingPath.strokeWidth.toFloat()
            mCanvas!!.drawPath(drawingPath.path, mPaint)
        }

        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, mBitmapPaint)

        drawBorder(canvas)
        canvas.drawTextCanvas(drawText ?: "", 100f, 100f)
        canvas.restore()
    }

    private val paint = Paint().apply {
        try {
            color = Color.RED
            textSize = 100f
            val plain = Typeface.createFromAsset(context?.assets, "fonts/rubik_regular.ttf")
            typeface = plain
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTextFromEditText(editText: String) {
        userText = editText
        invalidate()
    }

    private fun Canvas.drawTextCanvas(s: String, fl: Float, fl1: Float) {
        drawText(s, 100f, 100f, paint)
    }

    fun startTouch(x: Float, y: Float) {
        mPath = Path()
        val drawingPath = DrawingPath(
            brushColor, brushSize,
            mPath!!
        )
        paths.add(drawingPath)
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
        invalidate()
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs((x - mX).toDouble()).toFloat()
        val dy = abs((y - mY).toDouble()).toFloat()

        if (dx >= touchTolerance || dy >= touchTolerance) {
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }


    private fun touchUp() {
        mPath!!.lineTo(mX, mY)
    }

    private fun drawToCanvas(x: Float, y: Float) {
        mPath!!.lineTo(x, y)
        invalidate()
    }

    fun undoDrawing() {
        if (paths.size > 0) {
            undoPaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun redoDrawing() {
        if (undoPaths.size > 0) {
            paths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }

    fun enableEraser() {
        tempBrushColor = brushColor
        brushColor = backgroundColor
    }

    fun disableEraser() {
        if (tempBrushColor != 0) {
            brushColor = tempBrushColor
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouch(x, y)
                if (drawingChangeListener != null) {
                    drawingChangeListener!!.onTouchStart(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                if (drawingChangeListener != null) {
                    drawingChangeListener!!.onDrawingChange(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touchUp()
                if (drawingChangeListener != null) {
                    drawingChangeListener!!.onDrawingChange(x, y)
                }
                invalidate()
            }
        }
        return true
    }

    private fun addDrawingChangeListener(listener: DrawingChangeListener?) {
        drawingChangeListener = listener
    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }

    fun setBrushSize(size: Int) {
        brushSize = size;
    }

    fun setBrushColor(color: Int) {
        brushColor = color
    }

    fun drawBorder(canvas: Canvas) {
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 30f
            isAntiAlias = true
        }
        val borderRect = Rect(0, 0, canvas.width, canvas.height)
        canvas.drawRect(borderRect, borderPaint)
    }

    fun setBorderColor(color: Int) {
        borderColor = color
        invalidate()
    }

//    fun drawText(text: String, x: Float, y: Float) {
//        drawText = text
//        invalidate()
//    }

    companion object {
        const val DEFAULT_BRUSH_SIZE: Int = 20
        const val DEFAULT_BRUSH_COLOR: Int = Color.BLACK
        const val DEFAULT_BG_COLOR: Int = Color.WHITE
        private const val DEFAULT_TOUCH_TOLERANCE = 4f
    }
}