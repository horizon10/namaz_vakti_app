package com.example.namazvakti

data class NamazApiResponse(
    val success: Boolean,
    val result: List<NamazVakti> // result artık bir liste
)

data class NamazVakti(
    val saat: String,
    val vakit: String
)
