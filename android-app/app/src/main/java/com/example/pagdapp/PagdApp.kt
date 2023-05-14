package com.example.pagdapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import dagger.hilt.android.HiltAndroidApp
import org.tensorflow.lite.task.gms.vision.TfLiteVision

@HiltAndroidApp
class PagdApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val options = TfLiteInitializationOptions.builder()
            .setEnableGpuDelegateSupport(true)
            .build()

        TfLiteVision.initialize(this, options)
    }
}