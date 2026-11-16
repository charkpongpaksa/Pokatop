package com.example.pokatop.ui

import android.content.Context
import androidx.annotation.StringRes
import com.example.pokatop.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Dialogs {
    fun confirmDelete(
        context: Context,
        @StringRes title: Int,
        message: String,
        onCancel: () -> Unit = {},
        onConfirm: () -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
        if (message.isNotEmpty()) builder.setMessage(message)
        builder.show()
    }
}
