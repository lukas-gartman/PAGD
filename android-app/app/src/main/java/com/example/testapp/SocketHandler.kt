package com.example.testapp


import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.SocketOptionBuilder
import java.net.URISyntaxException

object SocketHandler {

    lateinit var mSocket: Socket
    private const val baseUrl = "http://10.0.2.2:8080"
    private const val physicalUrl = "https://pagdserver.onrender.com"

    @Synchronized
    fun setSocket() {
        try {
        // "http://10.0.2.2:3000" is the network your Android emulator must use to join the localhost network on your computer
        // "http://localhost:3000/" will not work
        // If you want to use your physical phone you could use the your ip address plus :3000
        // This will allow your Android Emulator and physical device at your home to connect to the server
            mSocket = IO.socket(physicalUrl)
        } catch (e: URISyntaxException) {
            Log.e(TAG, "setSocket: $e" )
        }
    }

   @Synchronized
    fun getSocket(): Socket {
       if(!::mSocket.isInitialized){
           setSocket()
       }
        return  mSocket
    }

   @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

   @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }
}