package com.namazvakti.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.namazvakti.app.databinding.ActivityMainBinding
import com.namazvakti.app.prayer.Cities
import com.namazvakti.app.prayer.Prayer
import com.namazvakti.app.prayer.PrayerCalculator
import com.namazvakti.app.prayer.PrayerEngine
import com.namazvakti.app.util.Formats
import com.namazvakti.app.util.Prefs
import com.namazvakti.app.widget.WidgetUpdater

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val handler = Handler(Looper.getMainLooper())
    private var renderedKey: String? = null

    private val ticker = object : Runnable {
        override fun run() {
            render()
            // Bir sonraki tam saniyeye hizala.
            val delay = 1000L - (System.currentTimeMillis() % 1000L)
            handler.postDelayed(this, delay)
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                prefs.notifyEnabled = false
                b.switchNotify.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        b.btnCity.setOnClickListener { showCityDialog() }
        b.btnTune.setOnClickListener { showTuneDialog() }
        b.btnExact.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            }
        }

        b.switchNotify.isChecked = prefs.notifyEnabled
        b.switchNotify.setOnCheckedChangeListener { _, checked ->
            prefs.notifyEnabled = checked
            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        b.btnExact.visibility = if (WidgetUpdater.canScheduleExact(this)) View.GONE else View.VISIBLE
        WidgetUpdater.refresh(this)
        renderedKey = null
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ------------------------------------------------------------------ çizim

    private fun render() {
        val now = System.currentTimeMillis()
        val offsets = prefs.offsets
        val snap = PrayerEngine.snapshot(prefs.city, now, offsets)

        b.btnCity.text = "${snap.city.name}  ▾"
        b.txtDate.text = Formats.longDate(snap.today)
        b.txtNextLabel.text = snap.next.prayer.untilLabel
        b.txtCountdown.text = Formats.countdown(snap.next.time - now)
        b.txtNextTime.text = "${snap.next.prayer.trName} · ${Formats.clock(snap.next.time)}" +
            if (snap.next.date.isAfter(snap.today)) "  (yarın)" else ""

        // Satırları yalnızca vakit/şehir/ayar değişince yeniden kur.
        val key = "${snap.city.name}|${snap.table.date}|${snap.next.prayer}|${offsets.joinToString(",")}"
        if (key != renderedKey) {
            renderedKey = key
            buildRows(snap.table.times, snap.next.prayer, snap.next.date == snap.table.date)
        }
    }

    private fun buildRows(times: LongArray, next: Prayer, highlightEnabled: Boolean) {
        b.rows.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (p in Prayer.values()) {
            val row = inflater.inflate(R.layout.item_prayer, b.rows, false)
            row.findViewById<TextView>(R.id.itemName).text = p.trName
            row.findViewById<TextView>(R.id.itemTime).text = Formats.clock(times[p.ordinal])
            if (highlightEnabled && p == next) {
                row.setBackgroundResource(R.drawable.row_active)
                row.findViewById<TextView>(R.id.itemName).setTextColor(getColor(R.color.gold))
                row.findViewById<TextView>(R.id.itemTime).setTextColor(getColor(R.color.gold))
            }
            b.rows.addView(row)
        }
    }

    // ---------------------------------------------------------------- diyaloglar

    private fun showCityDialog() {
        val names = Cities.all.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Şehir seç")
            .setSingleChoiceItems(names, prefs.cityIndex) { dialog, which ->
                prefs.cityIndex = which
                dialog.dismiss()
                onSettingsChanged()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    /** Resmî takvimle birebir uyum için vakit başına dakika ayarı (ör. -2, +3). */
    private fun showTuneDialog() {
        val current = prefs.offsets
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val fields = Prayer.values().map { p ->
            EditText(this).apply {
                hint = "${p.trName} (dakika)"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                setText(current[p.ordinal].toString())
            }.also { container.addView(it) }
        }

        AlertDialog.Builder(this)
            .setTitle("İnce ayar (dakika)")
            .setView(container)
            .setPositiveButton("Kaydet") { _, _ ->
                val values = IntArray(6) { i ->
                    (fields[i].text.toString().toIntOrNull() ?: 0).coerceIn(-30, 30)
                }
                prefs.offsets = values
                onSettingsChanged()
            }
            .setNeutralButton("Varsayılan") { _, _ ->
                prefs.offsets = PrayerCalculator.DEFAULT_OFFSETS.copyOf()
                onSettingsChanged()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun onSettingsChanged() {
        renderedKey = null
        WidgetUpdater.refresh(this)
        render()
    }
}
