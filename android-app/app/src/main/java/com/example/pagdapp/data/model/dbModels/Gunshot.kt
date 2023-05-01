package com.example.pagdapp.data.model.dbModels

import com.google.gson.annotations.SerializedName

data class Gunshot(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("coord_lat")
    val coordLat: Float,
    @SerializedName("coord_long")
    val coordLong: Float,
    @SerializedName("coord_alt")
    val coordAlt: Float,
    @SerializedName("gun")
    val gun: String,
    @SerializedName("shots_fired")
    val shotsFired: Int // Add this field if you want to send it in the request
)