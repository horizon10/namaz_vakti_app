package com.namazvakti.app.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Hesaplamayı önbellekleyen ve "şu an hangi vakitteyiz / sıradaki ne" sorusunu cevaplayan katman. */
object PrayerEngine {

    val ZONE: ZoneId = ZoneId.of("Europe/Istanbul")

    private val cache = HashMap<String, DayTimes>()

    class Snapshot(
        val city: City,
        val nowMillis: Long,
        val today: LocalDate,
        /** Az önce giren vakit. */
        val previous: PrayerEvent,
        /** Sıradaki vakit. */
        val next: PrayerEvent,
        /** Ekranda gösterilecek günün tablosu (yatsıdan sonra yarının tablosu). */
        val table: DayTimes
    ) {
        val tableIsTomorrow: Boolean get() = table.date.isAfter(today)
    }

    fun dayTimes(city: City, date: LocalDate, offsets: IntArray): DayTimes {
        val key = "${city.name}|$date|${offsets.joinToString(",")}"
        synchronized(cache) {
            cache[key]?.let { return it }
            if (cache.size > 40) cache.clear()
            val result = PrayerCalculator.calculate(date, city.lat, city.lon, ZONE, offsets)
            cache[key] = result
            return result
        }
    }

    fun snapshot(city: City, nowMillis: Long, offsets: IntArray): Snapshot {
        val today = Instant.ofEpochMilli(nowMillis).atZone(ZONE).toLocalDate()

        val events = ArrayList<PrayerEvent>(18)
        for (d in -1L..1L) {
            val day = dayTimes(city, today.plusDays(d), offsets)
            for (p in Prayer.values()) events.add(PrayerEvent(p, day[p], day.date))
        }
        events.sortBy { it.time }

        val nextIdx = events.indexOfFirst { it.time > nowMillis }.let { if (it < 1) events.size - 1 else it }
        val next = events[nextIdx]
        val previous = events[nextIdx - 1]
        val table = dayTimes(city, next.date, offsets)

        return Snapshot(city, nowMillis, today, previous, next, table)
    }
}
