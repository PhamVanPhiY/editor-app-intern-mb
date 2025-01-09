package com.example.editor_app_intern.customeview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.EditText

class CustomEditText(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    private val paint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        // Tạo Typeface từ font trong thư mục assets
        val typeface = Typeface.createFromAsset(context.assets, "fonts/rubik_regular.ttf")
        this.typeface = typeface
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText("Custom Text", 50f, 100f, paint)
    }
}