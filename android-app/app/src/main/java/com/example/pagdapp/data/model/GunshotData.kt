package com.example.pagdapp.data.model

import com.google.gson.annotations.SerializedName


data class GunshotData(
    @SerializedName("update")
    val isUpdate: Int,
    @SerializedName("gunshot_id")
    val gunshot_id: Int,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("coord_lat")
    val coordLat: Double,
    @SerializedName("coord_long")
    val coordLong: Double,
    @SerializedName("coord_alt")
    val coordAlt: Double,
    @SerializedName("gun")
    val gun: String,
    @SerializedName("shots_fired")
    val shotsFired: Int,
)
