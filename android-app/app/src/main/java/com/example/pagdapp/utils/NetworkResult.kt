package com.example.pagdapp.utils

sealed class NetworkResult<T>(
    val code: Int? = null,
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(code: Int,data: T) : NetworkResult<T>(code,data)
    class Error<T>(code: Int, message: String, data: T? = null) : NetworkResult<T>(code, data, message)
    class Loading<T> : NetworkResult<T>()
}