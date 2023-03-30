package com.example.pagdapp.models.dbModels

data class Gunshot(val timestamp: Long,
                   val coord_lat: Float,
                   val coord_long: Float,
                   val coord_alt: Float,
                   val gun: Gun)
