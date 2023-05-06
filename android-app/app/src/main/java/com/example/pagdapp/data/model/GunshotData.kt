package com.example.pagdapp.data.model

data class GunshotData(
    val isUpdate: Int,
    val gunshot_id: Int,
    val timestamp: Long,
    val coordLat: Double,
    val coordLong: Double,
    val coordAlt: Double,
    val gun: String,
    val shotsFired: Int,
)
