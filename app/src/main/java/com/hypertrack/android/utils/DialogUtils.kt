package com.hypertrack.android.utils

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import com.hypertrack.logistics.android.github.R
import java.time.LocalDate

fun createDatePickerDialog(
    context: Context,
    localDate: LocalDate? = null,
    onSelected: (LocalDate) -> Unit
): AlertDialog {
    val date = localDate ?: LocalDate.now()
    return DatePickerDialog(
        context,
        R.style.DatePickerTheme,
        { _, year, month, dayOfMonth ->
            onSelected(LocalDate.of(year, month + 1, dayOfMonth))
        }, date.year, date.monthValue - 1, date.dayOfMonth
    )
}
