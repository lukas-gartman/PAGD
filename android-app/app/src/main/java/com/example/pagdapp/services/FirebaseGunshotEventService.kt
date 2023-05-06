package com.example.pagdapp.services

import android.util.Log
import com.example.pagdapp.data.model.GunshotData
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class FirebaseGunshotEventService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FirebaseGunshotEvent", "Gunshot detected: ${remoteMessage.data}")
            val gson = Gson()
            val json = gson.toJson(remoteMessage.data)
            val gunshotData = gson.fromJson(json, GunshotData::class.java)
            // TODO: forward the gunshot data
        }

        // TODO: if we need this, implement notification handling. Otherwise remove.
        remoteMessage.notification?.let {
            Log.d("FirebaseGunshotEvent", "Gunshot detected: ${it.body}")
        }
    }
}