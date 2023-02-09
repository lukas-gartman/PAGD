package com.example.testapp

import android.location.Location
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface ServerApi {


    @POST("/api/positions")
    fun sendPosition(@Body position: List<Position>) : Call<Void>

    @GET("/api/positions")
    fun getPosition(): Call<List<Position>>

    @DELETE("/api/positions")
    fun deletePositions() : Call<String>
}