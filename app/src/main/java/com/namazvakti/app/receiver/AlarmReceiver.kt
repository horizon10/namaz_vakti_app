package com.namazvakti.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.namazvakti.app.prayer.PrayerEngine
import com.namazvakti.app.util.Notifier
import com.namazvakti.app.util.Prefs
import com.namazvakti.app.widget.WidgetUpdater

/** Vakit girdiği anda çalışır: bildirimi gösterir, widget'ı yeniler, sonraki alarmı kurar. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        val now = System.currentTimeMillis()
        val snap = PrayerEngine.snapshot(prefs.city, now, prefs.offsets)

        // Alarm gecikmiş olabilir (pil tasarrufu); 10 dakikaya kadar olanı haber ver.
        val justEntered = snap.previous
        val fresh = now - justEntered.time in 0..(10 * 60 * 1000L)
        if (prefs.notifyEnabled && fresh && prefs.lastNotifiedTime != justEntered.time) {
            prefs.lastNotifiedTime = justEntered.time
            Notifier.notifyPrayerEntered(context, snap.city.name, justEntered)
        }

        WidgetUpdater.refresh(context)
    }
}
