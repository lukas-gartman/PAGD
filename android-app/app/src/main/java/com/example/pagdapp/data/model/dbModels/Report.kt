package com.example.pagdapp.data.model.dbModels


data class Report(

    val timestamp: Long,
    var coord_lat: Float,
    var coord_long: Float,
    val coord_alt: Float,
    val gun: String
)