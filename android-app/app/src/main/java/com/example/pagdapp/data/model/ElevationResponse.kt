package com.example.pagdapp.data.model

data class ElevationResponse(
    val results: List<Result>,
    val status: String,
    val error_message: String? = null
)

data class Result(
    val elevation: Double,
    val location: Location,
    val resolution: Double
)

data class Location(
    val lat: Double,
    val lng: Double
)
