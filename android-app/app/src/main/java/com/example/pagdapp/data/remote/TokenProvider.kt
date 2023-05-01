package com.example.pagdapp.data.remote

import android.content.Context
import android.util.Log
import com.example.pagdapp.data.remote.api.IPAGDApi
import com.example.pagdapp.utils.TokenException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject


class TokenProvider @Inject constructor(
    private val context: Context,
    private val pagdApi: IPAGDApi
) {

    private var token: String? = null
    private var tokenExpiredDate: Long? = null


    init {
        val sharedPreferences = context.getSharedPreferences("PAGDApp", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", null)
        tokenExpiredDate = sharedPreferences.getLong("tokenExpiredDate", 0)
    }


    suspend fun isTokenValid() {
        if (token != null && System.currentTimeMillis() < tokenExpiredDate!!) {
            return
        }
        getToken()
    }

    fun getValidToken(): String {
        if (token != null && System.currentTimeMillis() < tokenExpiredDate!!) {
            return this.token ?: ""
        }
        return ""
    }


/*
    suspend fun getToken(): Response<Boolean> {
        return try {
            val response = pagdApi.register()
            if (response.isSuccessful) {
                Log.e("getTokenSuccess", response.message())
                val token = response.body()?.token
                writeTokenInfo(token)
                Response.success(true) // You can change the boolean value based on your use case
            } else {
                Response.error(response.code(), response.message().toResponseBody())
            }
        } catch (e: IOException) {
            Log.e("getTokenFail", e.toString())
            Response.error(0, (e.message)!!.toResponseBody())
        }
    }

 */



     suspend fun getToken() {
        try {
            val response = pagdApi.register()
            Log.e("getTokenSuccess", response.message())
            if (response.isSuccessful) {
                val token = response.body()?.token
                writeTokenInfo(token)
            } else {
                throw TokenException(response.message())
            }
        } catch (e: IOException) {
            Log.e("getToken", e.toString())
            throw TokenException("Failed to fetch token: ${e.message}")
        }

    }



    private fun writeTokenInfo(token: String?) {
        val sharedPreferences =
            context.getSharedPreferences("PAGDApp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("token", token).apply()

        val thirtyDaysInMillis: Long = 30L * 24 * 60 * 60 * 1000
        val currentTimePlusThirtyDays = System.currentTimeMillis() + thirtyDaysInMillis

        sharedPreferences.edit().putLong("tokenExpiredDate", currentTimePlusThirtyDays)
            .apply()
    }


}