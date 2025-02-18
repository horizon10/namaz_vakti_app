package com.example.namazvakti

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NamazViewModel : ViewModel() {

    private val apiService = NamazApiService.create()
    private val apiKey = "" // Buraya kendi API anahtarını ekle

    // Namaz vakitlerini bir liste olarak tutuyoruz
    private val _namazVakitleri = MutableLiveData<List<NamazVakti>?>()
    val namazVakitleri: LiveData<List<NamazVakti>?> get() = _namazVakitleri

    // Hata mesajları için bir LiveData
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Namaz vakitlerini yükleyen fonksiyon
    fun loadNamazVakitleri(city: String) {
        viewModelScope.launch {
            try {
                // API'ye istek gönderiyoruz
                val response = apiService.getNamazVakitleri(apiKey, city.lowercase()) // Küçük harfe çeviriyoruz
                if (response.success) {
                    // Eğer başarıyla yanıt geldiyse, sonuçları alıyoruz
                    _namazVakitleri.value = response.result
                } else {
                    // Eğer API isteği başarısızsa hata mesajını ayarlıyoruz
                    _errorMessage.value = "API isteği başarısız!"
                }
            } catch (e: Exception) {
                // Hata durumunda hata mesajını ayarlıyoruz
                _errorMessage.value = e.message ?: "Hata oluştu"
            }
        }
    }
}
