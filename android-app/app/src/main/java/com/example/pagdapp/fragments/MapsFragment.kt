package com.example.pagdapp.fragments

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.pagdapp.R
import com.example.pagdapp.databinding.FragmentMapsBinding
import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.models.dbModels.Report
import com.example.pagdapp.repositories.googleRep.GoogleApi
import com.example.pagdapp.repositories.googleRep.GoogleServer
import com.example.pagdapp.repositories.pagdServer.ServerApi
import com.example.pagdapp.viewModels.MainViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log


class MapsFragment : Fragment(), ReportDialog.ReportDialogListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val _userLocation = MutableLiveData<Location>()
    private var userLocation: LiveData<Location>? = _userLocation


    private val sharedMainViewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentMapsBinding


    companion object {
        fun newInstance() = MapsFragment()
        const val LOCATION_UPDATE_INTERVAL = 3000L
        const val FASTEST_LOCATION_INTERVAL = 2000F
        const val DEFAULT_ZOOM = 15
        val weaponTypes = mutableListOf(
            Gun("AK-47", "Assault Rifle"),
            Gun("Sig9", "Handgun")
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fusedLocationClient =
            activity?.let {
                LocationServices.getFusedLocationProviderClient(it.applicationContext)
            }!!
        binding = FragmentMapsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


        displayLocationView()
        displayClassificationView()

        initButtons()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to
     * install it inside the SupportMapFragment. This method will only be triggered once the
     * user has installed Google Play services and returned to the app.
     */
    private val callback = OnMapReadyCallback { googleMap ->
        try {
            mMap = googleMap
            mMap.isMyLocationEnabled = true

            initLocationRequest()
            placeMarker()

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location ->
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM.toFloat())
                )

            }

            mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDrag(p0: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                   val updateReport = sharedMainViewModel.updateReport(marker.id,marker.position)
                    val markerString = "${updateReport?.gun}, ${updateReport?.coord_lat}, " +
                            "${updateReport?.coord_long}, ${updateReport?.coord_alt}"
                    marker.showInfoWindow()
                    marker.title = markerString
                }

                override fun onMarkerDragStart(marker: Marker) {
                    marker.hideInfoWindow()
                }
            })

        } catch (s: SecurityException) {
            s.printStackTrace()
        }

    }

    @SuppressLint("MissingPermission")
    private fun initLocationRequest() {
        // TODO Check permissions?
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L
        )
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.let { locations ->
                for (location in locations) {
                    _userLocation.postValue(location)
                    sharedMainViewModel.updateUserLocation(location)
                }
            }
        }
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Set the map's camera position to the current location of the device.
                    val userLocation = task.result
                    if (userLocation != null) {

                        _userLocation.postValue(task.result)
                        val lat = "lat: " + userLocation.latitude
                        val long = ", lng: " + userLocation.longitude
                        val alt = ", alt: " + userLocation.altitude
                        val locationString = lat.plus(long).plus(alt)


                        sharedMainViewModel.updateUserLocation(userLocation)
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    userLocation.latitude,
                                    userLocation.longitude
                                ), DEFAULT_ZOOM.toFloat()
                            )
                        )
                    }
                }
            }

        } catch (s: SecurityException) {
            s.printStackTrace()
        }
    }

    private fun displayLocationView(){
        userLocation?.observe(viewLifecycleOwner) { location ->
            activity?.runOnUiThread {
                val timeMillis = location.time
                val date = Date(timeMillis)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateString = dateFormat.format(date)
                val locationString = "lat: ${location.latitude}," +
                        "\nlng: ${location.longitude}," +
                        "\nalt: ${location.altitude}" +
                        "\ntime: $dateString"
                binding.tvLocation.text = locationString
            }
        }
    }

    private fun displayClassificationView(){
        sharedMainViewModel.classificationResult.observe(viewLifecycleOwner) { result ->
            activity?.runOnUiThread {
                val resultString =
                    "Category: ${result.category}\nType: ${result.specificType}\nScore: ${result.score}"
                binding.tvResult.text = resultString
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun placeMarker() {

        // Add click listener to the map
        mMap.isMyLocationEnabled = true

        mMap.setOnMapClickListener { latLng ->
            setupDialog(latLng)
        }

        sharedMainViewModel.manualReports.observe(viewLifecycleOwner) { reports ->
            if(reports.isNotEmpty()) {
                binding.fabAddReport.show()
                binding.fabRemoveMarkers.show()
            }
        }


    }

    private suspend fun fetchElevation(location: LatLng): String {
        try {
            val response = withContext(Dispatchers.IO) {
                GoogleServer.service.getElevation(GoogleServer.toLocationString(location))
            }
            if (response.isSuccessful && response.body() != null) {

                Log.e("GoogleApiCall", response.body().toString())
                return response.body()!!.results[0].elevation.toString()
            } else {
                throw Exception("Failed to fetch elevation: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("GoogleApiCall", "Failed to fetch elevation", e)
            throw e
        }
    }

    private fun setupDialog(latLng: LatLng) {

        var reportDialog: ReportDialog
        val weapons = mutableListOf<String>()

        // TODO when server comes back online, use guns
        sharedMainViewModel.guns.observe(viewLifecycleOwner) { guns ->
            for (weapon in weaponTypes) {
                weapons.add(weapon.name)
            }
        }

        lifecycleScope.launch {
            try {
                val elevation = fetchElevation(latLng)
                reportDialog = ReportDialog(weapons, latLng, elevation, this@MapsFragment)
                reportDialog.show(parentFragmentManager, "ReportDialog")
            } catch (e: Exception) {
                // Handle the exception
            }
        }

    }

    private fun initButtons() {
        binding.fabAddReport.isVisible = false
        binding.fabRemoveMarkers.isVisible = false

        binding.fabRemoveMarkers.setOnClickListener {
            binding.fabAddReport.hide()
            binding.fabRemoveMarkers.hide()
            mMap.clear()
            sharedMainViewModel.clearReports()
        }
    }


    override fun onReportSelected(report: Report) {

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(report.timestamp)
        val markerString = "${report.gun}, ${report.coord_lat}, " +
                "${report.coord_long}, ${report.coord_alt}"
        val marker: Marker? = mMap.addMarker(
            MarkerOptions()
                .position(LatLng(report.coord_lat.toDouble(), report.coord_long.toDouble()))
                .title(markerString)
                .snippet(date)
                .draggable(true)
        )
        marker!!.showInfoWindow()
        sharedMainViewModel.addManualReport(marker.id,report)
    }


}