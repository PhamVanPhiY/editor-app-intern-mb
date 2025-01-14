package com.example.editor_app_intern.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.editor_app_intern.model.FontItem

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val _fontList = MutableLiveData<List<FontItem>>()
    val fontList: LiveData<List<FontItem>> get() = _fontList

    init {
        loadFonts()
    }

    private fun loadFonts() {
        val fonts = listOf(
            FontItem("fonts/poppins_bold.ttf", "Poppins Bold"),
            FontItem("fonts/lato_regular.ttf", "Lato Bold"),
            FontItem("fonts/rubik_regular.ttf", "Rubik Regular"),
            FontItem("fonts/playwrite_regular.ttf", "PlayWrite Regular"),
            FontItem("fonts/cookie_regular.ttf", "Cookie Regular"),
            FontItem("fonts/barriecito_regular.ttf", "Barriecito Regular"),
            FontItem("fonts/seriftext_regular.ttf", "Serif Regular"),
            FontItem("fonts/synemono_regular.ttf", "SyneMoMo Regular"),
            FontItem("fonts/nabla_regular.ttf", "Nabla Regular"),
            FontItem("fonts/eater_regular.ttf", "Eater Regular"),
        )
        _fontList.value = fonts
    }
}
