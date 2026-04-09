package com.example.brainclean.ui.component

import java.text.DateFormat
import java.util.Date

private val thoughtTimestampFormatter: DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

fun formatThoughtTimestamp(prefix: String, timestamp: Long?): String? {
    return timestamp?.let { "$prefix ${thoughtTimestampFormatter.format(Date(it))}" }
}
