package com.example.editor_app_intern.ui.album

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.editor_app_intern.model.Date
import com.example.editor_app_intern.model.Image
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AlbumViewModel : ViewModel() {
    private val _dates = MutableLiveData<List<Date>>()
    val dates: LiveData<List<Date>> get() = _dates

    fun loadImagesFromFolder(folderPath: String) {
        val folder = File(folderPath)
        val imagesByDate = mutableMapOf<String, MutableList<Image>>()
        if (!folder.exists()) {
            _dates.value = emptyList()
            return
        }

        if (folder.isDirectory) {
            val files = folder.listFiles { file ->
                file.isFile && file.extension in listOf(
                    "jpg",
                    "jpeg",
                    "png"
                )
            }
            files?.forEach { file ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateString = dateFormat.format(Date(file.lastModified()))
                if (imagesByDate[dateString] == null) {
                    imagesByDate[dateString] = mutableListOf()
                }
                val timestamp = file.lastModified()
                imagesByDate[dateString]?.add(Image(file.toURI().toString(), timestamp))
            }
            val dateList = imagesByDate.map { Date(it.key, it.value) }
            _dates.value = dateList
        }
    }
}