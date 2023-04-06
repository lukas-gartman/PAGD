package com.example.pagdapp.repositories.googleRep

import com.google.android.gms.maps.model.LatLng
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleApi {

    @GET("elevation/json")
    suspend fun getElevation(
        @Query("locations") location: String,
        @Query("key") apiKey: String = GoogleServer.API_KEY
    ): Response<ElevationResponse>

}