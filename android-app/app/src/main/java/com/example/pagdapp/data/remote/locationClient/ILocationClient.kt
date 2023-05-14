package com.example.pagdapp.data.remote.locationClient

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface ILocationClient {

    fun getLocationUpdates (interval: Long): Flow<Location>

    suspend fun getLocation(retries: Int): Location?
    suspend fun getLocation(): Location?

    class LocationException(message: String): Exception()
}