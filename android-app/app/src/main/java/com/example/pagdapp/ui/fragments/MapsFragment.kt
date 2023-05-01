package com.example.pagdapp.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import com.example.pagdapp.R
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.databinding.FragmentMapsBinding
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.remote.retrofitServices.GoogleService
import com.example.pagdapp.utils.NetworkResult
import com.example.pagdapp.ui.viewModels.MainViewModel
import com.example.pagdapp.ui.viewModels.ReportViewModel
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MapsFragment : Fragment(), ReportDialog.ReportDialogListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val _userLocation = MutableLiveData<Location>()
    private var userLocation: LiveData<Location>? = _userLocation


    private val sharedMainViewModel: MainViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private val reportViewModel: ReportViewModel by activityViewModels()
    private lateinit var binding: FragmentMapsBinding


    companion object {
        fun newInstance() = MapsFragment()
        const val DEFAULT_ZOOM = 15
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
        subscribeCollectors()
    }

    private fun subscribeCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reportViewModel.gunshots.collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> ""
                        is NetworkResult.Success -> {
                            gunshotsToMarkers(result.data!!)
                        }
                        is NetworkResult.Error -> {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }


    private val callback = OnMapReadyCallback { googleMap ->


        try {
            mMap = googleMap
            initSendReportButtons()

            mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDrag(p0: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {
                    val updateReport = reportViewModel.updateReport(marker.id, marker.position)
                    val markerString = "${updateReport?.gun}, ${updateReport?.coord_lat}, " +
                            "${updateReport?.coord_long}, ${updateReport?.coord_alt}"
                    marker.showInfoWindow()
                    marker.title = markerString
                }

                override fun onMarkerDragStart(marker: Marker) {
                    marker.hideInfoWindow()
                }
            })

            initLocationRequest()
            placeMarker()
            fetchMarkers()

            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location ->
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM.toFloat())
                )

            }


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

    private fun displayLocationView() {
        sharedMainViewModel.showLocation.observe(viewLifecycleOwner) { showLocation ->
            binding.tvLocation.isVisible = showLocation
        }


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

    private fun displayClassificationView() {

        sharedMainViewModel.showResult.observe(viewLifecycleOwner) { showResult ->
            binding.tvResult.isVisible = showResult
        }

        settingsViewModel.classificationResult.observe(viewLifecycleOwner) { result ->
            activity?.runOnUiThread {
                val resultString =
                    "Category: ${result.category}\nType: ${result.specificType}\nScore: ${result.score}"
                binding.tvResult.text = resultString
            }
        }
    }

    private fun fetchMarkers() {
        for (marker in reportViewModel.manualReports.value!!.entries) {
            val report = marker.value
            addDraggableMarker(report)
        }

    }


    private fun placeMarker() {
        mMap.setOnMapClickListener { latLng ->
            setupDialog(latLng)
        }
    }

    private suspend fun fetchElevation(location: LatLng): String {
        try {
            val response = withContext(Dispatchers.IO) {
                GoogleService.service.getElevation(GoogleService.toLocationString(location))
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

        val reportDialog: ReportDialog
        val weapons = mutableListOf<String>()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reportViewModel.guns.collect { data ->
                    when (data) {
                        is NetworkResult.Loading -> Toast.makeText(
                            requireContext(),
                            "Fetching guns...",
                            Toast.LENGTH_SHORT
                        ).show()
                        is NetworkResult.Success -> {
                            for (gun in data.data!!) {
                                weapons.add(gun.name)
                            }
                        }
                        is NetworkResult.Error ->
                            Toast.makeText(requireContext(), data.message, Toast.LENGTH_SHORT)
                                .show()
                    }
                }
            }
        }

        reportViewModel.fetchElevation(latLng)
        val elevation = reportViewModel.elevation.value.toString()
        reportDialog = ReportDialog(weapons, latLng, elevation, this@MapsFragment)
        reportDialog.show(parentFragmentManager, "ReportDialog")

    }


    private fun initSendReportButtons() {

        binding.fabAddReport.isVisible = false
        binding.fabRemoveMarkers.isVisible = false

        reportViewModel.manualReports.observe(viewLifecycleOwner) { reports ->
            if (reports.isNotEmpty()) {
                binding.fabAddReport.show()
                binding.fabRemoveMarkers.show()

            } else {
                binding.fabAddReport.hide()
                binding.fabRemoveMarkers.hide()
                mMap.clear()
            }
        }



        binding.fabAddReport.setOnClickListener {
            reportViewModel.sendManualReports()
        }

        binding.fabRemoveMarkers.setOnClickListener {
            binding.fabAddReport.hide()
            binding.fabRemoveMarkers.hide()
            mMap.clear()
            reportViewModel.clearReports()
        }



    }

    override fun onReportSelected(report: Report) {

        val marker = addDraggableMarker(report)
        marker!!.showInfoWindow()
        reportViewModel.addManualReport(marker.id, report)
        Log.e("onCreateDialog", report.toString() )
    }

    private fun checkLocationProvidersEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        return isGpsEnabled || isNetworkEnabled
    }


    private fun addDraggableMarker(report: Report): Marker? {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(report.timestamp)

        Log.e("onCreateDialog", report.toString() )
        val markerString =
            "${report.gun}, ${report.coord_lat}, ${report.coord_long}, ${report.coord_alt}"
        val markerOptions = MarkerOptions()
            .position(LatLng(report.coord_lat.toDouble(), report.coord_long.toDouble()))
            .title(markerString)
            .snippet(date)
            .draggable(true)

        return mMap.addMarker(markerOptions)
    }

    private fun createMarker(gunshot: Gunshot) {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(gunshot.timestamp)
        val markerString =
            "${gunshot.gun}, ${gunshot.coordAlt}, ${gunshot.coordLong}, ${gunshot.coordAlt}"
        val markerOptions = MarkerOptions()
            .position(LatLng(gunshot.coordLat.toDouble(), gunshot.coordLong.toDouble()))
            .title(markerString)
            .snippet(date)

        mMap.addMarker(markerOptions)
    }

    private fun gunshotsToMarkers(gunshotList: List<Gunshot>) {
        for (gunshot in gunshotList) {
            createMarker(gunshot)
        }
    }
}