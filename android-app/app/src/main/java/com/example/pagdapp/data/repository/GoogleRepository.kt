package com.example.pagdapp.data.repository

import com.example.pagdapp.data.remote.api.IGoogleApi
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import javax.inject.Inject

class GoogleRepository @Inject constructor(private val googleApi: IGoogleApi) : IGoogleRepository {


    override suspend fun getElevation(latLng: LatLng): String {
        try {
            val result = googleApi.getElevation(toLocationString(latLng))
            if (result.isSuccessful) {
                return result.body()!!.results[0].elevation.toString()
            } else {
                throw IOException("Failed to retrieve elevation: ${result.errorBody()?.string()}")
            }
        } catch (e: IOException) {
            throw IOException("Failed to retrieve elevation: $e")
        }
    }

    private fun toLocationString(location: LatLng): String {
        return "${location.latitude},${location.longitude}"
    }
}