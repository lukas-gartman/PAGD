package com.example.pagdapp.data.remote.api

import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.dbModels.JwtToken
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.model.networkModels.GunNetworkModel
import com.example.pagdapp.data.model.networkModels.GunshotNetworkModel
import com.example.pagdapp.data.model.networkModels.ReportNetworkModel
import org.json.JSONObject
import retrofit2.Response

import retrofit2.http.*

interface IPAGDApi {

    @GET("/")
    suspend fun helloWorld(): Response<String>

    @GET("/register")
    suspend fun register(): Response<JwtToken>

    @POST("/api/guns")
    suspend fun addGunToDB(
        @Header("Authorization") token: String,
        @Body gun: GunNetworkModel
    ): Response<Gun>

    @GET("/api/guns")
    suspend fun getGun(
        @Header("Authorization") token: String,
        @Query("name") gunName: String?
    ): Response<List<Gun>>

    @POST("/api/reports")
    suspend fun addReport(
        @Header("Authorization") token: String,
        @Body report: Report
    ): Response<JSONObject>

    @GET("/api/reports")
    suspend fun getReport(
        @Header("Authorization") token: String,
        @Query("id") id: Int?,
        @Query("time_from") timeFrom: Long?,
        @Query("time_to") timeTo: Long?
    ): Response<List<ReportNetworkModel>>

    @POST("/api/gunshots")
    suspend fun addGunShot(
        @Header("Authorization") token: String,
        @Body gunshot: GunshotNetworkModel
    ): Response<GunshotNetworkModel>

    @GET("/api/gunshots")
    suspend fun getGunshot(
        @Header("Authorization") token: String,
        @Query("gunshot_id") gunshotId: Int?,
        @Query("time_from") timeFrom: Long,
        @Query("time_to") timeTo: Long,
    ): Response<List<Gunshot>>

    @GET("/api/gunshots/latest")
    suspend fun getLatestGunshot(@Header("Authorization") token: String): Response<Gunshot>

}