package com.example.pagdapp.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import retrofit2.Response

object NetworkCallHelper {

    suspend inline fun <T> apiCall(
        crossinline apiCall: suspend () -> Response<T>,
        noinline callBackError: suspend () -> Unit = {},
        delay: Long = 0L
    ): Flow<NetworkResult<T>> =
        flow {
            emit(NetworkResult.Loading())
            delay(delay)

            try {
                val response = apiCall()
                Log.e("ApiResponseRaw", response.raw().toString())
                if (response.isSuccessful) {
                    handleSuccessfulResponse(response)
                } else {
                    emit(NetworkResult.Error(response.code(), response.message()))
                    if(response.code() == 401) {
                        handleErrorResponse(response, callBackError)
                    }

                }
            } catch (e: Exception) {
                Log.e("ApiResponseFailure", e.toString() + " stack:" + e.stackTrace)
                emit(NetworkResult.Error(0, e.message.toString()))
            }
        }


    inline fun <T> apiCallContinuous(
        crossinline apiCall: suspend () -> Response<T>,
        delayMillis: Long
    ): Flow<NetworkResult<T>> = flow {
        while (true) {
            emit(NetworkResult.Loading())
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        emit(NetworkResult.Success(response.code(), body))
                    } ?:  emit(NetworkResult.Error(response.code(), response.message()))
                } else {
                    emit(NetworkResult.Error(response.code(), response.message()))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(0, e.message.toString()))
            }
            delay(delayMillis)
        }
    }

    suspend fun <T> FlowCollector<NetworkResult<T>>.handleSuccessfulResponse(response: Response<T>) {
        val body = response.body()
        body?.let {
            Log.e("ApiResponseSuccess", body.toString())
            emit(NetworkResult.Success(response.code(), body))
        } ?: emit(NetworkResult.Error(response.code(), response.message()))
    }

    suspend fun <T> FlowCollector<NetworkResult<T>>.handleErrorResponse(
        response: Response<T>,
        callBackError: suspend () -> Unit
    ) {
        emit(NetworkResult.Error(response.code(), response.message()))
        try {
            callBackError()
        } catch (e: Exception) {
            emit(NetworkResult.Error(response.code(), response.message()))
            Log.e("ApiCallBackFailure", "onError failed: ${e.message}")
        }
    }

    suspend inline fun <T> simpleApiCall(
        crossinline apiCall: suspend () -> Response<T>
    ): NetworkResult<T> =
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    Log.e("ApiResponseSuccess", body.toString())
                    NetworkResult.Success(response.code(), body)
                } ?: NetworkResult.Error(response.code(), response.message())
            } else {
                error("${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ApiResponseFailure", e.toString())
            NetworkResult.Error(0, e.message.toString())
        }

}