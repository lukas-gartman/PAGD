package com.example.pagdapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.pagdapp.R
import com.example.pagdapp.data.remote.locationClient.ILocationClient
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.repository.IPAGDRepository
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.ui.MainActivity
import com.example.pagdapp.utils.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var locationClient: ILocationClient

    @Inject
    lateinit var sharedRepository: SharedRepository

    @Inject
    lateinit var pagdRepo: IPAGDRepository

    private var location: Location? = null

    override fun onBind(intent: Intent?): IBinder? = null


    override fun onCreate() {
        val options = TfLiteInitializationOptions.builder()
            .setEnableGpuDelegateSupport(true)
            .build()
        TfLiteVision.initialize(this, options)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!sharedRepository.isRunning.value!!) {
                    startForegroundService()
                }
            }
            ACTION_STOP -> {
                if (sharedRepository.isRunning.value == true) {
                    stopService()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedRepository.setIsRunning(false)
        serviceScope.cancel()
        sharedRepository.activeClassifier.value!!.stopRecording()


    }

    private fun stopService() {
        sharedRepository.setIsRunning(false)
        sharedRepository.activeClassifier.value!!.stopRecording()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        createNotificationChannelIfNeeded(notificationManager)
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification.build())
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun startForegroundService() {
        sharedRepository.setIsRunning(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        createNotificationChannelIfNeeded(notificationManager)
        val notification = buildNotification()
        startLocationUpdates(notificationManager, notification)
        startAudioClassification()
        startForeground(NOTIFICATION_ID, notification.build())
        serviceScope.launch {
            sendReport()
        }
    }


    private fun startLocationUpdates(
        notificationManager: NotificationManager,
        notification: NotificationCompat.Builder
    ) {
        val updateNotification =
            notification.setContentText("Tracking your location and using the microphone")
        notificationManager.notify(NOTIFICATION_ID, updateNotification.build())
    }

    private fun startAudioClassification() {
        sharedRepository.activeClassifier.value!!.startRecording()
    }


    private suspend fun sendReport() {
        serviceScope.launch mainLaunch@{
            sharedRepository.getActiveClassifierResultFlow().collect { result ->
                serviceScope.launch {
                    val location = withContext(Dispatchers.IO) {
                        locationClient.getLocation(3)
                    } ?: return@launch

                    val report = Report(
                        result.timestamp,
                        location.latitude.toFloat(),
                        location.longitude.toFloat(),
                        location.altitude.toFloat(),
                        gun = if (result.category != "Na") {
                            result.category
                        } else {
                            "AR-15"
                        }
                    )
                    if (sharedRepository.isSendingReports.value!!) {
                        val apiResult = pagdRepo.addReport(report)
                        // Create a notification that a report has been sent
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        createNotificationChannelIfNeeded(notificationManager)
                        val reportNotification = buildReportNotification(result.score, result.timestamp)
                        notificationManager.notify(NEW_NOTIFICATION_ID, reportNotification.build())

                        apiResult.collect {
                            Log.e(
                                "sendReport",
                                "Code: ${it.code}, message: ${it.message}, ${it.data}"
                            )
                        }
                        Log.e("sendReport", "Sent a report to server:$report")
                    } else {
                        Log.e("sendReport", "NOT sending:$report")
                    }
                }
            }
        }
    }

    private fun getStopServicePendingIntent(): PendingIntent {
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this,
            1,
            stopIntent,
            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getStartServicePendingIntent(): PendingIntent {
        val startIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_START
        }
        return PendingIntent.getService(
            this,
            2,
            startIntent,
            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
    NOTIFICATION OPTIONS
     */

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_MAP_FRAGMENT
        },
        FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


    private fun createNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                notificationManager,
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME
            )
            createNotificationChannel(
                notificationManager,
                NEW_NOTIFICATION_CHANNEL_ID,
                NEW_NOTIFICATION_CHANNEL_NAME
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }

/*
    private fun createNotificationChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }

 */


    private fun buildNotification(): NotificationCompat.Builder {
        val pendingIntent = if (sharedRepository.isRunning.value == true) {
            getStopServicePendingIntent()
        } else {
            getStartServicePendingIntent()
        }

        val actionText =
            if (sharedRepository.isRunning.value == true) "Stop tracking" else "Start tracking"
        val actionIcon =
            if (sharedRepository.isRunning.value == true) R.drawable.ic_close else R.drawable.ic_ok

        val contextText = if (sharedRepository.isRunning.value == true)
            "Tracking your location and using the microphone" else "Stopped tracking"

        val isOngoing = sharedRepository.isRunning.value == true

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(isOngoing) // Set ongoing based on the service's running state
            .setContentTitle("PAGD App")
            .setContentText(contextText)
            .setSmallIcon(R.drawable.pagd_icon_green_final)
            .addAction(actionIcon, actionText, pendingIntent)
            .setContentIntent(getMainActivityPendingIntent())
    }

    private fun buildReportNotification(score: Float, timestamp: Long): NotificationCompat.Builder {
        val date = Date(timestamp)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(date)

        return NotificationCompat.Builder(this, NEW_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(true) // This notification should be auto-cancelled when clicked
            .setOngoing(false) // This notification is not ongoing
            .setContentTitle("PAGD App")
            .setContentText("Report sent: score $score, time: $dateString ")
            .setSmallIcon(R.drawable.pagd_icon_green_final)
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Tracking"
        const val NOTIFICATION_ID = 1


        const val NEW_NOTIFICATION_CHANNEL_ID = "new_notification_channel"
        const val NEW_NOTIFICATION_CHANNEL_NAME = "New Notification"
        const val NEW_NOTIFICATION_ID = 10
    }
}