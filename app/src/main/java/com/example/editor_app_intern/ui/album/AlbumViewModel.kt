package com.example.editor_app_intern.ui.album

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.editor_app_intern.model.Date
import com.example.editor_app_intern.model.Image
import java.text.SimpleDateFormat
import java.util.*

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val _dates = MutableLiveData<List<Date>>()
    val dates: LiveData<List<Date>> get() = _dates

    fun loadImagesFromGallery() {
        val imagesByDate = mutableMapOf<String, MutableList<Image>>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA

        )

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/Pictures/%")

        val cursor = getApplication<Application>().contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val name = c.getString(nameColumn)
                val dateModified = c.getLong(dateColumn)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateString =
                    dateFormat.format(Date(dateModified * 1000))

                if (imagesByDate[dateString] == null) {
                    imagesByDate[dateString] = mutableListOf()
                }
                imagesByDate[dateString]?.add(
                    Image(
                        ContentUris.withAppendedId(queryUri, id).toString(), dateModified
                    )
                )
            }
        }

        val dateList = imagesByDate.map { Date(it.key, it.value) }
        _dates.value = dateList
        Log.d("AlbumViewModel", "Date List: $dateList")
    }

    fun sortImagesByNewest() {
        _dates.value = _dates.value
            ?.sortedByDescending { date -> date.imageModel.maxOfOrNull { it.timestamp } }
            ?.map { date ->
                date.copy(imageModel = date.imageModel.sortedByDescending { it.timestamp })
            }
    }

    fun sortImagesByOldest() {
        _dates.value = _dates.value
            ?.sortedBy { date -> date.imageModel.minOfOrNull { it.timestamp } }
            ?.map { date ->
                date.copy(imageModel = date.imageModel.sortedBy { it.timestamp })
            }
    }
}