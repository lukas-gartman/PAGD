package com.example.pagdapp.repositories.googleRep

import com.example.pagdapp.BuildConfig.MAPS_API_KEY
import com.google.android.gms.maps.model.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleServer {

    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"
    const val API_KEY = MAPS_API_KEY

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: GoogleApi by lazy {
        retrofit.create(GoogleApi::class.java)
    }

    fun toLocationString(location: LatLng): String {
        return "${location.latitude},${location.longitude}"
    }

}