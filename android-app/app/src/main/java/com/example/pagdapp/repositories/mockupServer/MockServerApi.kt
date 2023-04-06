package com.example.pagdapp.repositories.mockupServer

import com.example.pagdapp.models.Position
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST


interface MockServerApi {

    @POST("/api/positions")
    fun sendPosition(@Body position: List<Position>) : Call<Void>

    @GET("/api/positions")
    fun getPosition(): Call<List<Position>>

    @DELETE("/api/positions")
    fun deletePositions() : Call<String>

}