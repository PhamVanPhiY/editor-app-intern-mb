package com.example.editor_app_intern

import android.content.Context
import android.content.SharedPreferences

class SharedPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences? =
        context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)

    companion object {
        private const val TIMER_KEY = "timer_value"
        private const val IMAGE_PATH_KEY = "image_path"
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
}
