package com.example.pagdapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.pagdapp.R
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.remote.locationClient.ILocationClient
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.ui.MainActivity
import com.example.pagdapp.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseGunshotEventService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var locationProvider: ILocationClient

    @Inject
    lateinit var sharedRepository: SharedRepository

    private val _gunshots = mutableListOf<GunshotData>()
    val gunshots: List<GunshotData> = _gunshots


    override fun handleIntent(intent: Intent?) {
        super.handleIntent(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("onNewToken", token)
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("onMessageReceived", remoteMessage.toString())
        if (remoteMessage.data.isNotEmpty()) {
            val gson = Gson()
            val json = gson.toJson(remoteMessage.data)
            val gunshotData = gson.fromJson(json, GunshotData::class.java)

            Log.d("FirebaseGunshotEvent", "Gunshot detected: $gunshotData")

            if (Build.VERSION.SDK_INT < 28) {
                serviceScope.launch {
                    if (isLocationPermissionGranted()) {
                        startForegroundService()
                        val location = withContext(Dispatchers.IO) {
                            locationProvider.getLocation(3)
                        }
                        sendGunshotNotification(gunshotData, location)
                    } else {
                        sendGunshotNotification(gunshotData, null)
                    }
                    stopService()
                }

            }



            serviceScope.launch {
                sharedRepository.updateNotificationFlow(gunshotData)
            }

            sharedRepository.updateGunshotNotification(gunshotData)

        }

        // TODO: if we need this, implement notification handling. Otherwise remove.
        remoteMessage.notification?.let {
            Log.d("FirebaseGunshotEvent", "Gunshot detected: ${it.body}")
        }
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

        val notification = NotificationCompat.Builder(this, FIREBASE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_gunshot)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(FIREBASE_GUNSHOT_NOTIFICATION_ID, notification)
    }

    private fun createGunshotNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FIREBASE_NOTIFICATION_CHANNEL_ID,
                "Gunshot Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createLocationNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FIREBASE_LOCATION_CHANNEL_ID,
                "Location Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createLocationNotificationChannelIfNeeded(notificationManager)

        val notification = buildNotification(true)
        startForeground(FIREBASE_LOCATION_ID, notification.build())

        sharedRepository.updateListening(true)

        serviceScope.launch {
            val location = withContext(Dispatchers.IO) {
                locationProvider.getLocation(3)
            }

            Log.e("startForegroundService", "location: $location")

        }
    }

    private fun stopService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createLocationNotificationChannelIfNeeded(notificationManager)
        val notification = buildNotification(false)

        sharedRepository.updateListening(false)
        notificationManager.notify(FIREBASE_LOCATION_ID, notification.build())
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun buildNotification(serviceRunning: Boolean): NotificationCompat.Builder {

        val actionText =
            if (serviceRunning) "Sent your location" else "Start listening"
        val actionIcon =
            if (serviceRunning) R.drawable.ic_close else R.drawable.ic_ok

        val contextText = if (serviceRunning)
            "Listening for gunshots in your area" else "Stopped listening"



        return NotificationCompat.Builder(this, LocationService.GUNSHOT_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(serviceRunning) // Set ongoing based on the service's running state
            .setContentTitle("PAGD App")
            .setContentText(contextText)
            .setSmallIcon(R.drawable.pagd_icon_green_final)
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


    private fun formatDistanceInKilometers(distanceInMeters: Float): String {
        val distanceInKilometers = distanceInMeters / 1000
        return String.format("%.2f", distanceInKilometers)
    }

    private fun createIntentWithGunshotData(context: Context, gunshotData: GunshotData): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_SHOW_MAP_FRAGMENT
            putExtra(EXTRA_IS_UPDATE, gunshotData.isUpdate)
            putExtra(EXTRA_GUNSHOT_ID, gunshotData.gunshot_id)
            putExtra(EXTRA_TIMESTAMP, gunshotData.timestamp)
            putExtra(EXTRA_LATITUDE, gunshotData.coordLat)
            putExtra(EXTRA_LONGITUDE, gunshotData.coordLong)
            putExtra(EXTRA_COORD_ALT, gunshotData.coordAlt)
            putExtra(EXTRA_GUN, gunshotData.gun)
            putExtra(EXTRA_SHOTS_FIRED, gunshotData.shotsFired)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val FIREBASE_NOTIFICATION_CHANNEL_ID = "firebase_channel"
        const val NOTIFICATION_CHANNEL_NAME = "gunshot_firebase"
        const val NOTIFICATION_ID = 3
        const val FIREBASE_GUNSHOT_NOTIFICATION_ID = 4
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_COORD_ALT = "extra_coord_alt"
        const val EXTRA_IS_UPDATE = "extra_is_update"
        const val EXTRA_GUNSHOT_ID = "extra_gunshot_id"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_GUN = "extra_gun"
        const val EXTRA_SHOTS_FIRED = "extra_shots_fired"
        const val FIREBASE_LOCATION_CHANNEL_ID = "firebase_location_channel"
        const val FIREBASE_LOCATION_ID = 5
    }

}