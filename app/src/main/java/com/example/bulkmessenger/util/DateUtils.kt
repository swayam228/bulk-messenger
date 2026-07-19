package com.example.bulkmessenger.util

import java.util.Calendar

/** Local-timezone midnight for "today", used to scope sent-today flags and counts. */
fun startOfTodayMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
