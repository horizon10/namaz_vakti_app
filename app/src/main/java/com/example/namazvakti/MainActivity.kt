package com.example.namazvakti

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer

class MainActivity : ComponentActivity() {

    private val viewModel: NamazViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.txtNamazVakitleri)
        val btnChangeCity = findViewById<Button>(R.id.btnChangeCity)

        sharedPreferences = getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)
        val selectedCity = sharedPreferences.getString("selected_city", "İstanbul") ?: "İstanbul"

        // Verileri yükle
        viewModel.loadNamazVakitleri(selectedCity)

        // Namaz vakitlerini gözlemleme
        viewModel.namazVakitleri.observe(this, Observer { vakitler ->
            vakitler?.let {
                val text = buildString {
                    append("🕌 Namaz Vakitleri - $selectedCity\n")
                    it.forEach { namaz ->
                        append("${namaz.vakit}: ${namaz.saat}\n")
                    }
                }
                textView.text = text
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CITY_SELECTION_REQUEST_CODE && resultCode == RESULT_OK) {
            val newCity = data?.getStringExtra("selected_city") ?: "İstanbul"

            // Yeni şehri SharedPreferences'a kaydet
            sharedPreferences.edit().putString("selected_city", newCity).apply()

            // ViewModel'i güncelle
            viewModel.loadNamazVakitleri(newCity)
        }
    }

    private fun updateWidget(city: String, vakitler: List<NamazVakti>) {
        val sharedPreferences = getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)

        // Şehir ve namaz vakitlerini kaydediyoruz
        with(sharedPreferences.edit()) {
            putString("selected_city", city)
            val vakitString = buildString {
                append("🕌 Namaz Vakitleri - $city\n")
                vakitler.forEach { namaz ->
                    append("${namaz.vakit}: ${namaz.saat}\n")
                }
            }
            putString("NamazVakitleri", vakitString)
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