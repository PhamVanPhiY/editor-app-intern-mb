package com.example.editor_app_intern.model

data class TextItem(
    val id: String,
    val text: String,
    var x: Float,
    var y: Float,
    var fontPath: String,
    var color: Int,
    var size: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextItem) return false
        return id == other.id
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
}