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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import com.example.editor_app_intern.R
import com.example.editor_app_intern.ui.edit.EditActivity
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var isEraserEnabled: Boolean = false

    var isEditingText = false
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
    }
    private val PaintForText = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_BRUSH_COLOR_FOR_TEXT
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 0xff
        setXfermode(null)
        alpha = 0xff
        brushColorForText = DEFAULT_BRUSH_COLOR_FOR_TEXT
        backgroundColor = DEFAULT_BG_COLOR
        brushSize = DEFAULT_BRUSH_SIZE
        touchTolerance = DEFAULT_TOUCH_TOLERANCE
        textSize = 40f
        try {
            typeface = Typeface.createFromAsset(context?.assets, "fonts/rubik_regular.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val erasePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        isAntiAlias = true
    }

    private val paths = ArrayList<DrawingPath>()
    private val undoPaths = ArrayList<DrawingPath>()
    private var brushColor: Int
    private var brushColorForText: Int
    var backgroundBitmap: Bitmap? = null
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var backgroundColor: Int
    private val activityContext = context as EditActivity
    private var brushSize: Int = 10
    var touchTolerance: Float
    var canvasBitmap: Bitmap? = null
    var userText: String? = null
    private var textX = 200f
    private var textY = 200f

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
    var isDrawingEnabled: Boolean = false
    private val mBitmapPaint = Paint().apply {
        textSize = 50f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private var drawingChangeListener: DrawingChangeListener? = null
    private var tempBrushColor = 0
    var drawText: String? = null


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(canvasBitmap!!)
    }

//    fun enableDrawing(enable: Boolean) {
//        isDrawingEnabled = enable
//    }

    fun removeBackground(inputBitmap: Bitmap, progressBar: ProgressBar) {
        previousBackgroundBitmap = backgroundBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        CoroutineScope(Dispatchers.IO).launch {
            remover?.clearBackground(inputBitmap)?.collect { outputBitmap ->
                outputBitmap?.let {
                    Log.d("PaintView", "Output Bitmap size: ${it.width} x ${it.height}")
                    withContext(Dispatchers.Main) {
                        updateBackgroundBitmap(it)
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun undoRemoveBackground() {
        previousBackgroundBitmap?.let {
            updateBackgroundBitmap(it)
            previousBackgroundBitmap = null
        }
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    fun clearCanvas() {
        paths.clear()
        userText = null
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
            if (isEraserEnabled) {
                mCanvas!!.drawPath(drawingPath.path, erasePaint)
            } else {
                // Nếu không, vẽ bình thường
                mPaint.color = drawingPath.color
                mPaint.strokeWidth = drawingPath.strokeWidth.toFloat()
                mPaint.style = Paint.Style.STROKE
                mCanvas!!.drawPath(drawingPath.path, mPaint)
            }
            mPaint.color = drawingPath.color
            mPaint.strokeWidth = drawingPath.strokeWidth.toFloat()
            mPaint.style = Paint.Style.STROKE
            mCanvas!!.drawPath(drawingPath.path, mPaint)
        }



        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, mBitmapPaint)

        if (userText != null) {
            mCanvas!!.drawText(userText!!, textX, textY, PaintForText)
        }
        if (isTextBoxVisible) {
            val boundingRect = calculateTextBoundingRect()
            userText?.let {

                val borderPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }

                mCanvas!!.drawRect(boundingRect, borderPaint)
                mCanvas!!.drawBitmap(
                    scaledIconDeleteText,
                    null, // src Rect có thể là null nếu bạn muốn vẽ toàn bộ bitmap
                    Rect(
                        boundingRect.left - scaledIconDeleteText.width / 2,
                        boundingRect.top - scaledIconDeleteText.height / 2,
                        boundingRect.left - scaledIconDeleteText.width / 2 + scaledIconDeleteText.width,
                        boundingRect.top - scaledIconDeleteText.height / 2 + scaledIconDeleteText.height
                    ),
                    null
                )

                mCanvas!!.drawBitmap(
                    scaledIconEditText,
                    null,
                    Rect(
                        boundingRect.right - scaledIconEditText.width / 2,
                        boundingRect.top - scaledIconEditText.height / 2,
                        boundingRect.right - scaledIconEditText.width / 2 + scaledIconEditText.width,
                        boundingRect.top - scaledIconEditText.height / 2 + scaledIconEditText.height
                    ),
                    null
                )

                mCanvas!!.drawBitmap(
                    scaledIconRotateText,
                    null,
                    Rect(
                        boundingRect.left - scaledIconRotateText.width / 2,
                        boundingRect.bottom - scaledIconRotateText.height / 2,
                        boundingRect.left - scaledIconRotateText.width / 2 + scaledIconRotateText.width,
                        boundingRect.bottom - scaledIconRotateText.height / 2 + scaledIconRotateText.height
                    ),
                    null
                )

                mCanvas!!.drawBitmap(
                    scaledIconScaleText,
                    null,
                    Rect(
                        boundingRect.right - scaledIconScaleText.width / 2,
                        boundingRect.bottom - scaledIconScaleText.height / 2,
                        boundingRect.right - scaledIconScaleText.width / 2 + scaledIconScaleText.width,
                        boundingRect.bottom - scaledIconScaleText.height / 2 + scaledIconScaleText.height
                    ),
                    null
                )
                val padding = 20
                deleteIconRect.set(
                    (boundingRect.left - scaledIconDeleteText.width / 2 - padding).toInt(),
                    (boundingRect.top - scaledIconDeleteText.height / 2 - padding).toInt(),
                    (boundingRect.left - scaledIconDeleteText.width / 2 + scaledIconDeleteText.width + padding).toInt(),
                    (boundingRect.top - scaledIconDeleteText.height / 2 + scaledIconDeleteText.height + padding).toInt()
                )

                editIconRect.set(
                    (boundingRect.right - scaledIconEditText.width / 2 - padding).toInt(),
                    (boundingRect.top - scaledIconEditText.height / 2 - padding).toInt(),
                    (boundingRect.right - scaledIconEditText.width / 2 + scaledIconEditText.width + padding).toInt(),
                    (boundingRect.top - scaledIconEditText.height / 2 + scaledIconEditText.height + padding).toInt()
                )

                rotateIconRect.set(
                    (boundingRect.left - scaledIconRotateText.width / 2 - padding).toInt(),
                    (boundingRect.bottom - scaledIconRotateText.height / 2 - padding).toInt(),
                    (boundingRect.left - scaledIconRotateText.width / 2 + scaledIconRotateText.width + padding).toInt(),
                    (boundingRect.bottom - scaledIconRotateText.height / 2 + scaledIconRotateText.height + padding).toInt()
                )

                scaleIconRect.set(
                    (boundingRect.right - scaledIconScaleText.width / 2 - padding).toInt(),
                    (boundingRect.bottom - scaledIconScaleText.height / 2 - padding).toInt(),
                    (boundingRect.right - scaledIconScaleText.width / 2 + scaledIconScaleText.width + padding).toInt(),
                    (boundingRect.bottom - scaledIconScaleText.height / 2 + scaledIconScaleText.height + padding).toInt()
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
            return
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

//    private fun drawToCanvas(x: Float, y: Float) {
//        mPath!!.lineTo(x, y)
//        invalidate()
//    }

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

//    fun enableEraser() {
//        tempBrushColor = brushColor
//        brushColor = backgroundColor
//    }
//
//    fun disableEraser() {
//        if (tempBrushColor != 0) {
//            brushColor = tempBrushColor
//        }
//    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Kiểm tra nếu textbox đang hiển thị và có được chạm vào
                if (isTextBoxVisible && isTextBoxTouched(x, y)) {
                    activityContext.openEditSizeText()
                    isEditingText = true
                    isDraggingTextBox = true
                    initialTouchX = x
                    initialTouchY = y
                } else {
                    // Kiểm tra các biểu tượng xóa và chỉnh sửa
                    if (deleteIconRect.contains(x.toInt(), y.toInt())) {
                        onDeleteIconClick()
                        return true
                    } else if (editIconRect.contains(x.toInt(), y.toInt())) {
                        onEditIconClick()
                        return true
                    }

                    // Ẩn textbox nếu không chạm vào
                    if (isTextBoxVisible && !isTextTouched(x, y)) {
                        isTextBoxVisible = false
                        activityContext.closeEditHue()
                        activityContext.closeEditSizeText()
                        invalidate()
                        return true
                    }

                    // Nếu chế độ xóa được bật, bắt đầu xóa
                    if (isEraserEnabled) {
                        startTouch(x, y) // Bắt đầu xóa
                    } else if (isDrawingEnabled) {
                        startTouch(x, y) // Bắt đầu vẽ
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingTextBox) {
                    // Di chuyển textbox
                    val dx = x - initialTouchX
                    val dy = y - initialTouchY
                    textX += dx
                    textY += dy
                    initialTouchX = x
                    initialTouchY = y
                } else {
                    // Xóa hoặc vẽ dựa trên trạng thái
                    if (isEraserEnabled) {
                        touchMove(x, y) // Xóa
                    } else if (isDrawingEnabled) {
                        touchMove(x, y) // Vẽ
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (isDraggingTextBox) {
                    isDraggingTextBox = false
                } else {
                    if (isEraserEnabled) {
                        touchUp() // Kết thúc xóa
                    } else if (isDrawingEnabled) {
                        touchUp() // Kết thúc vẽ
                    }
                }
                invalidate()
            }
        }

        // Kiểm tra xem textbox có không hiển thị và có chạm vào văn bản hay không
        if (!isTextBoxVisible && userText != null && isTextTouched(x, y)) {
            isTextBoxVisible = true
            activityContext.openEditSizeText()
            isEditingText = true
            invalidate()
        }

        return true
    }

    private fun isTextTouched(x: Float, y: Float): Boolean {
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
        activityContext.closeEditSizeText()
        userText?.let { activityContext.openInputTextEdit(it, textX, textY) }
    }

    private fun onDeleteIconClick() {
        userText = null
        activityContext.clearText()
        activityContext.closeEditSizeText()
        invalidate()
    }

    private fun addDrawingChangeListener(listener: DrawingChangeListener?) {
        drawingChangeListener = listener
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        backgroundBitmap = scaledBitmap
        invalidate()
    }

    fun setBrushSize(size: Int) {
        brushSize = size;
    }

    fun setSizeForText(size: Int) {
        PaintForText.textSize = size.toFloat()
        invalidate()
    }

    private fun calculateTextBoundingRect(): Rect {
        if (userText == null || userText!!.isEmpty()) {
            return Rect(0, 0, 0, 0)
        }
        val textWidth = PaintForText.measureText(userText)
        val textHeight = PaintForText.textSize
        val rectLeft = textX - 10
        val rectTop = textY - textHeight - 10
        val rectRight = textX + textWidth + 10
        val rectBottom = textY + 10
        return Rect(rectLeft.toInt(), rectTop.toInt(), rectRight.toInt(), rectBottom.toInt())
    }

    fun setBrushColorForText(color: Int) {
        brushColorForText = color
        PaintForText.color = color
        invalidate()
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
    fun drawText(text: String, x: Float, y: Float) {
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
            PaintForText.typeface = typeface
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        const val DEFAULT_BRUSH_COLOR_FOR_TEXT: Int = Color.BLACK
        const val DEFAULT_BRUSH_SIZE: Int = 20
        const val DEFAULT_BRUSH_COLOR: Int = Color.WHITE
        const val DEFAULT_BG_COLOR: Int = Color.WHITE
        private const val DEFAULT_TOUCH_TOLERANCE = 4f
    }
}