package com.example.editor_app_intern.customeview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import com.tolunaykan.drawinglibrary.PaintView


class CustomPaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PaintView(context, attrs) {
    private val paint = Paint().apply {
        color = Color.RED // Thay bằng brushColor nếu cần
        textSize = 100f
        val plain = Typeface.createFromAsset(context.assets, "poppins_bold.ttf")
        typeface = plain
    }

    private var backgroundBitmap: Bitmap? = null
    private val backgroundPaint = Paint()

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        backgroundBitmap?.let {
            val srcRect = Rect(0, 0, it.width, it.height)
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(it, srcRect, destRect, backgroundPaint)
        }

    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }

}