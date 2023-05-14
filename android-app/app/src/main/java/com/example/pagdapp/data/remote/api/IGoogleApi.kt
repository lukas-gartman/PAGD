package com.example.pagdapp.data.remote.api

import com.example.pagdapp.data.model.ElevationResponse
import com.example.pagdapp.data.remote.retrofitServices.GoogleService
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface IGoogleApi {

    @GET("elevation/json")
    suspend fun getElevation(
        @Query("locations") location: String,
        @Query("key") apiKey: String = GoogleService.API_KEY
    ): Response<ElevationResponse>

}