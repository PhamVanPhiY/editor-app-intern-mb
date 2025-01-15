package com.example.editor_app_intern.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.editor_app_intern.R

open class OptionDialog(
    private val title: String? = null,
    private val message: String? = null,
    private val labelNegative: String? = null,
    private val labelPositive: String? = null,
    private val onPositive: () -> Unit = {},
    private val onNegative: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle(title ?: resources.getString(R.string.notice))
                setMessage(message ?: resources.getString(R.string.app_name))
                setNegativeButton(
                    labelNegative ?: resources.getString(R.string.cancel)
                ) { dialog, _ ->
                    onNegative()
                    dialog.dismiss()
                }
                setPositiveButton(labelPositive ?: resources.getString(R.string.ok)) { dialog, _ ->
                    onPositive()
                    dialog.dismiss()
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun dismiss() {
        onDismiss()
        super.dismiss()
    }
}

class DeleteOptionsDialog(
    private val context: Context,
    private val message: String? = null,
    private val onCancelDelete: () -> Unit = {},
    private val onDelete: () -> Unit
) : OptionDialog(
    message = message ?: context.resources.getString(R.string.delete_image),
    labelNegative = context.resources.getString(R.string.cancel),
    labelPositive = context.resources.getString(R.string.delete),
    onNegative = onCancelDelete,
    onPositive = onDelete
)

class SaveOptionsDialog(
    private val context: Context,
    private val onSaveNew: () -> Unit = {},
    private val onSaveReplace: () -> Unit
) : OptionDialog(
    message = context.resources.getString(R.string.save_image),
    labelNegative = context.resources.getString(R.string.save_new),
    labelPositive = context.resources.getString(R.string.save_replace),
    onNegative = onSaveNew,
    onPositive = onSaveReplace
)

class DiscardChangeDialog(
    private val context: Context,
    private val onDiscard: () -> Unit
) : OptionDialog(
    message = context.resources.getString(R.string.discard_changes),
    onPositive = onDiscard
)
