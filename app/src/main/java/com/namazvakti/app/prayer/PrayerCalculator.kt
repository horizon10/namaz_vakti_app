package com.namazvakti.app.prayer

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * İnternetsiz, tamamen astronomik hesaplama (PrayTimes.org yöntemi).
 *
 *  - İmsak : güneş ufkun 18° altında
 *  - Yatsı : güneş ufkun 17° altında
 *  - Güneş : doğuş (0.833° düzeltmeli)
 *  - Öğle  : güneşin meridyenden geçişi
 *  - İkindi: gölge boyu = cisim boyu + öğle gölgesi (Şafii / Diyanet)
 *  - Akşam : batış (0.833° düzeltmeli)
 *
 * Sonuca vakit başına dakika cinsinden ince ayar (ihtiyat payı) eklenir.
 */
object PrayerCalculator {

    private const val FAJR_ANGLE = 18.0
    private const val ISHA_ANGLE = 17.0
    private const val ASR_FACTOR = 1.0

    /** Sıra: İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı (dakika). */
    val DEFAULT_OFFSETS: IntArray = intArrayOf(0, -7, 5, 4, 7, 0)

    fun calculate(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        offsets: IntArray = DEFAULT_OFFSETS,
        elevationMeters: Double = 0.0
    ): DayTimes {
        val startOfDay = date.atStartOfDay(zone)
        val tzHours = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth) - lon / (15.0 * 24.0)
        val horizon = 0.833 + 0.0347 * sqrt(elevationMeters)

        // Başlangıç tahmini (saat): imsak, güneş, öğle, ikindi, akşam, yatsı
        var t = doubleArrayOf(5.0, 6.0, 12.0, 15.0, 18.0, 19.0)
        repeat(2) {
            t = doubleArrayOf(
                sunAngleTime(jd, lat, FAJR_ANGLE, t[0], ccw = true),
                sunAngleTime(jd, lat, horizon, t[1], ccw = true),
                midDay(jd, t[2]),
                asrTime(jd, lat, ASR_FACTOR, t[3]),
                sunAngleTime(jd, lat, horizon, t[4], ccw = false),
                sunAngleTime(jd, lat, ISHA_ANGLE, t[5], ccw = false)
            )
        }

        val shift = tzHours - lon / 15.0
        val millis = LongArray(6) { i ->
            val minutes = ((t[i] + shift) * 60.0).roundToLong() + offsets.getOrElse(i) { 0 }
            startOfDay.plusMinutes(minutes).toInstant().toEpochMilli()
        }
        return DayTimes(date, millis)
    }

    // ---------------------------------------------------------------- astronomi

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Dönüş: (güneş yönelimi derece, zaman denklemi saat). */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinD(g) + 0.020 * sinD(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(Math.toDegrees(atan2(cosD(e) * sinD(l), cosD(l))) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = Math.toDegrees(asin(sinD(e) * sinD(l)))
        return Pair(decl, eqt)
    }

    private fun midDay(jd: Double, time: Double): Double {
        val eqt = sunPosition(jd + time / 24.0).second
        return fixHour(12.0 - eqt)
    }

    private fun sunAngleTime(jd: Double, lat: Double, angle: Double, time: Double, ccw: Boolean): Double {
        val decl = sunPosition(jd + time / 24.0).first
        val noon = midDay(jd, time)
        val x = (-sinD(angle) - sinD(decl) * sinD(lat)) / (cosD(decl) * cosD(lat))
        val h = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0))) / 15.0
        return noon + if (ccw) -h else h
    }

    private fun asrTime(jd: Double, lat: Double, factor: Double, time: Double): Double {
        val decl = sunPosition(jd + time / 24.0).first
        val angle = -Math.toDegrees(atan(1.0 / (factor + tanD(abs(lat - decl)))))
        return sunAngleTime(jd, lat, angle, time, ccw = false)
    }

    // ------------------------------------------------------------ yardımcılar

    private fun sinD(d: Double) = sin(Math.toRadians(d))
    private fun cosD(d: Double) = cos(Math.toRadians(d))
    private fun tanD(d: Double) = tan(Math.toRadians(d))
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)
    private fun fix(a: Double, mode: Double): Double {
        val r = a - mode * floor(a / mode)
        return if (r < 0) r + mode else r
    }
}
