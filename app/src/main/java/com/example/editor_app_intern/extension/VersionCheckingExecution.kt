package com.example.editor_app_intern.extension

import android.os.Build


fun <T> atLeastVersionUpSideDownCake(block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        block()
    } else null
}

fun <T> atLeastVersionTiramisu(block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        block()
    } else null
}

fun <T> atLeastVersionS(block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        block()
    } else null
}

fun <T> atLeastVersionR(block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        block()
    } else null
}

fun <T> atLeastVersionQ(block: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        block()
    } else null
}
