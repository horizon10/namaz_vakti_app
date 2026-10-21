package com.namazvakti.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.namazvakti.app.widget.WidgetUpdater

/** Açılış, saat değişimi, saat dilimi değişimi, uygulama güncellemesi: alarmı yeniden kur, widget'ı yenile. */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.refresh(context)
    }
}
