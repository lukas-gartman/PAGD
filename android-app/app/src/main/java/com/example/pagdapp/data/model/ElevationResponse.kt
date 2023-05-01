package com.example.pagdapp.repositories.googleRep

data class ElevationResponse(
    val results: List<Result>,
    val status: String
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
