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
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.remote.locationClient.ILocationClient
import com.example.pagdapp.data.repository.IPAGDRepository
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.ui.MainActivity
import com.example.pagdapp.utils.Constants
import com.example.pagdapp.utils.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@AndroidEntryPoint
class GunshotService : Service() {


    override fun onBind(p0: Intent?): IBinder? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var pagdRepo: IPAGDRepository

    @Inject
    lateinit var sharedRepository: SharedRepository

    @Inject
    lateinit var locationProvider: ILocationClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder


    override fun onCreate() {
        super.onCreate()

    }


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
            checkForGunshots(sharedRepository.intervalDelay.value!!)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun checkForGunshots(interval : Long) {
        while (serviceScope.isActive) {
            val timeIntervalSecondsAgo = System.currentTimeMillis() - interval
            val timeNow = System.currentTimeMillis()
            Log.e("checkForGunshots", sharedRepository.intervalDelay.value!!.toString() )
            pagdRepo.getGunshot(null, timeIntervalSecondsAgo, timeNow)
                .catch { e -> e.printStackTrace() }
                .flatMapLatest { gunshots ->
                    getLocationUpdatesWithPermissionCheck(interval).map { location ->
                        gunshots to location
                    }
                }
                .collect { (gunshots, location) ->
                    when (gunshots) {
                        is NetworkResult.Success -> {
                            if (gunshots.data?.isNotEmpty() == true) {
                                sendGunshotNotification(gunshots.data, location)
                            }
                        }
                        else -> {}
                    }
                    Log.e(
                        "SERVICE LOGS",
                        "location: " + location.toString() + " guns:" + gunshots.data.toString()
                    )
                }

            delay(interval)
        }
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
        val stopIntent = Intent(this, GunshotService::class.java).apply {
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
        val startIntent = Intent(this, GunshotService::class.java).apply {
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

    private fun getLocationUpdatesWithPermissionCheck(delay: Long): Flow<Location?> {
        return if (isLocationPermissionGranted()) {
            locationProvider.getLocationUpdates(delay)
        } else {
            flowOf(null)
        }
    }

    private fun sendGunshotNotification(gunshots: List<Gunshot>, location: Location?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createGunshotNotificationChannelIfNeeded(notificationManager)

        val title = "Gunshot Detected"
        val text: String = if (location != null) {
            val distance = returnClosestGunshot(location, gunshots).toString()
            "Gunshot detected $distance meters away from your location."
        } else {
            "Gunshot detected. Location permission not granted."
        }

        val notification = NotificationCompat.Builder(this, GUNSHOT_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_gunshot)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(GUNSHOT_NOTIFICATION_ID, notification)
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

    private fun returnClosestGunshot(myLocation: Location, gunshots: List<Gunshot>): Float? {
        val distances = mutableListOf<Float>()
        for (gunshot in gunshots) {
            val gunshotLocation = Location("").apply {
                latitude = gunshot.coordLat.toDouble()
                longitude = gunshot.coordLong.toDouble()
            }
            distances.add(distanceBetweenTwoLocations(myLocation, gunshotLocation))
        }
        return distances.minOrNull()
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



        return NotificationCompat.Builder(this, TrackingService.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(serviceRunning) // Set ongoing based on the service's running state
            .setContentTitle("PAGD App")
            .setContentText(contextText)
            .setSmallIcon(R.drawable.pagd_icon_green_final)
            .addAction(actionIcon, actionText, pendingIntent)
            .setContentIntent(getMainActivityPendingIntent())
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

    /*
    @SuppressLint("RestrictedApi")
    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannelIfNeeded(notificationManager)
        baseNotificationBuilder.mActions.clear()
        val notification = baseNotificationBuilder
            .addAction(R.drawable.ic_close, "Stop listening", getStopServicePendingIntent()).build()
        startForeground(NOTIFICATION_ID, notification)


        serviceScope.launch {
            checkForGunshots(Constants.CONTINUOUS_NETWORK_CALL_MIDDLE)
        }

    }


    @SuppressLint("RestrictedApi")
    private fun stopService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannelIfNeeded(notificationManager)
        baseNotificationBuilder.mActions.clear()
        baseNotificationBuilder.addAction(R.drawable.ic_ok, "Start listening", getStartServicePendingIntent())

        val notification = baseNotificationBuilder
            .setOngoing(false) // Set the notification to not be ongoing so it can be dismissed
            .setSmallIcon(R.drawable.pagd_icon) // Set the notification's small icon
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

 */

}