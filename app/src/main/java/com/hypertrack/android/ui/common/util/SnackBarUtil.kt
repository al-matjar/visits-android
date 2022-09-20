package com.hypertrack.android.ui.common.util

import android.view.View
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import com.google.android.material.snackbar.Snackbar
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.format
import com.hypertrack.logistics.android.github.R


object SnackBarUtil {

    fun showErrorSnackBar(view: View, errorTextConsumable: Consumable<ErrorMessage>) {
        errorTextConsumable.consume {
            showErrorSnackBar(view, it)
        }
    }

    fun showErrorSnackBar(view: View, errorMessage: ErrorMessage) {
        val text = errorMessage.text
        val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(MyApplication.context.getString(R.string.close)) {
                dismiss()
            }
            setActionTextColor(MyApplication.context.getColor(R.color.colorRed))
        }
        try {
            val snackbarView = snackbar.view
            snackbarView.setOnClickListener {
                ClipboardUtil.copyToClipboard(
                    errorMessage.originalException?.format() ?: text
                )
            }
            val snackTextView = snackbarView.findViewById<View>(
                com.google.android.material.R.id.snackbar_text
            ) as TextView
            snackTextView.maxLines = 20
        } catch (_: Exception) {
            // ignore as not important
        }
        snackbar.show()
    }

    // use showErrorSnackBar for errors
    fun showSnackBar(view: View, text: String) {
        val snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(MyApplication.context.getString(R.string.close)) {
                dismiss()
            }
            setActionTextColor(MyApplication.context.getColor(R.color.colorHyperTrackGreen))
        }
        try {
            val snackbarView = snackbar.view
            val snackTextView = snackbarView.findViewById<View>(
                com.google.android.material.R.id.snackbar_text
            ) as TextView
            snackTextView.maxLines = 20
        } catch (_: Exception) {
            // ignore as not important
        }
        snackbar.show()
    }

}
