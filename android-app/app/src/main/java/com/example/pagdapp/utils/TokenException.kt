package com.example.pagdapp.utils


import java.io.IOException

class TokenException(message: String = NO_TOKEN_ERROR) : IOException(message) {

    companion object {
        const val NO_TOKEN_ERROR = "No valid token"
    }
}

