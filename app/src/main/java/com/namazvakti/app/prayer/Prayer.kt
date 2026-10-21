package com.namazvakti.app.prayer

import java.time.LocalDate

/** Günün altı vakti. Sıra, DayTimes.times dizisindeki indeksle aynıdır. */
enum class Prayer(
    val trName: String,
    val untilLabel: String,
    val enteredText: String
) {
    IMSAK("İmsak", "İmsak vaktine kalan", "İmsak vakti girdi (sabah namazı)"),
    GUNES("Güneş", "Güneşin doğuşuna kalan", "Güneş doğdu"),
    OGLE("Öğle", "Öğle vaktine kalan", "Öğle vakti girdi"),
    IKINDI("İkindi", "İkindi vaktine kalan", "İkindi vakti girdi"),
    AKSAM("Akşam", "Akşam vaktine kalan", "Akşam vakti girdi"),
    YATSI("Yatsı", "Yatsı vaktine kalan", "Yatsı vakti girdi")
}

/** Bir günün vakitleri; times[i] = Prayer.values()[i] için epoch milisaniye. */
class DayTimes(val date: LocalDate, val times: LongArray) {
    operator fun get(p: Prayer): Long = times[p.ordinal]
}

/** Zaman çizgisindeki tek bir vakit olayı. */
data class PrayerEvent(val prayer: Prayer, val time: Long, val date: LocalDate)
