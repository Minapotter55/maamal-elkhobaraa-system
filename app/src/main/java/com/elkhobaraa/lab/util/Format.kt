package com.elkhobaraa.lab.util

import com.elkhobaraa.lab.data.OrderItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LabInfo {
    const val NAME = "معمل الخبراء"
    const val NAME_EN = "Al-Khobaraa Medical Lab"
    const val TAGLINE = "معمل الخبراء… الخبرة في خدمة صحتك"
    const val ADDRESS = "أمام مستشفى الخارجة التخصصي - بجوار سنترال العديسي"
    const val WHATSAPP = "01281492333"
    const val PHONE = "01016037738"
}

object Dates {
    const val DAY = 86_400_000L

    fun startOfDay(t: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = t
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun startOfMonth(): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = startOfDay()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    fun format(t: Long): String = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale.US).format(Date(t))
    fun date(t: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(t))
}

fun money(d: Double): String = String.format(Locale.US, "%,.0f ج.م", d)

fun num(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

fun String.toAmount(): Double = trim().replace(",", ".").replace("٫", ".").toDoubleOrNull() ?: 0.0

fun rangeText(min: Double?, max: Double?, text: String): String = when {
    text.isNotBlank() -> text
    min != null && max != null -> "${num(min)} - ${num(max)}"
    min != null -> "> ${num(min)}"
    max != null -> "< ${num(max)}"
    else -> ""
}

fun OrderItem.range(): String = rangeText(normalMin, normalMax, normalText)

/** -1 = low, 1 = high, 0 = normal / not numeric. */
fun OrderItem.flag(): Int {
    val v = result.trim().replace(",", ".").toDoubleOrNull() ?: return 0
    if (normalMin != null && v < normalMin) return -1
    if (normalMax != null && v > normalMax) return 1
    return 0
}
