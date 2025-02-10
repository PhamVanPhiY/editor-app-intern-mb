package com.example.editor_app_intern.model

import android.os.Parcel
import android.os.Parcelable

data class StickerLocal(
    val id: String,
    val path: String,
    var widthSticker: Float,
    var heightSticker: Float,
    var x: Float,
    var y: Float,
    var isSelected: Boolean = false,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(path)
        parcel.writeFloat(widthSticker)
        parcel.writeFloat(heightSticker)
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StickerLocal> {
        override fun createFromParcel(parcel: Parcel): StickerLocal {
            return StickerLocal(parcel)
        }

        override fun newArray(size: Int): Array<StickerLocal?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StickerLocal) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}