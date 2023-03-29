package com.example.pagdapp.repositories.pagdServer

import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.models.dbModels.Gunshot
import com.example.pagdapp.models.dbModels.JwtToken
import com.example.pagdapp.models.dbModels.Report
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response

import retrofit2.http.*

interface ServerApi {

    @GET("/")
    fun helloWorld(): Call<JsonObject>

    @GET("/register")
    suspend fun register(): Response<JwtToken>

    @POST("/api/guns")
    suspend fun addGunToDB(@Body gun: Gun): Response<Void>

    @GET("/api/guns")
    suspend fun getGun(@Query("name") gun_name: String?): Response <List<Gun>>

    @POST("/api/reports")
    suspend fun sendReport(@Body report: Report): Response<Void>

    @GET("/api/reports")
    suspend fun getReport(
        @Query("id") id: Int?,
        @Query("time_from") timeFrom: Int?,
        @Query("time_to") timeTo: Int?
    ): Response<List<Report>>

    @POST("/api/gunshots")
    fun sendRecord(@Body record: Gunshot): Call<Void>

    @GET("/api/gunshots")
    fun getGunshot(
        @Query("timestamp") timestamp: Int?,
        @Query("coord_lat") coord_lat: String?,
        @Query("coord_long") coord_long: String?,
        @Query("coord_lat") coord_alt: String?,
    ): Call<List<Gunshot>>

}