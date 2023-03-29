package com.example.pagdapp.repositories.pagdServer

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * This class provides an interface for making asynchronous HTTP requests to the PAGD server API.
 * It uses Retrofit and OkHttp libraries to communicate with the server, and handles the
 * authentication by adding an Authorization header to each request with a JWT token.
 *
 * To use this class, call the "getInstance()" function, which returns a ServerApi instance.
 * This instance can be used to call the different endpoints provided by the PAGD server API.
 */
object PAGDServer {

    private const val baseUrl = "http://tottes.net"

    private var retrofit: Retrofit? = null
    private var token: String? = null
    private val authInterceptor = AuthInterceptor(token)


    /**
     * This function returns a retrofit instance without the Authorization header interceptor.
     * It is used internally to create a retrofit instance before adding the interceptor.
     *
     * @return A Retrofit instance without the Authorization header interceptor.
     */
    private fun getRetrofitInstance(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    /**
     * Returns a ServerApi instance with the Authorization header interceptor added.
     * This instance can be used to make asynchronous HTTP requests to the PAGD server API.
     * The first time this function is called, it retrieves a JWT token from the server
     * by making a request to the "/register" endpoint, and creates a new Retrofit instance
     * with the Authorization header interceptor added.
     * Subsequent calls to this function reuse the same Retrofit instance to avoid unnecessary
     * network calls and improve performance.
     *
     * @return a ServerApi instance for making asynchronous HTTP requests to the PAGD server API.
     */
    suspend fun getInstance(): ServerApi {
        if (token == null) {
            token = getToken()
            token?.let { Log.e("RESPONSEFROMSERVER", it) }
            authInterceptor.token = token
        }

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getOkHttpClient(authInterceptor))
            .build()
        return retrofit!!.create(ServerApi::class.java)
    }

    /**
     * A OkHttpClient instance with the Authorization header interceptor added.
     *
     * @param authInterceptor the interceptor that adds the Authorization header to requests
     * @return a new OkHttpClient instance with the Authorization header interceptor added
     */
    private fun getOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    /**
     * Retrieves a JWT token from the PAGD server API by making a request to the "/register" endpoint.
     * This function is called the first time "getInstance()" is called to obtain a JWT token.
     * It is a suspend function because it makes a network call and should not block the UI thread.
     *
     * @return a JWT token string
     * @throws RuntimeException if the JWT token cannot be retrieved from the server
     */
    internal suspend fun getToken(): String? {
        try {
            val retrofit = getRetrofitInstance()
            val serverApi = retrofit.create(ServerApi::class.java)
            val response = serverApi.register()

            return if (response.isSuccessful) {
                Log.e("getToken", response.toString())
                response.body()!!.token
            } else {
                null
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("getToken", "Failed to retrieve token")
            return null
        }
    }

    /**
     * Interceptor used to add the JWT token to the Authorization header to each API requests.
     *
     * @param token the bearer token that will be added to the Authorization header
     * @return Interceptor
     */
    private class AuthInterceptor(var token: String?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            if (token != null) {
                val request = token?.let {
                    original.newBuilder()
                        .header("Authorization", it)
                        .method(original.method(), original.body())
                        .build()
                }

                return chain.proceed(request)
            }

            return chain.proceed(original)
        }
    }

}

