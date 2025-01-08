package com.example.editor_app_intern.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.editor_app_intern.R
import com.example.editor_app_intern.model.FilterCamera

class CameraViewModel (application: Application) : AndroidViewModel(application) {

    private val _filters = MutableLiveData<List<FilterCamera>>()
    val filters: LiveData<List<FilterCamera>> get() = _filters

    init {
        loadFilters()
    }

    private fun loadFilters() {
        val filterList = listOf(
            FilterCamera( R.drawable.filter_normal,"Normal"), // You should replace with actual drawable resource
            FilterCamera( R.drawable.filter_sketch,"Sketch"),
            FilterCamera( R.drawable.filter_invert,"Invert"),
            FilterCamera(R.drawable.filter_solarize,"Solarize"),
            FilterCamera( R.drawable.filter_gray_scale,"GrayScale"),
            FilterCamera( R.drawable.filter_brightness_03f,"Brightness"),
            FilterCamera( R.drawable.filter_constrast,"Contrast"),
            FilterCamera( R.drawable.filter_pixelation,"Pixelation"),
            FilterCamera( R.drawable.filter_glass,"Glass Sphere"),
            FilterCamera( R.drawable.filter_cross_hatch,"Crosshatch"),
            FilterCamera( R.drawable.filter_gamma_2f,"Gamma")
        )

        _filters.value = filterList
    }
}