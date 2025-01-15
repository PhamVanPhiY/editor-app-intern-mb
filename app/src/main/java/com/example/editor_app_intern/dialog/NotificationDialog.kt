package com.example.editor_app_intern.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.editor_app_intern.R

class NotificationDialog(
    private val title: String? = null,
    private val message: String? = null,
    private val labelPositive: String? = null,
    private val onPositive: () -> Unit = {},
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(title ?: getString(R.string.notice))
                setMessage(message ?: getString(R.string.app_name))
                setPositiveButton(labelPositive ?: getString(R.string.ok)) { dialog, _ ->
                    onPositive()
                    dialog.dismiss()
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
