package com.example.editor_app_intern.customeview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.editor_app_intern.R
import com.example.editor_app_intern.ui.edit.EditActivity
import dev.eren.removebg.RemoveBg
import kotlin.math.abs


class PaintView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val remover = context?.let { RemoveBg(it) }
    private var mX = 0f
    private var mY = 0f
    private var previousBackgroundBitmap: Bitmap? = null
    private var borderColor: Int = Color.BLACK
    private var mPath: Path? = null
    var isTextBoxVisible = true
    private var isDraggingTextBox = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val mPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_BRUSH_COLOR
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 0xff
        setXfermode(null)
        alpha = 0xff
        brushColor = DEFAULT_BRUSH_COLOR
        backgroundColor = DEFAULT_BG_COLOR
        brushSize = DEFAULT_BRUSH_SIZE
        touchTolerance = DEFAULT_TOUCH_TOLERANCE
        textSize = 100f
        try {
            typeface = Typeface.createFromAsset(context?.assets, "fonts/rubik_regular.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val paths = ArrayList<DrawingPath>()
    private val undoPaths = ArrayList<DrawingPath>()
    private var brushColor: Int
    private var backgroundBitmap: Bitmap? = null
    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private var backgroundColor: Int
    private val activityContext = context as EditActivity
    private var brushSize: Int = 10
    var touchTolerance: Float
    var canvasBitmap: Bitmap? = null
    var userText: String? = null // Lưu văn bản người dùng nhập
    private var textX = 200f // Vị trí X mặc định
    private var textY = 200f // Vị trí Y mặc định

    private val iconEditText: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.edit)
    private val iconDeleteText: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.remove_text)
    private val iconScaleText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom)
    private val iconRotateText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.reload)

    private val iconWidth = 50
    private val iconHeight = 50

    private val deleteIconRect = Rect()
    private val editIconRect = Rect()
    private val rotateIconRect = Rect()
    private val scaleIconRect = Rect()

    private val scaledIconEditText: Bitmap =
        Bitmap.createScaledBitmap(iconEditText, iconWidth, iconHeight, true)
    private val scaledIconDeleteText: Bitmap =
        Bitmap.createScaledBitmap(iconDeleteText, iconWidth, iconHeight, true)
    private val scaledIconScaleText: Bitmap =
        Bitmap.createScaledBitmap(iconScaleText, iconWidth, iconHeight, true)
    private val scaledIconRotateText: Bitmap =
        Bitmap.createScaledBitmap(iconRotateText, iconWidth, iconHeight, true)
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
    var drawText: String? = null

//    init {
//        mPaint.isAntiAlias = true
//        mPaint.isDither = true
//        mPaint.color = DEFAULT_BRUSH_COLOR
//        mPaint.style = Paint.Style.STROKE
//        mPaint.strokeJoin = Paint.Join.ROUND
//        mPaint.strokeCap = Paint.Cap.ROUND
//        mPaint.setXfermode(null)
//        mPaint.alpha = 0xff
//        brushColor = DEFAULT_BRUSH_COLOR
//        backgroundColor = DEFAULT_BG_COLOR
//        brushSize = DEFAULT_BRUSH_SIZE
//        touchTolerance = DEFAULT_TOUCH_TOLERANCE
//    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(canvasBitmap!!)
    }

    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }

//    fun removeBackground(inputBitmap: Bitmap, progressBar: ProgressBar) {
//        previousBackgroundBitmap = backgroundBitmap?.copy(Bitmap.Config.ARGB_8888, true)
//        CoroutineScope(Dispatchers.IO).launch {
//            remover?.clearBackground(inputBitmap)?.collect { outputBitmap ->
//                outputBitmap?.let {
//                    Log.d("PaintView", "Output Bitmap size: ${it.width} x ${it.height}")
//                    withContext(Dispatchers.Main) {
//                        setBackgroundBitmap(it)
//                        progressBar.visibility = View.GONE
//                    }
//                }
//            }
//        }
//    }

//    fun undoRemoveBackground() {
//        previousBackgroundBitmap?.let {
//            setBackgroundBitmap(it)
//            previousBackgroundBitmap = null
//        }
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

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        mCanvas?.drawRect(Rect(0, 0, width, height), backgroundPaint)
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
            mPaint.style = Paint.Style.STROKE
            mCanvas!!.drawPath(drawingPath.path, mPaint)
        }


        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, mBitmapPaint)

        if (userText != null) {
            canvas.drawText(userText!!, textX, textY, mPaint) // Luôn vẽ văn bản
        }
        if (isTextBoxVisible) {
            // Vẽ hình chữ nhật và các biểu tượng chỉ khi isTextBoxVisible là true
            userText?.let {
                val textWidth = mPaint.measureText(it)
                val textHeight = mPaint.textSize
                val rectLeft = textX - 20
                val rectTop = textY - textHeight - 20
                val rectRight = textX + textWidth + 20
                val rectBottom = textY + 20

                val borderPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }

                canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, borderPaint)
                canvas.drawBitmap(
                    scaledIconDeleteText,
                    rectLeft - scaledIconDeleteText.width / 2,
                    rectTop - scaledIconDeleteText.height / 2,
                    null
                )
                canvas.drawBitmap(
                    scaledIconEditText,
                    rectRight - scaledIconEditText.width / 2,
                    rectTop - scaledIconEditText.height / 2,
                    null
                )
                canvas.drawBitmap(
                    scaledIconRotateText,
                    rectLeft - scaledIconRotateText.width / 2,
                    rectBottom - scaledIconRotateText.height / 2,
                    null
                )
                canvas.drawBitmap(
                    scaledIconScaleText,
                    rectRight - scaledIconScaleText.width / 2,
                    rectBottom - scaledIconScaleText.height / 2,
                    null
                )

                // Cập nhật các hình chữ nhật cho các biểu tượng
                deleteIconRect.set(
                    (rectLeft - scaledIconDeleteText.width / 2).toInt(),
                    (rectTop - scaledIconDeleteText.height / 2).toInt(),
                    (rectLeft - scaledIconDeleteText.width / 2 + scaledIconDeleteText.width).toInt(),
                    (rectTop - scaledIconDeleteText.height / 2 + scaledIconDeleteText.height).toInt()
                )
                editIconRect.set(
                    (rectRight - scaledIconEditText.width / 2).toInt(),
                    (rectTop - scaledIconEditText.height / 2).toInt(),
                    (rectRight - scaledIconEditText.width / 2 + scaledIconEditText.width).toInt(),
                    (rectTop - scaledIconEditText.height / 2 + scaledIconEditText.height).toInt()
                )
                rotateIconRect.set(
                    (rectLeft - scaledIconRotateText.width / 2).toInt(),
                    (rectBottom - scaledIconRotateText.height / 2).toInt(),
                    (rectLeft - scaledIconRotateText.width / 2 + scaledIconRotateText.width).toInt(),
                    (rectBottom - scaledIconRotateText.height / 2 + scaledIconRotateText.height).toInt()
                )
                scaleIconRect.set(
                    (rectRight - scaledIconScaleText.width / 2).toInt(),
                    (rectBottom - scaledIconScaleText.height / 2).toInt(),
                    (rectRight - scaledIconScaleText.width / 2 + scaledIconScaleText.width).toInt(),
                    (rectBottom - scaledIconScaleText.height / 2 + scaledIconScaleText.height).toInt()
                )
            }
        }
        canvas.restore()

    }


    fun centerText() {
        userText?.let {
            val textWidth = mPaint.measureText(it)
            val textHeight = mPaint.textSize
            textX = (width - textWidth) / 2
            textY = (height + textHeight) / 2
        }
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
        if (mPath == null) {
            return // Tránh thực hiện nếu mPath là null
        }

        val dx = abs((x - mX).toDouble()).toFloat()
        val dy = abs((y - mY).toDouble()).toFloat()

        if (dx >= touchTolerance || dy >= touchTolerance) {
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }


    private fun touchUp() {
        if (mPath != null) {
            mPath!!.lineTo(mX, mY)
        } else {
            Log.e("PaintView", "mPath is null in touchUp()")
        }
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Kiểm tra nếu chạm vào vùng hình chữ nhật
                if (isTextBoxVisible && isTextBoxTouched(x, y)) {
                    isDraggingTextBox = true
                    initialTouchX = x
                    initialTouchY = y
                } else {
                    // Kiểm tra các biểu tượng
                    if (deleteIconRect.contains(x.toInt(), y.toInt())) {
                        onDeleteIconClick()
                        return true
                    } else if (editIconRect.contains(x.toInt(), y.toInt())) {
                        onEditIconClick()
                        return true
                    }

                    // Nếu không chạm vào hình chữ nhật, ẩn nó đi
                    if (isTextBoxVisible && !isTextTouched(x, y)) {
                        isTextBoxVisible = false
                        invalidate() // Chỉ làm mới lại để ẩn hình chữ nhật và các icon
                        return true
                    }

                    // Nếu không chạm vào văn bản, bắt đầu vẽ
                    startTouch(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingTextBox) {
                    val dx = x - initialTouchX
                    val dy = y - initialTouchY
                    textX += dx
                    textY += dy
                    initialTouchX = x
                    initialTouchY = y
                } else {
                    touchMove(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (isDraggingTextBox) {
                    isDraggingTextBox = false
                } else {
                    touchUp()
                }
                invalidate()
            }
        }

        // Kiểm tra nếu chạm vào văn bản để hiển thị hình chữ nhật
        if (!isTextBoxVisible && userText != null && isTextTouched(x, y)) {
            isTextBoxVisible = true
            invalidate()
        }

        return true
    }

    private fun isTextTouched(x: Float, y: Float): Boolean {
        val textWidth = mPaint.measureText(userText)
        val textHeight = mPaint.textSize
        val rectLeft = textX - 20
        val rectTop = textY - textHeight - 20
        val rectRight = textX + textWidth + 20
        val rectBottom = textY + 20
        return x >= rectLeft && x <= rectRight && y >= rectTop && y <= rectBottom
    }

    private fun isTextBoxTouched(x: Float, y: Float): Boolean {
        if (userText == null) {
            return false
        }
        val textWidth = mPaint.measureText(userText)
        val textHeight = mPaint.textSize
        val rectLeft = textX - 20
        val rectTop = textY - textHeight - 20
        val rectRight = textX + textWidth + 20
        val rectBottom = textY + 20
        return x >= rectLeft && x <= rectRight && y >= rectTop && y <= rectBottom
    }


    private fun onEditIconClick() {
        userText?.let { activityContext.openInputTextEdit(it, textX, textY) }
    }

    private fun onDeleteIconClick() {
        userText = null
        activityContext.clearText()
        invalidate()
    }

    private fun addDrawingChangeListener(listener: DrawingChangeListener?) {
        drawingChangeListener = listener
    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        backgroundBitmap = scaledBitmap
        invalidate()
    }

    fun setBrushSize(size: Int) {
        brushSize = size;
    }

    fun setBrushColor(color: Int) {
        brushColor = color
    }

    //    fun drawBorder(canvas: Canvas) {
//        val borderPaint = Paint().apply {
//            color = borderColor
//            style = Paint.Style.STROKE
//            strokeWidth = 30f
//            isAntiAlias = true
//        }
//        val borderRect = Rect(0, 0, canvas.width, canvas.height)
//        canvas.drawRect(borderRect, borderPaint)
//    }
    fun drawText(text: String,x: Float, y: Float) {
        userText = text
        textX = x
        textY = y
//        centerText()
        invalidate()
    }

    fun setBorderColor(color: Int) {
        borderColor = color
        invalidate()
    }

    fun setFont(path: String) {
        try {
            val typeface = Typeface.createFromAsset(context.assets, path)
            mPaint.typeface = typeface
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        const val DEFAULT_BRUSH_SIZE: Int = 20
        const val DEFAULT_BRUSH_COLOR: Int = Color.BLACK
        const val DEFAULT_BG_COLOR: Int = Color.WHITE
        private const val DEFAULT_TOUCH_TOLERANCE = 4f
    }
}