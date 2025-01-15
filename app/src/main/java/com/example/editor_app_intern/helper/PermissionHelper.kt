package com.example.editor_app_intern.helper

import com.example.editor_app_intern.extension.atLeastVersionR
import com.example.editor_app_intern.extension.atLeastVersionTiramisu
import com.example.editor_app_intern.extension.atLeastVersionUpSideDownCake

object PermissionHelper {
    val PERMISSIONS = atLeastVersionUpSideDownCake {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            android.Manifest.permission.READ_MEDIA_IMAGES,
        )
    } ?: atLeastVersionTiramisu {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
        )
    } ?: atLeastVersionR {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    } ?: arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}
