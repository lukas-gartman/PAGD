package com.example.pagdapp.data.remote.locationClient

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ActivityScenario.launch
import com.example.pagdapp.ui.fragments.MapsFragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class LocationClient @Inject constructor(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : ILocationClient {

    private val locationMutex = Mutex()
    private var _location: Location? = null


    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnable =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnable) {
                // TODO()
            }

            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                interval
            ).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)

                    result.locations.lastOrNull()?.let { location ->
                        launch {
                            send(location)
                            locationMutex.withLock { _location = location }
                        }

                    }

                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override suspend fun getLocation(retries: Int): Location? {
        var currentRetry = 0

        while (currentRetry < retries) {
            try {
                return suspendCancellableCoroutine { continuation ->
                    client.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        object : CancellationToken() {
                            override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                                CancellationTokenSource().token

                            override fun isCancellationRequested() = false
                        })
                        .addOnSuccessListener { location: Location? ->
                            if (continuation.isActive) {
                                Log.e("getLocation3", "Location: $location")
                                continuation.resume(location, null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Handle the failure case, if necessary.
                            continuation.resumeWith(Result.failure(exception))
                        }

                    continuation.invokeOnCancellation {
                        Log.e("getLocation", "Coroutine was canceled")
                    }
                }
            } catch (exception: Exception) {
                // Log the error or perform other actions as necessary.
                currentRetry++
            }
        }

        return null
    }

    override suspend fun getLocation(): Location? {
        return locationMutex.withLock { _location }
    }


}