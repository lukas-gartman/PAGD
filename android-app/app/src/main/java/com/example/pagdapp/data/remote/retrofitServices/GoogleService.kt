package com.example.pagdapp.data.remote.retrofitServices

import com.example.pagdapp.BuildConfig.MAPS_API_KEY
import com.example.pagdapp.data.remote.api.IGoogleApi
import com.google.android.gms.maps.model.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleService {

    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"
    const val API_KEY = MAPS_API_KEY

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: IGoogleApi by lazy {
        retrofit.create(IGoogleApi::class.java)
    }

    fun toLocationString(location: LatLng): String {
        return "${location.latitude},${location.longitude}"
    }

}