package com.example.testapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val baseUrl = "http://10.0.2.2:8080"
    private const val physicalUrl = "https://pagdserver.onrender.com"

    fun getInstance(): ServerApi {
        return Retrofit.Builder()
            .baseUrl(physicalUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().
            create(ServerApi::class.java)
    }
}