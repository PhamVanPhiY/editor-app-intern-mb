package com.example.editor_app_intern.customeview

interface DrawingChangeListener {
    fun onTouchStart(x: Float, y: Float)
    fun onDrawingChange(x: Float, y: Float)
}