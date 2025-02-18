package com.example.namazvakti

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: NamazViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countdownRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val txtCityName = findViewById<TextView>(R.id.txtCityName)
        val txtCountdown = findViewById<TextView>(R.id.txtCountdown)
        val txtNamazVakitleri = findViewById<TextView>(R.id.txtNamazVakitleri)
        val btnChangeCity = findViewById<Button>(R.id.btnChangeCity)

        sharedPreferences = getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)
        val selectedCityConventer = sharedPreferences.getString("selected_city_conventer", "İstanbul") ?: "İstanbul"
        val selectedCity=sharedPreferences.getString("selected_city", "İstanbul") ?: "İstanbul"

        // Set city name
        txtCityName.text = selectedCity

        // Verileri yükle
        viewModel.loadNamazVakitleri(selectedCityConventer)

        // Countdown timer runnable
        countdownRunnable = object : Runnable {
            override fun run() {
                val vakitler = viewModel.namazVakitleri.value
                if (vakitler != null && vakitler.isNotEmpty()) {
                    updateCountdownText(txtCountdown, vakitler)
                }
                handler.postDelayed(this, 1000) // Update every second
            }
        }

        // Namaz vakitlerini gözlemleme
        viewModel.namazVakitleri.observe(this, Observer { vakitler ->
            vakitler?.let {
                // Update prayer times
                val text = buildString {
                    it.forEach { namaz ->
                        append("${namaz.vakit}: ${namaz.saat}\n")
                    }
                }
                txtNamazVakitleri.text = text

                // Start countdown
                updateCountdownText(txtCountdown, vakitler)
                handler.post(countdownRunnable)

                // Update widget
                updateWidget(selectedCity, vakitler)
            }
        })

        // Hata mesajlarını gözlemleme
        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, "Hata: $it", Toast.LENGTH_LONG).show()
            }
        })

        // Şehir değiştirme butonuna tıklanıldığında şehir seçim ekranına geçiş yapma
        btnChangeCity.setOnClickListener {
            val intent = Intent(this, CitySelectionActivity::class.java)
            startActivityForResult(intent, CITY_SELECTION_REQUEST_CODE)
        }
    }

    private fun updateCountdownText(
        txtCountdown: TextView,
        vakitler: List<NamazVakti>
    ) {
        val currentTime = Calendar.getInstance()

        // Next prayer and time remaining
        val (nextPrayer, timeRemaining) = findNextPrayerAndTimeRemaining(currentTime, vakitler)

        val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60

        // Update countdown
        txtCountdown.text = "$nextPrayer vaktine kalan süre: " +
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun findNextPrayerAndTimeRemaining(currentTime: Calendar, vakitler: List<NamazVakti>): Pair<String, Long> {
        var nextPrayer = ""
        var minTimeDiff = Long.MAX_VALUE

        for (namaz in vakitler) {
            val prayerTimeParts = namaz.saat.split(":")
            val prayerHour = prayerTimeParts[0].toInt()
            val prayerMinute = prayerTimeParts[1].toInt()

            val prayerTime = Calendar.getInstance()
            prayerTime.set(Calendar.HOUR_OF_DAY, prayerHour)
            prayerTime.set(Calendar.MINUTE, prayerMinute)
            prayerTime.set(Calendar.SECOND, 0)

            // If prayer time is earlier today, add 24 hours
            if (prayerTime.before(currentTime)) {
                prayerTime.add(Calendar.DAY_OF_MONTH, 1)
            }

            val timeDiff = prayerTime.timeInMillis - currentTime.timeInMillis

            if (timeDiff < minTimeDiff) {
                minTimeDiff = timeDiff
                nextPrayer = namaz.vakit
            }
        }

        return Pair(nextPrayer, minTimeDiff)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(countdownRunnable)
    }

    override fun onResume() {
        super.onResume()
        handler.post(countdownRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CITY_SELECTION_REQUEST_CODE && resultCode == RESULT_OK) {
            val newCity = data?.getStringExtra("selected_city_conventer") ?: "İstanbul"

            // Yeni şehri SharedPreferences'a kaydet
            sharedPreferences.edit().putString("selected_city_conventer", newCity).apply()

            // Update city name text
            findViewById<TextView>(R.id.txtCityName).text = newCity

            // ViewModel'i güncelle
            viewModel.loadNamazVakitleri(newCity)
        }
    }

    private fun updateWidget(city: String, vakitler: List<NamazVakti>) {
        val sharedPreferences = getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)

        // Find next prayer and time remaining
        val currentTime = Calendar.getInstance()
        val (nextPrayer, timeRemaining) = findNextPrayerAndTimeRemaining(currentTime, vakitler)

        val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60

        // Şehir ve namaz vakitlerini kaydediyoruz
        with(sharedPreferences.edit()) {
            putString("selected_city", city)

            val vakitString = buildString {
                append("🕌 $city\n")
                append("$nextPrayer vaktine: ")
                append(String.format("%02d:%02d kaldı\n", hours, minutes))
                vakitler.forEach { namaz ->
                    append("${namaz.vakit}: ${namaz.saat}\n")
                }
            }
            putString("NamazVakitleri", vakitString)
            apply()
        }
        // Widget için sadeleştirilmiş format
        val widgetString = vakitler.joinToString(separator = "\n") { namaz ->
            "${namaz.vakit}: ${namaz.saat}"
        }
        with(sharedPreferences.edit()) {
            putString("WidgetVakitleri", widgetString)
            apply()
        }
        with(sharedPreferences.edit()) {
            val sehir = buildString {
                append("$city")
            }
            putString("sehir", sehir)
            apply()
        }

        // Widget'ı güncellemek için broadcast gönder
        val intent = Intent(this, NamazWidgetProvider::class.java)
        intent.action = "com.example.namazvakti.WIDGET_UPDATE"
        sendBroadcast(intent)
    }

    companion object {
        private const val CITY_SELECTION_REQUEST_CODE = 1
    }

}