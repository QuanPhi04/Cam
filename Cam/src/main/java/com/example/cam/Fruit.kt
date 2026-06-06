package com.example.cam

import android.net.Uri

data class Fruit(
    val id: String,
    val name: String,
    val imageUri: Uri? = null,
    val imageUrl: String? = null,
    val tags: Map<String, String> = emptyMap(),
    val isAiDetected: Boolean = false,
    val confidence: Float? = null,
    val scanDate: String = ""
)
