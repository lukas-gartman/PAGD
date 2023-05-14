package com.example.pagdapp.data.model.networkModels

import com.google.gson.annotations.SerializedName


data class GunshotNetworkModel(
    @SerializedName("gunshot_id")
    val gunshotId: Int,
    @SerializedName("report_id")
    val reportId: Int?,
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
    val shotsFired: Int
)