package com.example.namazvakti

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NamazWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "com.example.namazvakti.WIDGET_UPDATE" ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, NamazWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, widgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val sharedPreferences = context.getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)
        val namazVakitleri = sharedPreferences.getString("NamazVakitleri", "Veri alınamadı")

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_namaz)
        remoteViews.setTextViewText(R.id.widgetNamazVakitleri, namazVakitleri)

        // Widget'a tıklandığında ana uygulamayı aç
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widgetNamazVakitleri, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }
}