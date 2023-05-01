package com.example.pagdapp.data.repository

import com.google.android.gms.maps.model.LatLng

interface IGoogleRepository {

    suspend fun getElevation(latLng: LatLng): String
}