package com.example.editor_app_intern.ui.sticker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.editor_app_intern.model.Sticker
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class StickerViewModel(application: Application) : AndroidViewModel(application) {
    private val _stickers = MutableLiveData<List<Sticker>>()
    val stickers: LiveData<List<Sticker>> get() = _stickers

    private val storage: FirebaseStorage = Firebase.storage

    fun loadStickersFromJson() {
        val jsonFileRef = storage.reference.child("list_sticker/list_sticker.json")
        jsonFileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val json = String(bytes)
            val gson = Gson()
            val stickerListType = object : TypeToken<List<Sticker>>() {}.type
            val stickers: List<Sticker> = gson.fromJson(json, stickerListType)
            _stickers.value = stickers
            Log.d("StickerViewModel", "Loaded stickers: $stickers")
        }.addOnFailureListener { exception ->
            Log.e("StickerViewModel", "Error loading stickers: ${exception.message}")
        }
    }
}