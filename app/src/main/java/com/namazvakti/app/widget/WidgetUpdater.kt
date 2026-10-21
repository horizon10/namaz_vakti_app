package com.namazvakti.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.namazvakti.app.MainActivity
import com.namazvakti.app.R
import com.namazvakti.app.prayer.Prayer
import com.namazvakti.app.prayer.PrayerEngine
import com.namazvakti.app.receiver.AlarmReceiver
import com.namazvakti.app.util.Formats
import com.namazvakti.app.util.Prefs

/**
 * Widget'ı çizer ve bir sonraki vakit girdiği anda kendini yeniden çizecek alarmı kurar.
 *
 * Geri sayım, RemoteViews içindeki Chronometer ile yapılır: saniye saniye akması için
 * uygulamanın uyanık olması gerekmez. Alarm yalnızca vurgulanan vakti değiştirmek ve
 * bildirim göstermek içindir.
 */
object WidgetUpdater {

    private val NAME_IDS = intArrayOf(
        R.id.wName0, R.id.wName1, R.id.wName2, R.id.wName3, R.id.wName4, R.id.wName5
    )
    private val TIME_IDS = intArrayOf(
        R.id.wTime0, R.id.wTime1, R.id.wTime2, R.id.wTime3, R.id.wTime4, R.id.wTime5
    )
    private val CELL_IDS = intArrayOf(
        R.id.wCell0, R.id.wCell1, R.id.wCell2, R.id.wCell3, R.id.wCell4, R.id.wCell5
    )

    /** Widget'ları yeniden çizer ve sonraki alarmı kurar. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, PrayerWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            manager.updateAppWidget(ids, buildViews(app))
        }
        scheduleNextAlarm(app)
    }

    private fun buildViews(context: Context): RemoteViews {
        val prefs = Prefs(context)
        val now = System.currentTimeMillis()
        val snap = PrayerEngine.snapshot(prefs.city, now, prefs.offsets)

        val rv = RemoteViews(context.packageName, R.layout.widget_prayer)

        rv.setTextViewText(
            R.id.wCity,
            "${snap.city.name} · ${snap.next.prayer.trName}" +
                if (snap.next.date.isAfter(snap.today)) " (yarın)" else ""
        )

        // Geri sayım: elapsedRealtime zaman tabanına çevrilmiş hedef an.
        val base = SystemClock.elapsedRealtime() + (snap.next.time - now)
        rv.setChronometerCountDown(R.id.wCountdown, true)
        rv.setChronometer(R.id.wCountdown, base, null, true)

        val accent = context.getColor(R.color.gold)
        val normalText = context.getColor(R.color.widget_text)
        val normalSub = context.getColor(R.color.widget_text_secondary)

        for (i in Prayer.values().indices) {
            val p = Prayer.values()[i]
            val isNext = p == snap.next.prayer && snap.table.date == snap.next.date
            rv.setTextViewText(NAME_IDS[i], p.trName)
            rv.setTextViewText(TIME_IDS[i], Formats.clock(snap.table.times[i]))
            rv.setTextColor(NAME_IDS[i], if (isNext) accent else normalSub)
            rv.setTextColor(TIME_IDS[i], if (isNext) accent else normalText)
            rv.setInt(CELL_IDS[i], "setBackgroundResource", if (isNext) R.drawable.widget_cell_active else 0)
        }

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        rv.setOnClickPendingIntent(R.id.wRoot, open)
        return rv
    }

    private fun scheduleNextAlarm(context: Context) {
        val prefs = Prefs(context)
        val now = System.currentTimeMillis()
        val next = PrayerEngine.snapshot(prefs.city, now, prefs.offsets).next

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, 1, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.time, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.time, pi)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.time, pi)
        }
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarm.canScheduleExactAlarms()
    }
}
