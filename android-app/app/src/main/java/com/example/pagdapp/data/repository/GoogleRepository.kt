package com.example.pagdapp.data.repository

import android.util.Log
import com.example.pagdapp.data.remote.api.IGoogleApi
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import javax.inject.Inject

class GoogleRepository @Inject constructor(private val googleApi: IGoogleApi) : IGoogleRepository {


    override suspend fun getElevation(latLng: LatLng): String {
        return try {
            val result = googleApi.getElevation(toLocationString(latLng))
            if (result.isSuccessful) {
                val responseBody = result.body()!!
                if (responseBody.results.isNotEmpty()) {
                    responseBody.results[0].elevation.toString()
                } else {
                    val errorMessage = responseBody.error_message
                        ?: "Failed to retrieve elevation: No elevation results found."
                    throw IOException(errorMessage)
                }
            } else {
                throw IOException("Failed to retrieve elevation: ${result.errorBody()?.string()}")
            }
        } catch (e: IOException) {
            throw IOException("Failed to retrieve elevation: ${e.message}", e)
        }
    }

    private fun toLocationString(location: LatLng): String {
        return "${location.latitude},${location.longitude}"
    }
}