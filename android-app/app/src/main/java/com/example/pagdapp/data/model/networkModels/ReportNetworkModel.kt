package com.example.pagdapp.data.model.networkModels

data class ReportNetworkModel(
    val report_id: Int,
    val timestamp: Long,
    val coord_lat: Float,
    val coord_long: Float,
    val coord_alt: Float,
    val gun: String
)
