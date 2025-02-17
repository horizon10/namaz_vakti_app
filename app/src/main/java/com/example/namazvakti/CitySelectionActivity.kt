package com.example.namazvakti

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.ComponentActivity

class CitySelectionActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_selection)

        sharedPreferences = getSharedPreferences("NamazAppPrefs", Context.MODE_PRIVATE)

        val citySpinner = findViewById<Spinner>(R.id.citySpinner)
        val btnSelectCity = findViewById<Button>(R.id.btnSelectCity)

        // Şehir listesini `strings.xml` dosyasından al
        val cities = resources.getStringArray(R.array.cities)

        // Spinner'a şehirleri bağlamak için bir adapter oluştur
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = adapter

        // Önceden seçilen şehir varsa onu göster
        val savedCity = sharedPreferences.getString("selected_city", null)
        if (savedCity != null) {
            val index = cities.indexOf(savedCity)
            if (index >= 0) {
                citySpinner.setSelection(index)
            }
        }

        btnSelectCity.setOnClickListener {
            val selectedCity = citySpinner.selectedItem.toString()

            with(sharedPreferences.edit()) {
                putString("selected_city", selectedCity)
                apply()
            }

            // Widget'ı anında güncelle
            updateWidgetImmediately()

            // Ana ekrana dön
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
    private fun updateWidgetImmediately() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, NamazWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (widgetIds.isNotEmpty()) {
            val intent = Intent(this, NamazWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
            sendBroadcast(intent)
        }
    }


}
