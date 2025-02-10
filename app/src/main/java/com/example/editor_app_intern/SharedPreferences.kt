package com.example.editor_app_intern

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.editor_app_intern.customeview.DrawingPath
import com.example.editor_app_intern.model.DrawingPathDTO
import com.example.editor_app_intern.model.StickerLocal
import com.example.editor_app_intern.model.TextItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream

class SharedPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences? =
        context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "CameraPrefs"
        private const val TIMER_KEY = "timer_value"
        private const val IMAGE_PATH_KEY = "image_path"
        private const val TEXT_ITEMS_KEY = "text_items"
        private const val BACKGROUND_BITMAP_KEY = "background_bitmap"
        private const val STICKERS_KEY = "stickers"
        private const val IMAGE_PATH_ORIGIN_KEY = "image_path_origin"
        private const val PATHS_KEY = "drawing_paths"
    }

    fun saveTimerValue(timerValue: Long) {
        sharedPreferences?.edit()?.putLong(TIMER_KEY, timerValue)?.apply()
    }

    fun getTimerValue(): Long {
        return sharedPreferences?.getLong(TIMER_KEY, 0) ?: 0
    }

    fun clearTimerValue() {
        sharedPreferences?.edit()?.remove(TIMER_KEY)?.apply()
    }

    fun saveImagePath(imagePath: String) {
        sharedPreferences?.edit()?.putString(IMAGE_PATH_KEY, imagePath)?.apply()
    }

    fun getImagePath(): String? {
        return sharedPreferences?.getString(IMAGE_PATH_KEY, null)
    }

    fun clearImagePath() {
        sharedPreferences?.edit()?.remove(IMAGE_PATH_KEY)?.apply()
    }

    fun saveTextItems(textItems: List<TextItem>) {
        val json = Gson().toJson(textItems)
        sharedPreferences?.edit()?.putString(TEXT_ITEMS_KEY, json)?.apply()
    }

    fun getTextItems(): List<TextItem>? {
        val json = sharedPreferences?.getString(TEXT_ITEMS_KEY, null) ?: return null
        val type = object : TypeToken<List<TextItem>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun removeTextItem(textItem: TextItem) {
        val textItems = getTextItems()?.toMutableList() ?: return
        textItems.remove(textItem)
        val json = Gson().toJson(textItems)
        sharedPreferences?.edit()?.putString(TEXT_ITEMS_KEY, json)?.apply()
    }

    fun clearTextItems() {
        sharedPreferences?.edit()?.remove(TEXT_ITEMS_KEY)?.apply()
    }

    fun saveSticker(sticker: StickerLocal) {
        val stickers = getStickers().toMutableList()
        val existingStickerIndex =
            stickers.indexOfFirst { it.id == sticker.id }
        if (existingStickerIndex != -1) {
            stickers[existingStickerIndex] = sticker
        } else {
            stickers.add(sticker)
        }
        val json = Gson().toJson(stickers)
        sharedPreferences?.edit()?.putString(STICKERS_KEY, json)?.apply()
    }

    fun getStickers(): List<StickerLocal> {
        val json = sharedPreferences?.getString(STICKERS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<StickerLocal>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clearStickers() {
        sharedPreferences?.edit()?.remove(STICKERS_KEY)?.apply()
    }

    fun removeSticker(sticker: StickerLocal) {
        val stickers = getStickers().toMutableList()
        stickers.remove(sticker)
        val json = Gson().toJson(stickers)
        sharedPreferences?.edit()?.putString(STICKERS_KEY, json)?.apply()
    }

    fun saveBackgroundBitmap(bitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val encodedBitmap = Base64.encodeToString(byteArray, Base64.DEFAULT)
        sharedPreferences?.edit()?.putString(BACKGROUND_BITMAP_KEY, encodedBitmap)?.apply()
    }

    fun getBackgroundBitmap(): Bitmap? {
        val encodedBitmap = sharedPreferences?.getString(BACKGROUND_BITMAP_KEY, null) ?: return null
        val decodedByteArray = Base64.decode(encodedBitmap, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
    }

    fun clearBackgroundBitmap() {
        sharedPreferences?.edit()?.remove(BACKGROUND_BITMAP_KEY)?.apply()
    }
    fun saveImagePathOrigin(imagePathOrigin: String) {
        sharedPreferences?.edit()?.putString(IMAGE_PATH_ORIGIN_KEY, imagePathOrigin)?.apply()
    }

    fun getImagePathOrigin(): String? {
        return sharedPreferences?.getString(IMAGE_PATH_ORIGIN_KEY, null)
    }
    fun clearImagePathOrigin() {
        sharedPreferences?.edit()?.remove(IMAGE_PATH_ORIGIN_KEY)?.apply()
    }
    fun savePaths(paths: List<DrawingPath>) {
        val dtos = paths.map { DrawingPathDTO(it.color, it.strokeWidth, it.toPathString()) }
        val json = Gson().toJson(dtos)
        sharedPreferences?.edit()?.putString(PATHS_KEY, json)?.apply()
    }

    fun getPaths(): List<DrawingPath> {
        val json = sharedPreferences?.getString(PATHS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<DrawingPathDTO>>() {}.type
        val dtos: List<DrawingPathDTO> = Gson().fromJson(json, type)
        return dtos.map { DrawingPath(it.color, it.strokeWidth, DrawingPath.fromPathString(it.pathString)) }
    }

    fun clearPaths() {
        sharedPreferences?.edit()?.remove(PATHS_KEY)?.apply()
    }
}
