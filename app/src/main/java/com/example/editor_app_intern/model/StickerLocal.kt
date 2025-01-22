package com.example.editor_app_intern.model
data class StickerLocal(
    val id: String,
    val path: String,
    var widthSticker: Float,
    var heightSticker: Float,
    var x: Float,
    var y: Float,
    var isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StickerLocal) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}