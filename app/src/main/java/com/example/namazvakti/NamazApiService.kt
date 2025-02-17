package com.example.namazvakti

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NamazApiService {
    @GET("pray/all")
    suspend fun getNamazVakitleri(
        @Header("authorization") apiKey: String,
        @Query("data.city") city: String
    ): NamazApiResponse

    companion object {
        private const val BASE_URL = "https://api.collectapi.com/"

        fun create(): NamazApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NamazApiService::class.java)
        }
    }
}