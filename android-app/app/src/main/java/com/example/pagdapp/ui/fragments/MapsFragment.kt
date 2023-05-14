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
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.model.networkModels.GunshotNetworkModel
import com.example.pagdapp.databinding.FragmentMapsBinding
import com.example.pagdapp.ui.viewModels.MainViewModel
import com.example.pagdapp.ui.viewModels.ReportViewModel
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.example.pagdapp.utils.MapUtils
import com.example.pagdapp.utils.NetworkResult
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MapsFragment : Fragment(), ReportDialog.ReportDialogListener, GunshotDialog.GunshotListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val _userLocation = MutableLiveData<Location>()
    private var userLocation: LiveData<Location>? = _userLocation
    private lateinit var gunshotData: GunshotData
    private val heatmapData = mutableListOf<LatLng>()
    private val heatmapProvider = HeatmapTileProvider.Builder()


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


    }

    private fun subscribeGunshotNotification(mMap: GoogleMap) {
        sharedMainViewModel.gunshotNotifications.observe(viewLifecycleOwner) {
            MapUtils.createMarker(it, mMap)

        }

    }

    private fun subscribeCollectors(mMap: GoogleMap) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        reportViewModel.gunshots,
                        sharedMainViewModel.showHeatmap.asFlow()
                    ) { gunshotsResult, showHeatmap ->
                        Pair(gunshotsResult, showHeatmap)
                    }.collect { (result, showHeatmap) ->
                        when (result) {
                            is NetworkResult.Loading -> ""
                            is NetworkResult.Success -> {
                                mMap.clear()
                                if (showHeatmap) {
                                    addHeatMap(mMap, result.data!!)
                                } else {
                                    gunshotsToMarkers(result.data!!)
                                }
                            }
                            is NetworkResult.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }

                launch {
                    reportViewModel.errorFlow.collect { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }


    private val callback = OnMapReadyCallback { googleMap ->
        try {
            mMap = googleMap
            initSendReportButtons()
            subscribeGunshotNotification(mMap)
            subscribeCollectors(mMap)

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
            placeGunshot()
            fetchMarkers()

            mMap.isMyLocationEnabled = true

            sharedMainViewModel.clickGunshotNotification.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let { gunshotData ->
                    MapUtils.animateCameraToLocation(
                        mMap,
                        LatLng(gunshotData.coordLat, gunshotData.coordLong),
                        DEFAULT_ZOOM.toFloat(),
                        1000
                    )
                } ?: fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM.toFloat())
                    )
                }
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
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

    private fun placeGunshot() {
        mMap.setOnMapLongClickListener { latLng ->
            setupGunshotDialog(latLng)
        }
    }


    private fun placeMarker() {
        mMap.setOnMapClickListener { latLng ->
            setupDialog(latLng)
        }
    }

    private fun setupGunshotDialog(latLng: LatLng) {
        val gunshotDialog: GunshotDialog
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
        gunshotDialog = GunshotDialog(weapons, latLng, elevation, this@MapsFragment)
        gunshotDialog.show(parentFragmentManager, "ReportDialog")
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
        binding.fabRemoveGunshots.isVisible = false

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    reportViewModel.gunshots.collect {
                        binding.fabRemoveGunshots.isVisible =
                            it is NetworkResult.Success && it.data?.isNotEmpty() == true
                    }
                }
            }
        }

        binding.fabRemoveGunshots.setOnClickListener {
            reportViewModel.removeGunshots()
            mMap.clear()
            binding.fabRemoveGunshots.isVisible = false
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
        Log.e("onCreateDialog", report.toString())
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
            "${gunshot.gun}, ${gunshot.coordLong}, ${gunshot.coordLat}, ${gunshot.coordAlt}, shots fired:${gunshot.shotsFired}"
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


    private fun gunshotToHeatmapData(gunshotList: List<Gunshot>): List<LatLng> {
        val list = mutableListOf<LatLng>()
        for (gunshot in gunshotList) {
            val data = LatLng(gunshot.coordLat.toDouble(), gunshot.coordLong.toDouble())
            list.add(data)
        }
        return list
    }

    override fun onGunshotSelected(gunshot: GunshotNetworkModel) {
        reportViewModel.addGunshot(gunshot)
        Log.e("onCreateDialog", gunshot.toString())
    }


    private fun addHeatMap(mMap: GoogleMap, gunshotList: List<Gunshot>) {
        val latLngs: List<LatLng?> = gunshotToHeatmapData(gunshotList)
        Log.e("addHeatMap", latLngs.toString())

        try {
            val provider = HeatmapTileProvider.Builder()
                .data(latLngs)
                .build()

            // Add a tile overlay to the map, using the heat map tile provider.
            val overlay = mMap.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        } catch (e: IllegalArgumentException) {
        }

    }

    private fun removeHeatMap() {
        // tileOverlay?.remove()
    }


}