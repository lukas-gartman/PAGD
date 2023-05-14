package com.example.pagdapp.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.pagdapp.R
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.remote.locationClient.ILocationClient
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.ui.MainActivity
import com.example.pagdapp.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@AndroidEntryPoint
class LocationService : Service() {


    override fun onBind(p0: Intent?): IBinder? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var sharedRepository: SharedRepository

    @Inject
    lateinit var locationProvider: ILocationClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()

            }
            ACTION_STOP -> {
                stopService()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannelIfNeeded(notificationManager)

        val notification = buildNotification(true)
        startForeground(NOTIFICATION_ID, notification.build())

        sharedRepository.updateListening(true)

        serviceScope.launch {
            sharedRepository.notificationFlow.collect { message ->

                if (isLocationPermissionGranted()) {
                    val location = withContext(Dispatchers.IO) {
                        locationProvider.getLocation(3)
                    }
                    sendGunshotNotification(message, location)
                    Log.e("startForegroundService", message.toString())
                } else {
                    sendGunshotNotification(message, null)
                }


            }

        }

    }

    private fun stopService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannelIfNeeded(notificationManager)
        val notification = buildNotification(false)

        sharedRepository.updateListening(false)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }


    private fun createNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            GUNSHOT_NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }

    private fun getStopServicePendingIntent(): PendingIntent {
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getStartServicePendingIntent(): PendingIntent {
        val startIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_START
        }
        return PendingIntent.getService(
            this,
            2,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun isLocationPermissionGranted(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationPermission || coarseLocationPermission
    }


    private fun createGunshotNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GUNSHOT_NOTIFICATION_CHANNEL_ID,
                "Gunshot Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendGunshotNotification(gunshot: GunshotData, location: Location?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createGunshotNotificationChannelIfNeeded(notificationManager)

        val title = "Gunshot Detected"
        val text: String = if (location != null) {
            val distance = distanceFromGunshot(gunshot, location)
            val inKm = formatDistanceInKilometers(distance)
            "Gunshot detected $inKm km away from your location."
        } else {
            "Gunshot detected. Location permission not granted."
        }

        val pendingIntent = getMainActivityPendingIntent(gunshot)

        val notification = NotificationCompat.Builder(
            this,
            FirebaseGunshotEventService.FIREBASE_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_gunshot)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            FirebaseGunshotEventService.FIREBASE_GUNSHOT_NOTIFICATION_ID,
            notification
        )
    }


    private fun buildNotification(serviceRunning: Boolean): NotificationCompat.Builder {
        val pendingIntent = if (serviceRunning) {
            getStopServicePendingIntent()
        } else {
            getStartServicePendingIntent()
        }

        val actionText =
            if (serviceRunning) "Stop listening" else "Start listening"
        val actionIcon =
            if (serviceRunning) R.drawable.ic_close else R.drawable.ic_ok

        val contextText = if (serviceRunning)
            "Listening for gunshots in your area" else "Stopped listening"



        return NotificationCompat.Builder(this, GUNSHOT_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(serviceRunning) // Set ongoing based on the service's running state
            .setContentTitle("PAGD App")
            .setContentText(contextText)
            .setSmallIcon(R.drawable.pagd_icon_green_final)
            .addAction(actionIcon, actionText, pendingIntent)
            .setContentIntent(getMainActivityPendingIntent())
    }

    private fun distanceFromGunshot(gunshot: GunshotData, myLocation: Location): Float {
        val gunshotLocation = Location("").apply {
            latitude = gunshot.coordLat
            longitude = gunshot.coordLong
        }
        return distanceBetweenTwoLocations(myLocation, gunshotLocation)
    }

    private fun getMainActivityPendingIntent(gunshot: GunshotData) = PendingIntent.getActivity(
        this,
        0,
        createIntentWithGunshotData(this, gunshot),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createIntentWithGunshotData(context: Context, gunshotData: GunshotData): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_SHOW_MAP_FRAGMENT
            putExtra(FirebaseGunshotEventService.EXTRA_IS_UPDATE, gunshotData.isUpdate)
            putExtra(FirebaseGunshotEventService.EXTRA_GUNSHOT_ID, gunshotData.gunshot_id)
            putExtra(FirebaseGunshotEventService.EXTRA_TIMESTAMP, gunshotData.timestamp)
            putExtra(FirebaseGunshotEventService.EXTRA_LATITUDE, gunshotData.coordLat)
            putExtra(FirebaseGunshotEventService.EXTRA_LONGITUDE, gunshotData.coordLong)
            putExtra(FirebaseGunshotEventService.EXTRA_COORD_ALT, gunshotData.coordAlt)
            putExtra(FirebaseGunshotEventService.EXTRA_GUN, gunshotData.gun)
            putExtra(FirebaseGunshotEventService.EXTRA_SHOTS_FIRED, gunshotData.shotsFired)
        }
    }

    private fun distanceBetweenTwoLocations(location1: Location, location2: Location): Float {
        val results = FloatArray(3)
        Location.distanceBetween(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude,
            results
        )
        return results[0]
    }

    private fun formatDistanceInKilometers(distanceInMeters: Float): String {
        val distanceInKilometers = distanceInMeters / 1000
        return String.format("%.2f", distanceInKilometers)
    }


    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = Constants.ACTION_SHOW_MAP_FRAGMENT
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val GUNSHOT_NOTIFICATION_CHANNEL_ID = "gunshot_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Gunshot"
        const val NOTIFICATION_ID = 2
        const val GUNSHOT_NOTIFICATION_ID = 3
    }

}