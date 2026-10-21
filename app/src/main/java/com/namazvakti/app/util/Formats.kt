package com.namazvakti.app.util

import com.namazvakti.app.prayer.PrayerEngine
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val TR: Locale = Locale.forLanguageTag("tr-TR")
    private val clock = DateTimeFormatter.ofPattern("HH:mm", TR)
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR)
    private val shortDate = DateTimeFormatter.ofPattern("d MMMM", TR)

    fun clock(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(PrayerEngine.ZONE).format(clock)

    fun longDate(date: LocalDate): String = date.format(longDate)

    fun shortDate(date: LocalDate): String = date.format(shortDate)

    /** 3 sa 12 dk 05 sn biçimi (uygulama içi geri sayım için). */
    fun countdown(millis: Long): String {
        val total = (millis.coerceAtLeast(0L) + 999L) / 1000L
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format(TR, "%02d:%02d:%02d", h, m, s)
    }
}
