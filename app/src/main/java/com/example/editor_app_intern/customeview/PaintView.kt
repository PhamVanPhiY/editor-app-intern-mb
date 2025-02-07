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
import com.example.editor_app_intern.SharedPreferences
import com.example.editor_app_intern.model.StickerLocal
import com.example.editor_app_intern.model.TextItem
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

    val PaintForText = Paint().apply {
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
        style = Paint.Style.FILL
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
    var selectedTextItem: TextItem? = null
    var selectedStickerItem: StickerLocal? = null
    private val textItems = mutableListOf<TextItem>()
    val stickerItems = mutableListOf<StickerLocal>()
    private var previousBackgroundBitmap: Bitmap? = null
    private var mPath: Path? = null
    var isTextBoxVisible = true
    var isStickerTextBoxVisible = true
    private var isDraggingTextBox = false
    private var isDraggingSticker = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialTouchStickerX = 0f
    private var preferences: SharedPreferences
    private var initialTouchStickerY = 0f
    private var isScalingSticker = false
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
    private var eraserRect: Rect? = null
    private val eraserSize = 100



    private val iconEditText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.edit)
    private val iconEditSticker: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.edit)
    private val iconDeleteText: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.remove_text)
    private val iconDeleteSticker: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.remove_text)
    private val iconScaleText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom)
    private val iconScaleSticker: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom)
    private val iconRotateText: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.reload)
    private val iconRotateSticker: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.reload)
    private val iconWidth = 50
    private val iconHeight = 50

    private val deleteIconRect = Rect()
    private val deleteIconRectSticker = Rect()
    private val editIconRect = Rect()
    private val editIconRectSticker = Rect()
    private val rotateIconRect = Rect()
    private val rotateIconRectSticker = Rect()
    private val scaleIconRect = Rect()
    private val scaleIconRectSticker = Rect()

    private val scaledIconEditText: Bitmap =
        Bitmap.createScaledBitmap(iconEditText, iconWidth, iconHeight, true)
    private val scaledIconDeleteText: Bitmap =
        Bitmap.createScaledBitmap(iconDeleteText, iconWidth, iconHeight, true)
    private val scaledIconScaleText: Bitmap =
        Bitmap.createScaledBitmap(iconScaleText, iconWidth, iconHeight, true)
    private val scaledIconRotateText: Bitmap =
        Bitmap.createScaledBitmap(iconRotateText, iconWidth, iconHeight, true)

    private val scaledIconEditSticker: Bitmap =
        Bitmap.createScaledBitmap(iconEditSticker, iconWidth, iconHeight, true)
    private val scaledIconDeleteSticker: Bitmap =
        Bitmap.createScaledBitmap(iconDeleteSticker, iconWidth, iconHeight, true)
    private val scaledIconScaleSticker: Bitmap =
        Bitmap.createScaledBitmap(iconScaleSticker, iconWidth, iconHeight, true)
    private val scaledIconRotateSticker: Bitmap =
        Bitmap.createScaledBitmap(iconRotateSticker, iconWidth, iconHeight, true)
    private var mCanvas: Canvas? = null
    var isDrawingEnabled: Boolean = false
    private val mBitmapPaint = Paint().apply {
        textSize = 50f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    init {
        preferences = SharedPreferences(context!!)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(canvasBitmap!!)
    }


    fun removeBackground(inputBitmap: Bitmap, progressBar: ProgressBar) {
        previousBackgroundBitmap = backgroundBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        CoroutineScope(Dispatchers.IO).launch {
            remover?.clearBackground(inputBitmap)?.collect { outputBitmap ->
                outputBitmap?.let {
                    Log.d("PaintView", "Output Bitmap size: ${it.width} x ${it.height}")
                    withContext(Dispatchers.Main) {
                        updateBackgroundBitmap(it)
                        preferences.clearBackgroundBitmap()
                        preferences.saveBackgroundBitmap(it)
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun undoRemoveBackground() {
        previousBackgroundBitmap?.let {
            preferences.clearBackgroundBitmap()
            preferences.saveBackgroundBitmap(it)
            val backgroundBitmapBeforermBg = preferences.getBackgroundBitmap()
            if (backgroundBitmapBeforermBg != null) {
                updateBackgroundBitmap(backgroundBitmapBeforermBg)
            }
            previousBackgroundBitmap = null
        }
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
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
            mPaint.color = drawingPath.color
            mPaint.strokeWidth = drawingPath.strokeWidth.toFloat()
            mPaint.style = Paint.Style.STROKE
            mCanvas!!.drawPath(drawingPath.path, mPaint)
        }

        if (isEraserEnabled && eraserRect != null) {
            mCanvas!!.drawRect(eraserRect!!, erasePaint)
        }
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, mBitmapPaint)
        if (textItems.isNotEmpty()) {
            for (textItem in textItems) {
                val boundingRect: Rect
                if (textItem == selectedTextItem && isTextBoxVisible) {
                    boundingRect = calculateTextBoundingRect(selectedTextItem!!)
                    mCanvas!!.drawRect(boundingRect, borderPaint)
                    drawTextIcons(
                        boundingRect.left.toFloat(),
                        boundingRect.top.toFloat(),
                        boundingRect.right.toFloat(),
                        boundingRect.bottom.toFloat()
                    )
                } else {
                    boundingRect = calculateTextBoundingRect(textItem)
                }

                PaintForText.apply {
                    textSize = textItem.size
                    color = textItem.color
                    typeface = Typeface.createFromAsset(context.assets, textItem.fontPath)
                }

                val textWidth = PaintForText.measureText(textItem.text)
                val textHeight = textItem.size
                val textX = boundingRect.left + (boundingRect.width() - textWidth) / 2
                val textY = boundingRect.top + (boundingRect.height() + textHeight) / 2 - 10
                mCanvas!!.drawText(textItem.text, textX, textY, PaintForText)
            }
        }

        for (sticker in stickerItems) {
            val bitmapFromPath = BitmapFactory.decodeFile(sticker.path)
            if (bitmapFromPath != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmapFromPath,
                    sticker.widthSticker.toInt(),
                    sticker.heightSticker.toInt(),
                    true
                )
                mCanvas!!.drawBitmap(scaledBitmap, sticker.x, sticker.y, paintImage)
                if (sticker.isSelected && isStickerTextBoxVisible) {
                    val stickerRect =
                        calculateStickerBoundingRect(scaledBitmap, sticker.x, sticker.y)
                    mCanvas!!.drawRect(stickerRect, borderPaint)
                    val iconSize = 10f
                    val padding = 20

                    deleteIconRectSticker.set(
                        (stickerRect.left - iconSize / 2 - padding).toInt(),
                        (stickerRect.top - iconSize / 2 - padding).toInt(),
                        (stickerRect.left - iconSize / 2 + iconSize + padding).toInt(),
                        (stickerRect.top - iconSize / 2 + iconSize + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconDeleteSticker, null, deleteIconRectSticker, null)

                    editIconRectSticker.set(
                        (stickerRect.right - iconSize / 2 - padding).toInt(),
                        (stickerRect.top - iconSize / 2 - padding).toInt(),
                        (stickerRect.right - iconSize / 2 + iconSize + padding).toInt(),
                        (stickerRect.top - iconSize / 2 + iconSize + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconEditSticker, null, editIconRectSticker, null)

                    rotateIconRectSticker.set(
                        (stickerRect.left - iconSize / 2 - padding).toInt(),
                        (stickerRect.bottom - iconSize / 2 - padding).toInt(),
                        (stickerRect.left - iconSize / 2 + iconSize + padding).toInt(),
                        (stickerRect.bottom - iconSize / 2 + iconSize + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconRotateSticker, null, rotateIconRectSticker, null)

                    scaleIconRectSticker.set(
                        (stickerRect.right - iconSize / 2 - padding).toInt(),
                        (stickerRect.bottom - iconSize / 2 - padding).toInt(),
                        (stickerRect.right - iconSize / 2 + iconSize + padding).toInt(),
                        (stickerRect.bottom - iconSize / 2 + iconSize + padding).toInt()
                    )
                    mCanvas!!.drawBitmap(scaledIconScaleText, null, scaleIconRectSticker, null)
                }
            } else {
                Log.e("BitmapLoading", "Bitmap is null for path: ${sticker.path}")
            }
        }

        canvas.restore()

    }

    fun startTouch(x: Float, y: Float) {
        if (isEraserEnabled) {
            mPath = Path()
        } else {
            mPath = Path()
            val drawingPath = DrawingPath(
                brushColor, brushSize, mPath!!
            )
            paths.add(drawingPath)
        }
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
        invalidate()
    }

    private fun drawTextIcons(
        rectLeft: Float, rectTop: Float, rectRight: Float, rectBottom: Float
    ) {
        val iconSize = 40f
        deleteIconRect.set(
            (rectLeft - iconSize / 2).toInt(),
            (rectTop - iconSize / 2).toInt(),
            (rectLeft + iconSize / 2).toInt(),
            (rectTop + iconSize / 2).toInt()
        )
        mCanvas!!.drawBitmap(scaledIconDeleteText, null, deleteIconRect, null)

        editIconRect.set(
            (rectRight - iconSize / 2).toInt(),
            (rectTop - iconSize / 2).toInt(),
            (rectRight + iconSize / 2).toInt(),
            (rectTop + iconSize / 2).toInt()
        )
        mCanvas!!.drawBitmap(scaledIconEditText, null, editIconRect, null)

        rotateIconRect.set(
            (rectLeft - iconSize / 2).toInt(),
            (rectBottom - iconSize / 2).toInt(),
            (rectLeft + iconSize / 2).toInt(),
            (rectBottom + iconSize / 2).toInt()
        )
        mCanvas!!.drawBitmap(scaledIconRotateText, null, rotateIconRect, null)

        scaleIconRect.set(
            (rectRight - iconSize / 2).toInt(),
            (rectBottom - iconSize / 2).toInt(),
            (rectRight + iconSize / 2).toInt(),
            (rectBottom + iconSize / 2).toInt()
        )
        mCanvas!!.drawBitmap(scaledIconScaleText, null, scaleIconRect, null)
    }

    private fun touchMove(x: Float, y: Float) {
        if (isEraserEnabled) {
            eraserRect = Rect(
                (x - eraserSize / 2).toInt(),
                (y - eraserSize / 2).toInt(),
                (x + eraserSize / 2).toInt(),
                (y + eraserSize / 2).toInt()
            )
            mCanvas!!.drawRect(eraserRect!!, erasePaint)
        } else if (mPath != null) {

            val dx = abs((x - mX).toDouble()).toFloat()
            val dy = abs((y - mY).toDouble()).toFloat()

            if (dx >= touchTolerance || dy >= touchTolerance) {
                mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mCanvas!!.drawPath(mPath!!, mPaint)
                mX = x
                mY = y
            }
        }
        invalidate()
    }

    private fun touchUp() {
        if (isEraserEnabled) {
            if (eraserRect != null) {
                mCanvas!!.drawRect(eraserRect!!, erasePaint)
                eraserRect = null
            }
        } else {
            if (mPath != null) {
                mPath!!.lineTo(mX, mY)
                paths.add(DrawingPath(brushColor, brushSize, mPath!!))
                mPath = null
            }
        }
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


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedTextItem = textItems.find { textItem ->
                    val textWidth = mPaint.measureText(textItem.text)
                    val textHeight = mPaint.textSize
                    val rectLeft = textItem.x - 20
                    val rectTop = textItem.y - textHeight - 20
                    val rectRight = textItem.x + textWidth + 20
                    val rectBottom = textItem.y + 20
                    x in rectLeft..rectRight && y >= rectTop && y <= rectBottom
                }
                if (selectedTextItem != null) {
                    if (deleteIconRect.contains(x.toInt(), y.toInt())) {
                        onDeleteTextItem(selectedTextItem!!)
                        activityContext.closeEditSizeText()
                        return true
                    } else if (editIconRect.contains(x.toInt(), y.toInt())) {
                        onEditTextItem(selectedTextItem!!)
                        activityContext.closeEditSizeText()
                        return true
                    }
                    activityContext.setupSeekBarForText(selectedTextItem!!)
                    activityContext.openEditSizeText()
                    isDraggingTextBox = true
                    isEditingText = true
                    isTextBoxVisible = true
                    initialTouchX = x
                    initialTouchY = y

                    invalidate()
                }
                stickerItems.forEach { sticker ->
                    sticker.isSelected = (sticker == selectedStickerItem)
                }
                selectedStickerItem = stickerItems.find { sticker ->
                    val pathSticker = sticker.path
                    if (pathSticker.isNullOrEmpty()) {
                        return@find false
                    }

                    val bitmapFromPath = BitmapFactory.decodeFile(pathSticker)
                    if (bitmapFromPath == null) {
                        return@find false
                    }
                    val scaledWidth = sticker.widthSticker
                    val scaledHeight = sticker.heightSticker
                    val rectLeft = sticker.x
                    val rectTop = sticker.y
                    val rectRight = sticker.x + scaledWidth
                    val rectBottom = sticker.y + scaledHeight

                    x in rectLeft..rectRight && y in rectTop..rectBottom
                }
                if (selectedStickerItem != null) {
                    if (deleteIconRectSticker.contains(x.toInt(), y.toInt())) {
                        onDeleteIconClickSticker(selectedStickerItem!!)
                        return true
                    } else if (scaleIconRectSticker.contains(x.toInt(), y.toInt())) {
                        isScalingSticker = true
                        initialTouchX = x
                        initialTouchY = y
                        return true
                    }
                    isDraggingSticker = true
                    initialTouchStickerX = x
                    initialTouchStickerY = y
                    isStickerTextBoxVisible = true

                    invalidate()
                }
                if (isStickerTouched(x, y)) {
                    isDraggingSticker = true
                    initialTouchStickerX = x
                    initialTouchStickerY = y
                    isStickerTextBoxVisible = true
                } else {
                    if (isStickerTextBoxVisible) {
                        isStickerTextBoxVisible = false
                        invalidate()
                    }

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
                if (isDraggingTextBox && selectedTextItem != null) {
                    val dx = x - initialTouchX
                    val dy = y - initialTouchY
                    selectedTextItem!!.x += dx
                    selectedTextItem!!.y += dy
                    initialTouchX = x
                    initialTouchY = y
                } else if (isDraggingSticker && selectedStickerItem != null) {
                    val dx = x - initialTouchStickerX
                    val dy = y - initialTouchStickerY
                    selectedStickerItem!!.x += dx
                    selectedStickerItem!!.y += dy
                    initialTouchStickerX = x
                    initialTouchStickerY = y
                    preferences.saveSticker(selectedStickerItem!!)

                } else if (isScalingSticker && selectedStickerItem != null) {
                    val dx = x - initialTouchX
                    val dy = y - initialTouchY
                    val newWidth = (selectedStickerItem!!.widthSticker + dx).coerceAtLeast(50f)
                    val newHeight = (selectedStickerItem!!.heightSticker + dy).coerceAtLeast(50f)
                    selectedStickerItem!!.widthSticker = newWidth
                    selectedStickerItem!!.heightSticker = newHeight
                    initialTouchX = x
                    initialTouchY = y
                    invalidate()
                    preferences.saveSticker(selectedStickerItem!!)
                }else {
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
                } else if (isScalingSticker) {
                    isScalingSticker = false
                }  else {
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


    private fun onEditTextItem(selectedTextItem: TextItem) {
        textItems.remove(selectedTextItem)
        activityContext.openInputTextEdit(
            selectedTextItem.text, selectedTextItem.x, selectedTextItem.y
        )
    }

    private fun onDeleteTextItem(textItem: TextItem) {
        textItems.remove(textItem)
        preferences.removeTextItem(textItem)
        invalidate()
    }

    private fun onDeleteIconClickSticker(selectedStickerItem: StickerLocal) {
        stickerItems.remove(selectedStickerItem)
        preferences.removeSticker(selectedStickerItem)
        invalidate()
    }


    private fun isStickerTouched(x: Float, y: Float): Boolean {
        for (sticker in stickerItems) {
            val pathSticker = sticker.path

            if (pathSticker.isNullOrEmpty()) {
                continue
            }

            val bitmapFromPath = BitmapFactory.decodeFile(pathSticker)
            if (bitmapFromPath == null) {
                continue
            }

            val scaledWidth = sticker.widthSticker
            val scaledHeight = sticker.heightSticker

            val stickerRect = Rect(
                sticker.x.toInt(),
                sticker.y.toInt(),
                (sticker.x + scaledWidth).toInt(),
                (sticker.y + scaledHeight).toInt()
            )
            if (x >= stickerRect.left && x <= stickerRect.right && y >= stickerRect.top && y <= stickerRect.bottom) {
                return true
            }
        }
        return false
    }

    private fun isTextTouched(x: Float, y: Float): Boolean {
        for (textItem in textItems) {
            val textWidth = mPaint.measureText(textItem.text)
            val textHeight = mPaint.textSize
            val rectLeft = textItem.x - 20
            val rectTop = textItem.y - textHeight - 20
            val rectRight = textItem.x + textWidth + 20
            val rectBottom = textItem.y + 20
            if (x >= rectLeft && x <= rectRight && y >= rectTop && y <= rectBottom) {
                return true
            }
        }
        return false
    }

    fun updateSelectedTextSize(size: Float) {
        selectedTextItem?.let {
            it.size = size
            invalidate()
        }
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        backgroundBitmap = scaledBitmap
        invalidate()
    }

    fun loadTextItems() {
        textItems.clear()
        textItems.addAll(preferences.getTextItems() ?: emptyList())
        invalidate()
    }

    fun loadStickerItems() {
        stickerItems.clear()
        val newStickers = preferences.getStickers()
        preferences.clearStickers()
        stickerItems.addAll(newStickers.distinctBy { it.id })
        invalidate()
    }

    private fun calculateTextBoundingRect(textItem: TextItem): Rect {
        PaintForText.apply {
            textSize = textItem.size
        }
        val textWidth = PaintForText.measureText(textItem.text)
        val textHeight = textItem.size
        val padding = 20
        val rectLeft = textItem.x - padding
        val rectTop = textItem.y - textHeight - padding
        val rectRight = textItem.x + textWidth + padding
        val rectBottom = textItem.y + padding
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

    fun saveTextItems(preferences: SharedPreferences) {
        preferences.saveTextItems(textItems)
    }

    fun loadTextItems(preferences: SharedPreferences) {
        textItems.clear()
        textItems.addAll(preferences.getTextItems() ?: emptyList())
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

    fun addText(
        id: String,
        text: String,
        x: Float,
        y: Float,
        pathFont: String,
        color: Int,
        size: Float
    ) {
        textItems.add(TextItem(id, text, x, y, pathFont, color, size))
        invalidate()
    }


    companion object {
        const val DEFAULT_BRUSH_COLOR_FOR_TEXT: Int = Color.BLACK
        const val DEFAULT_BRUSH_SIZE: Int = 20
        const val DEFAULT_BRUSH_COLOR: Int = Color.BLACK
        const val DEFAULT_BG_COLOR: Int = Color.BLACK
        private const val DEFAULT_TOUCH_TOLERANCE = 4f
    }
}