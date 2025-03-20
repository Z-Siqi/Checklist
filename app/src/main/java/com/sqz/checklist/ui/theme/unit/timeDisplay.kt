package com.sqz.checklist.ui.theme.unit

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale

fun timeDisplay(seconds: Long, short: Boolean = false): String {
    val week = seconds / 604800
    val days = (seconds % 604800) / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    val longFormat = if (short) MeasureFormat.FormatWidth.SHORT else MeasureFormat.FormatWidth.WIDE
    val measureFormat = MeasureFormat.getInstance(Locale.getDefault(), longFormat)
    val timeUnits = mutableListOf<Measure>()
    if (week > 0) timeUnits.add(Measure(week, MeasureUnit.WEEK))
    if (days > 0) timeUnits.add(Measure(days, MeasureUnit.DAY))
    if (hours > 0) timeUnits.add(Measure(hours, MeasureUnit.HOUR))
    if (minutes > 0) timeUnits.add(Measure(minutes, MeasureUnit.MINUTE))
    if (remainingSeconds > 0 || timeUnits.isEmpty()) timeUnits.add(
        Measure(
            remainingSeconds,
            MeasureUnit.SECOND
        )
    )
    val formattedTime = measureFormat.formatMeasures(*timeUnits.toTypedArray())
    return formattedTime
}
