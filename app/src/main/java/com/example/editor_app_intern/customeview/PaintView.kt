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
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
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

    private val mPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = DEFAULT_BRUSH_COLOR
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 0xff
        setXfermode(null)
        brushColor = DEFAULT_BRUSH_COLOR
        backgroundColor = DEFAULT_BG_COLOR
        brushSize = DEFAULT_BRUSH_SIZE
        touchTolerance = DEFAULT_TOUCH_TOLERANCE
        textSize = 100f
    }

    private val erasePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val paintImage = Paint().apply {
        isAntiAlias = true
    }

    private val remover = context?.let { RemoveBg(it) }
    private var mX = 0f
    private var mY = 0f
    private var previousBackgroundBitmap: Bitmap? = null
    private var borderColor: Int = Color.BLACK
    private var mPath: Path? = null
    var isTextBoxVisible = true
    var isStickerTextBoxVisible = true
    private var isDraggingTextBox = false
    private var isDraggingSticker = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialTouchStickerX = 0f
    private var initialTouchStickerY = 0f
    private var isScalingSticker = false
    private var stickerWidth: Float = 200f
    private var stickerHeight: Float = 200f
    private var rectWidth: Float = stickerWidth
    private var rectHeight: Float = stickerHeight
    var isEraserEnabled: Boolean = false
    var isEditingText = false
    private val paths = ArrayList<DrawingPath>()
    private val undoPaths = ArrayList<DrawingPath>()
    private var brushColor: Int
    private var brushColorForText: Int
    var backgroundBitmap: Bitmap? = null
    private var backgroundColor: Int
    private val activityContext = context as EditActivity
    private var brushSize: Int = 10
    var touchTolerance: Float
    var canvasBitmap: Bitmap? = null
    var userText: String? = null
    var pathSticker: String? = null
    private var textX = 200f
    private var textY = 200f

    private var textXSticker = 100f
    private var textYSticker = 100f

    private val iconEditText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.edit)
    private val iconDeleteText: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.remove_text)
    private val iconDeleteSticker: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.remove_text)
    private val iconScaleText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom)
    private val iconScaleSticker: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom)
    private val iconRotateText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.reload)

    private val iconWidth = 50
    private val iconHeight = 50

    private val deleteIconRect = Rect()
    private val deleteIconRectSticker = Rect()
    private val editIconRect = Rect()
    private val rotateIconRect = Rect()
    private val scaleIconRect = Rect()
    private val scaleIconRectSticker = Rect()


    private val scaledIconEditText: Bitmap =
        Bitmap.createScaledBitmap(iconEditText, iconWidth, iconHeight, true)
    private val scaledIconDeleteText: Bitmap =
        Bitmap.createScaledBitmap(iconDeleteText, iconWidth, iconHeight, true)
    private val scaledIconDeleteSticker: Bitmap =
        Bitmap.createScaledBitmap(iconDeleteSticker, iconWidth, iconHeight, true)
    private val scaledIconScaleText: Bitmap =
        Bitmap.createScaledBitmap(iconScaleText, iconWidth, iconHeight, true)
    private val scaledIconScaleSticker: Bitmap =
        Bitmap.createScaledBitmap(iconScaleSticker, iconWidth, iconHeight, true)
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

    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }

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
                Log.d("PaintView", "isEraserEnabled: $isEraserEnabled")
                mCanvas!!.drawPath(drawingPath.path, erasePaint)
            } else {
                mPaint.color = drawingPath.color
                mPaint.strokeWidth = drawingPath.strokeWidth.toFloat()
                mPaint.style = Paint.Style.STROKE
                mCanvas!!.drawPath(drawingPath.path, mPaint)
            }
        }
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, mBitmapPaint)

        if (userText != null) {
            mCanvas!!.drawText(userText!!, textX, textY, PaintForText)
        }
        if (pathSticker != null) {
            val bitmapFromPath = BitmapFactory.decodeFile(pathSticker)
            if (bitmapFromPath != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmapFromPath,
                    stickerWidth.toInt(),
                    stickerHeight.toInt(),
                    true
                )
                mCanvas!!.drawBitmap(scaledBitmap, textXSticker, textYSticker, paintImage)

                if (isStickerTextBoxVisible) {
                    val stickerRect =
                        calculateStickerBoundingRect(scaledBitmap, textXSticker, textYSticker)
                    mCanvas!!.drawRect(stickerRect, borderPaint)

                    // Vẽ các icon
                    val padding = 20
                    deleteIconRectSticker.set(
                        (stickerRect.left - scaledIconDeleteSticker.width / 2 - padding).toInt(),
                        (stickerRect.top - scaledIconDeleteSticker.height / 2 - padding).toInt(),
                        (stickerRect.left - scaledIconDeleteSticker.width / 2 + scaledIconDeleteSticker.width + padding).toInt(),
                        (stickerRect.top - scaledIconDeleteSticker.height / 2 + scaledIconDeleteSticker.height + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconDeleteSticker, null, deleteIconRectSticker, null)

                    editIconRect.set(
                        (stickerRect.right - scaledIconEditText.width / 2 - padding).toInt(),
                        (stickerRect.top - scaledIconEditText.height / 2 - padding).toInt(),
                        (stickerRect.right - scaledIconEditText.width / 2 + scaledIconEditText.width + padding).toInt(),
                        (stickerRect.top - scaledIconEditText.height / 2 + scaledIconEditText.height + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconEditText, null, editIconRect, null)

                    rotateIconRect.set(
                        (stickerRect.left - scaledIconRotateText.width / 2 - padding).toInt(),
                        (stickerRect.bottom - scaledIconRotateText.height / 2 - padding).toInt(),
                        (stickerRect.left - scaledIconRotateText.width / 2 + scaledIconRotateText.width + padding).toInt(),
                        (stickerRect.bottom - scaledIconRotateText.height / 2 + scaledIconRotateText.height + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconRotateText, null, rotateIconRect, null)

                    scaleIconRectSticker.set(
                        (stickerRect.right - scaledIconScaleSticker.width / 2 - padding).toInt(),
                        (stickerRect.bottom - scaledIconScaleSticker.height / 2 - padding).toInt(),
                        (stickerRect.right - scaledIconScaleSticker.width / 2 + scaledIconScaleSticker.width + padding).toInt(),
                        (stickerRect.bottom - scaledIconScaleSticker.height / 2 + scaledIconScaleSticker.height + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconScaleSticker, null, scaleIconRectSticker, null)
                }
            } else {
                Log.e("BitmapLoading", "Bitmap is null for path: $pathSticker")
            }
        }

        if (isTextBoxVisible) {
            val boundingRect = calculateTextBoundingRect()
            userText?.let {
                mCanvas!!.drawRect(boundingRect, borderPaint)
                mCanvas!!.drawBitmap(
                    scaledIconDeleteText, null, Rect(
                        boundingRect.left - scaledIconDeleteText.width / 2,
                        boundingRect.top - scaledIconDeleteText.height / 2,
                        boundingRect.left - scaledIconDeleteText.width / 2 + scaledIconDeleteText.width,
                        boundingRect.top - scaledIconDeleteText.height / 2 + scaledIconDeleteText.height
                    ), null
                )

                mCanvas!!.drawBitmap(
                    scaledIconEditText, null, Rect(
                        boundingRect.right - scaledIconEditText.width / 2,
                        boundingRect.top - scaledIconEditText.height / 2,
                        boundingRect.right - scaledIconEditText.width / 2 + scaledIconEditText.width,
                        boundingRect.top - scaledIconEditText.height / 2 + scaledIconEditText.height
                    ), null
                )

                mCanvas!!.drawBitmap(
                    scaledIconRotateText, null, Rect(
                        boundingRect.left - scaledIconRotateText.width / 2,
                        boundingRect.bottom - scaledIconRotateText.height / 2,
                        boundingRect.left - scaledIconRotateText.width / 2 + scaledIconRotateText.width,
                        boundingRect.bottom - scaledIconRotateText.height / 2 + scaledIconRotateText.height
                    ), null
                )

                mCanvas!!.drawBitmap(
                    scaledIconScaleText, null, Rect(
                        boundingRect.right - scaledIconScaleText.width / 2,
                        boundingRect.bottom - scaledIconScaleText.height / 2,
                        boundingRect.right - scaledIconScaleText.width / 2 + scaledIconScaleText.width,
                        boundingRect.bottom - scaledIconScaleText.height / 2 + scaledIconScaleText.height
                    ), null
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

    fun startTouch(x: Float, y: Float) {
        mPath = Path()
        if (isEraserEnabled) {

        } else {
            val drawingPath = DrawingPath(
                brushColor, brushSize, mPath!!
            )
            paths.add(drawingPath)
        }
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
            if (isEraserEnabled) {
                mPath!!.lineTo(x, y)
            } else {
                mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            }
            mX = x
            mY = y
            invalidate()
        }
    }


    private fun touchUp() {
        if (mPath != null) {
            if (isEraserEnabled) {
                mPath!!.lineTo(mX, mY)
                mCanvas!!.drawPath(mPath!!, erasePaint)
                mPath = null
            } else {
                mPath!!.lineTo(mX, mY)
                paths.add(DrawingPath(brushColor, brushSize, mPath!!))
                mPath = null
            }
            invalidate()

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
                } else if (isStickerTouched(x, y)) {
                    isDraggingSticker = true
                    initialTouchStickerX = x
                    initialTouchStickerY = y
                    isStickerTextBoxVisible = true
                } else {
                    if (isStickerTextBoxVisible) {
                        isStickerTextBoxVisible = false
                        Log.d("isStickerVisible", "isStickerVisible = $isStickerTextBoxVisible")
                        invalidate()
                    }
                    if (deleteIconRect.contains(x.toInt(), y.toInt())) {
                        onDeleteIconClick()
                        return true
                    } else if (editIconRect.contains(x.toInt(), y.toInt())) {
                        onEditIconClick()
                        return true
                    }
                    if (deleteIconRectSticker.contains(x.toInt(), y.toInt())) {
                        onDeleteIconClickSticker()
                        return true
                    }
                    if (scaleIconRectSticker.contains(x.toInt(), y.toInt())) {
                        isScalingSticker = true
                        initialTouchX = x
                        initialTouchY = y
                        return true
                    }
                    // Ẩn textbox nếu không chạm vào
                    if (isTextBoxVisible && !isTextTouched(x, y)) {
                        isTextBoxVisible = false
                        activityContext.closeEditSizeText()
                        invalidate()
                        return true
                    }
                    if (!isTextBoxVisible && !isTextTouched(x, y)) {
                        activityContext.closeEditHue()
                    }
                    if (isEraserEnabled) {
                        startTouch(x, y)
                    } else if (isDrawingEnabled) {
                        startTouch(x, y)
                    }
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
                } else if (isDraggingSticker) {
                    val dx = x - initialTouchStickerX
                    val dy = y - initialTouchStickerY
                    textXSticker += dx
                    textYSticker += dy
                    initialTouchStickerX = x
                    initialTouchStickerY = y
                    Log.d("Toa do", "x: $initialTouchStickerX, y: $initialTouchStickerY")
                } else if (isScalingSticker) {
                    val dx = x - initialTouchX
                    val dy = y - initialTouchY
                    stickerWidth = (stickerWidth + dx).coerceAtLeast(50f)
                    stickerHeight = (stickerHeight + dy).coerceAtLeast(50f)
                    rectWidth = stickerWidth
                    rectHeight = stickerHeight
                    initialTouchX = x
                    initialTouchY = y
                } else {
                    if (isEraserEnabled) {
                        touchMove(x, y)
                    } else if (isDrawingEnabled) {
                        touchMove(x, y)
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (isDraggingTextBox) {
                    isDraggingTextBox = false
                } else if (isDraggingSticker) {
                    isDraggingSticker = false
                } else if (isScalingSticker) { // Kết thúc việc thay đổi kích thước
                    isScalingSticker = false
                } else {
                    if (isEraserEnabled) {
                        touchUp()
                    } else if (isDrawingEnabled) {
                        touchUp()
                    }
                }
                invalidate()
            }
        }
        if (!isTextBoxVisible && userText != null && isTextTouched(x, y)) {
            isTextBoxVisible = true
            activityContext.openEditSizeText()
            isEditingText = true
            invalidate()
        }

        return true
    }

    private fun onDeleteIconClickSticker() {
        pathSticker = null
        invalidate()
    }

    private fun isStickerTouched(x: Float, y: Float): Boolean {
        if (pathSticker == null) {
            return false
        }

        val bitmapFromPath = BitmapFactory.decodeFile(pathSticker)
        if (bitmapFromPath == null) {
            return false
        }

        val scaledWidth = stickerWidth
        val scaledHeight = stickerHeight
        val stickerRect = Rect(
            textXSticker.toInt(),
            textYSticker.toInt(),
            (textXSticker + scaledWidth).toInt(),
            (textYSticker + scaledHeight).toInt()
        )

        return x >= stickerRect.left && x <= stickerRect.right && y >= stickerRect.top && y <= stickerRect.bottom
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

    private fun calculateStickerBoundingRect(bitmap: Bitmap, x: Float, y: Float): Rect {
        if (bitmap.isRecycled) {
            return Rect(0, 0, 0, 0)
        }
        val rectLeft = x.toInt()
        val rectTop = y.toInt()
        val rectRight = (x + bitmap.width).toInt()
        val rectBottom = (y + bitmap.height).toInt()
        return Rect(rectLeft, rectTop, rectRight, rectBottom)
    }


    fun setBrushColorForText(color: Int) {
        brushColorForText = color
        PaintForText.color = color
        invalidate()
    }

    fun setBrushColor(color: Int) {
        brushColor = color
    }

    fun setDrawText(text: String, x: Float, y: Float) {
        userText = text
        textX = x
        textY = y
        invalidate()
    }

    fun setDrawSticker(path: String, x: Float, y: Float) {
        pathSticker = path
        textX = x
        textY = y
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
        const val DEFAULT_BRUSH_COLOR: Int = Color.BLACK
        const val DEFAULT_BG_COLOR: Int = Color.BLACK
        private const val DEFAULT_TOUCH_TOLERANCE = 4f
    }
}