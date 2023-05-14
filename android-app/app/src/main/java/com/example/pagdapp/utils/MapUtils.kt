package com.example.pagdapp.utils

import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.ui.fragments.MapsFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*

object MapUtils {

    fun createMarker(gunshot: GunshotData, mMap: GoogleMap) {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(gunshot.timestamp)
        val markerString =
            "${gunshot.gun}, ${gunshot.coordLat.toFloat()}, ${gunshot.coordLong.toFloat()}," +
                    " ${gunshot.coordAlt}, shots fired:${gunshot.shotsFired.toFloat()}"
        val markerOptions = MarkerOptions()
            .position(LatLng(gunshot.coordLat, gunshot.coordLong))
            .title(markerString)
            .snippet(date)

        mMap.addMarker(markerOptions)
    }

    fun moveCameraToLocation(mMap: GoogleMap, latLng: LatLng, zoomLevel: Float) {
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    latLng.latitude,
                    latLng.longitude
                ), zoomLevel
            )
        )
    }

    fun animateCameraToLocation(mMap: GoogleMap, latLng: LatLng, zoomLevel: Float, duration: Int) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        mMap.animateCamera(cameraUpdate, duration, object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                // Code to be executed when the animation finishes
            }

            override fun onCancel() {
                // Code to be executed when the animation is canceled
            }
        })
    }
}