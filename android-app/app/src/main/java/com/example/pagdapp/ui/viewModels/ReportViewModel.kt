package com.example.pagdapp.ui.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.dbModels.Gunshot
import com.example.pagdapp.data.model.dbModels.Report
import com.example.pagdapp.data.model.networkModels.GunshotNetworkModel
import com.example.pagdapp.data.model.networkModels.ReportNetworkModel
import com.example.pagdapp.data.repository.IGoogleRepository
import com.example.pagdapp.data.repository.IPAGDRepository
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.utils.Constants.GET_ELEVATION_ERROR
import com.example.pagdapp.utils.GunshotGenerator
import com.example.pagdapp.utils.NetworkResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val pagdRepo: IPAGDRepository,
    private val googleRepo: IGoogleRepository,
    private val sharedRepo: SharedRepository
) : ViewModel() {


    private val _guns = MutableStateFlow<NetworkResult<List<Gun>>>(NetworkResult.Loading())
    val guns: StateFlow<NetworkResult<List<Gun>>> = _guns

    private val _reports =
        MutableStateFlow<NetworkResult<List<ReportNetworkModel>>>(NetworkResult.Loading())
    val reports: StateFlow<NetworkResult<List<ReportNetworkModel>>> = _reports

    private val _gunshots = MutableStateFlow<NetworkResult<List<Gunshot>>>(NetworkResult.Loading())
    val gunshots = _gunshots.asStateFlow()

    private val _elevation = MutableStateFlow(0f)
    val elevation: StateFlow<Float> get() = _elevation

    private val _manualReports = MutableLiveData<HashMap<String, Report>>(hashMapOf())
    val manualReports: LiveData<HashMap<String, Report>> = _manualReports

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorFlow

    private val _successFlow = MutableStateFlow<String?>(null)
    val successFlow: StateFlow<String?> = _successFlow

    private val _apiRequest = MutableStateFlow<NetworkResult<String>>(NetworkResult.Loading())
    val apiRequest: StateFlow<NetworkResult<String>> = _apiRequest

    private val _sortAscending = MutableLiveData(true)
    val sortAscending: LiveData<Boolean> = _sortAscending

    private val weaponTypes = mutableListOf(
        Gun("AK-47", "Assault Rifle"),
        Gun("Sig9", "Handgun")
    )


    init {
        fetchAllGuns()
        fetchAllReports()
    }


    fun addManualReport(markerId: String, report: Report) {
        val currentReports = _manualReports.value ?: hashMapOf()
        currentReports[markerId] = report
        _manualReports.value = currentReports
    }

    fun updateReport(markerId: String, latLng: LatLng): Report? {
        val currentReports = _manualReports.value ?: hashMapOf()
        Log.e("addManualReport", "Before" + currentReports[markerId].toString())
        currentReports[markerId]?.apply {
            coord_lat = latLng.latitude.toFloat()
            coord_long = latLng.longitude.toFloat()
        }
        _manualReports.value = currentReports
        Log.e("addManualReport", "After" + currentReports[markerId].toString())
        return currentReports[markerId]

    }

    fun clearReports() {
        _manualReports.value = hashMapOf()
    }


    fun addGun(gun_name: String, gun_type: String) {
        viewModelScope.launch {
            pagdRepo.addGunToDB(Gun(gun_name, gun_type)).collect { result ->

                _apiRequest.value = result
                Log.e("addGun", result.message.toString())
            }
        }
    }

    fun fetchAllGuns() {
        getGuns(null)
    }

    fun fetchAllReports() {
        getReport(null, null, null)
    }

    private fun getGuns(gun_name: String?) {
        viewModelScope.launch {
            pagdRepo.getGun(gun_name).collect { result ->
                _guns.value = result
            }
        }
    }

    fun getReport(id: Int?, time_from: Long?, time_to: Long?) {
        viewModelScope.launch {
            pagdRepo.getReport(id, time_from, time_to)
                .catch { e -> e.printStackTrace() }
                .collect { result ->
                    _reports.value = result
                }
        }
    }


    suspend fun sendReportToServer(
        timestamp: Long,
        coord_lat: Float,
        coord_long: Float,
        coord_alt: Float,
        gun: String
    ) {
        viewModelScope.launch {
            val newReport =
                Report(timestamp, coord_lat, coord_long, coord_alt, gun)
            pagdRepo.addReport(newReport)
                .catch { e -> e.printStackTrace() }
                .collect { result ->
                    _apiRequest.value = result
                }
        }
    }


    private suspend fun gunshots(gunshotId: Int?, timeFrom: Long, timeTo: Long) {

        pagdRepo.getGunshot(gunshotId, timeFrom, timeTo).collect { gunshots ->
            _gunshots.value = gunshots
        }


        /*
        _gunshots.value = NetworkResult.Success(
            200,
            GunshotGenerator.generateGunshotSamples(10, 57.70849F, 11.97423F)
        )

         */
    }

    fun fetchSelectedGunshots() {
        viewModelScope.launch {
            val date = sharedRepo.dateFromToInMilli
            gunshots(null, date.first, date.second)
        }
    }


    fun fetchElevation(latLng: LatLng) = viewModelScope.launch {
        try {
            val fetchedElevation = googleRepo.getElevation(latLng).toFloat()
            _elevation.value = fetchedElevation
        } catch (e: IOException) {
            _errorFlow.emit(e.message!!)
        }
    }


    fun sendManualReports() {

        viewModelScope.launch {
            while (_manualReports.value!!.isNotEmpty()) {

                val report = _manualReports.value!!.entries.first().value

                pagdRepo.addReport(report)
                    .catch { e -> e.printStackTrace() }
                    .collect {
                        Log.e("sendManualReports", it.code.toString())
                    }

                val updatedReports = _manualReports.value!!
                updatedReports.remove(_manualReports.value!!.entries.first().key)
                _manualReports.postValue(updatedReports)
                delay(500L)
            }
        }
    }


    fun updateSorting(boolean: Boolean) {
        _sortAscending.postValue(boolean)
    }

    fun addGunshot(gunshot: GunshotNetworkModel) {
        viewModelScope.launch {
            pagdRepo.addGunshot(gunshot)
                .catch { e -> e.printStackTrace() }
                .collect{ result ->
                _apiRequest.value = result
            }
        }
    }

    fun removeGunshots() {
        _gunshots.value = NetworkResult.Success(200,emptyList())
    }

}