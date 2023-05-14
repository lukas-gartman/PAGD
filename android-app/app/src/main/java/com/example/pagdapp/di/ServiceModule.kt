package com.example.pagdapp.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.pagdapp.R
import com.example.pagdapp.data.remote.locationClient.ILocationClient
import com.example.pagdapp.data.remote.locationClient.LocationClient
import com.example.pagdapp.services.LocationService
import com.example.pagdapp.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {


    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context,
        client: FusedLocationProviderClient
    ): ILocationClient {
        return LocationClient(context, client)
    }


    @Provides
    @Singleton
    fun provideMainActivityPendingIntent(
        @ApplicationContext app: Context
    ): PendingIntent = PendingIntent.getActivity(
        app, 0,
        Intent(app, MainActivity::class.java).also {
            it.action = LocationService.ACTION_START
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    fun provideStopGunshotServiceIntent(@ApplicationContext app: Context): PendingIntent {
        val stopIntent = Intent(app, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        return PendingIntent.getService(
            app,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun provideStartGunshotServiceIntent(@ApplicationContext app: Context): PendingIntent {
        val startIntent = Intent(app, LocationService::class.java).apply {
            action = LocationService.ACTION_START
        }
        return PendingIntent.getService(
            app,
            2,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    @Provides
    @Singleton
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(app, LocationService.GUNSHOT_NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.pagd_icon)
        .setContentTitle("PAGD")
        .setContentText("Listening for gunshots in your area")
        .setContentIntent(pendingIntent)


}