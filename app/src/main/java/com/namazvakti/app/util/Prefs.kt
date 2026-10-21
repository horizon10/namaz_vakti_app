package com.namazvakti.app.util

import android.content.Context
import com.namazvakti.app.prayer.Cities
import com.namazvakti.app.prayer.City
import com.namazvakti.app.prayer.PrayerCalculator

class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("namaz_vakti", Context.MODE_PRIVATE)

    var cityIndex: Int
        get() = sp.getInt(KEY_CITY, Cities.defaultIndex).coerceIn(0, Cities.all.lastIndex)
        set(value) = sp.edit().putInt(KEY_CITY, value).apply()

    val city: City get() = Cities.all[cityIndex]

    var notifyEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIFY, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIFY, value).apply()

    /** Vakit başına dakika ayarı: İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı. */
    var offsets: IntArray
        get() {
            val parts = sp.getString(KEY_OFFSETS, null)?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            return if (parts != null && parts.size == 6) parts.toIntArray() else PrayerCalculator.DEFAULT_OFFSETS.copyOf()
        }
        set(value) = sp.edit().putString(KEY_OFFSETS, value.joinToString(",")).apply()

    /** Aynı vakit için bildirimin iki kez gösterilmemesi için. */
    var lastNotifiedTime: Long
        get() = sp.getLong(KEY_LAST_NOTIFIED, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_NOTIFIED, value).apply()

    private companion object {
        const val KEY_CITY = "city_index"
        const val KEY_NOTIFY = "notify"
        const val KEY_OFFSETS = "offsets"
        const val KEY_LAST_NOTIFIED = "last_notified"
    }
}
