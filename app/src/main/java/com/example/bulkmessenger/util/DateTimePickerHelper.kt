package com.example.bulkmessenger.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

/** Chains the native date picker into the native time picker, returning the combined millis. */
fun showDateTimePicker(context: Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(picked.timeInMillis)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = now.timeInMillis - 1000
    }.show()
}
