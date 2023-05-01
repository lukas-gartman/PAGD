package com.example.pagdapp.data.remote.retrofitServices

import com.example.pagdapp.data.remote.api.MockServerApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MockupService {

    private const val baseUrl = "http://10.0.2.2:8080"
    private const val physicalUrl = "https://pagdserver.onrender.com"

    fun getInstance(): MockServerApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().
            create(MockServerApi::class.java)
    }
}