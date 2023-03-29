package com.example.pagdapp.viewModels

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import com.example.pagdapp.models.Category
import com.example.pagdapp.models.Position
import com.example.pagdapp.models.audioclassifier.AudioClassificationResult
import com.example.pagdapp.models.audioclassifier.AudioClassifier
import com.example.pagdapp.models.audioclassifier.AudioClassifierListener
import com.example.pagdapp.models.audioclassifier.IAudioClassifier
import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.models.dbModels.Gunshot
import com.example.pagdapp.models.dbModels.Report
import com.example.pagdapp.repositories.pagdServer.ServerApi
import com.example.pagdapp.repositories.pagdServer.PAGDServer
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {


    /**
     * AI-model variables
     */
    private var audioClassifier: IAudioClassifier
    private val model = "lite-model_yamnet_classification_tflite_1.tflite"

    private val _classificationResult = MutableLiveData<AudioClassificationResult>()
    val classificationResult = _classificationResult


    private val _threshold = MutableLiveData(0.5F)
    val threshold: LiveData<Float> = _threshold

    lateinit var categories: List<Category>

    private val _checkAllCategories = MutableLiveData(true)
    val checkAllCategories: LiveData<Boolean> = _checkAllCategories

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    /**
     * ServerAPI variables
     */

    private lateinit var pagdServer: ServerApi

    private val _isSendingReports = MutableLiveData(false)
    val isSendingReports: LiveData<Boolean> = _isSendingReports

    private val _guns = MutableLiveData<List<Gun>>(mutableListOf())
    val guns: LiveData<List<Gun>> = _guns

    private val _reports = MutableLiveData<List<Report>>(mutableListOf())
    val reports: LiveData<List<Report>> = _reports

    private val _manualReports = MutableLiveData<HashMap<String, Report>>(hashMapOf())
    val manualReports: LiveData<HashMap<String, Report>> = _manualReports

    fun addManualReport(markerId: String, report: Report) {
        val currentReports = _manualReports.value ?: hashMapOf()
        currentReports[markerId] = report
        _manualReports.value = currentReports
    }

    fun updateReport(markerId: String, latLng: LatLng) : Report? {
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

    fun clearReports(){
        _manualReports.value = hashMapOf()
    }
    /**
     * MapAPI variables
     */


    private val gunshots = mutableListOf<Position>()
    private val _userLocation = MutableLiveData<Location>()
    private val userLocation: LiveData<Location> = _userLocation


    init {
        // Initialize the audio classifier
        audioClassifier = AudioClassifier(application.applicationContext, model)

        viewModelScope.launch {
            try {
                categories = stringListToCategoryList(audioClassifier.getCategories())


                // Instantiate the server api
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        pagdServer = PAGDServer.getInstance()
                        getGuns(null)
                        getReport(null, null, null)
                    }

                }
                audioClassifier.setAudioClassificationListener( object : AudioClassifierListener {
                    override fun onAudioClassification(result: AudioClassificationResult) {
                        _classificationResult.postValue(result)
                        viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                sendReportToServer(
                                    userLocation.value!!.time,
                                    userLocation.value!!.latitude.toFloat(),
                                    userLocation.value!!.longitude.toFloat(),
                                    userLocation.value!!.altitude.toFloat(),
                                    result.specificType.toString()
                                )

                            }
                        }
                    }
                })

            } catch (e: IOException) {
                Log.e(TAG, e.toString())
                // e.printStackTrace()
            }
        }



    }


    /**
     * AI-Model functions
     */

    fun toggleRecording() {

        // TODO (Implement permissions)
        if (!isRecording.value!!) {
            _isRecording.postValue(true)
            audioClassifier.startRecording()

        } else if (isRecording.value!!) {
            _isRecording.postValue(false)
            audioClassifier.stopRecording()
        }
    }


    // TODO should maybe observe the threshold instead of being update in a function
    fun updateThreshold() {
        threshold.value?.let { audioClassifier.setThreshold(it) }
    }

    fun updateCategory(category: Category) {
        if (category.isIncluded) {
            audioClassifier.includeCategory(category.title)
        } else {
            audioClassifier.excludeCategory(category.title)
        }

    }

    fun updateAllCategories() {
        _checkAllCategories.value = !checkAllCategories.value!!
        if (checkAllCategories.value == true) {
            audioClassifier.includeAllCategories()
        } else if (checkAllCategories.value == false) {
            audioClassifier.excludeAllCategories()
        }

    }

    fun setThreshold(threesHold: Float) {
        _threshold.postValue(threesHold)
    }


    /**
     * MapAPI functions
     */


    fun updateUserLocation(location: Location) {
        _userLocation.postValue(location)
    }


    /**
     * ServerAPI calls
     */

    fun toggleReport() {
        _isSendingReports.value = !_isSendingReports.value!!
    }

    private fun getGuns(gun_name: String?) {
        viewModelScope.launch {
            try {
                val response = pagdServer.getGun(gun_name)
                val result: List<Gun>?
                Log.e("APICALL", response.toString())
                if (response.isSuccessful && response.body() != null) {
                    result = response.body()
                    _guns.value = result!!

                } else {
                    _guns.value = mutableListOf()
                }

            } catch (e: Exception) {
                Log.e("Failed to get guns", e.toString())
                _guns.value = mutableListOf()

            }
        }
    }

    fun addGun(name: String, type: String) {

        viewModelScope.launch {
            val response = pagdServer.addGunToDB(Gun(name, type))
            try {
                if (response.isSuccessful) {
                    Log.e("APICALL", "Added gun to server: ${response.message()}")
                }

            } catch (e: Exception) {
                Log.e("Failed to add gun", response.message())
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

        if (!isSendingReports.value!!) return
        if (classificationResult.value?.score!! < threshold.value!!) return


        val response =
            pagdServer.sendReport(Report(timestamp, coord_lat, coord_long, coord_alt, gun))

        Log.e("sendReportToServer", response.toString())
        try {
            if (response.isSuccessful) {
                Log.e("sendReportToServer", response.message())
            }

        } catch (e: Exception) {
            Log.e("Failed to send report", e.toString())
        }

    }

    fun getReport(id: Int?, time_from: Int?, time_to: Int?) {
        viewModelScope.launch {
            val response = pagdServer.getReport(id, time_from, time_to)
            val result: List<Report>?
            try {

                if (response.isSuccessful && response.body() != null) {
                    result = response.body()
                    _reports.value = result!!
                } else {
                    _reports.value = mutableListOf()
                }

            } catch (e: Exception) {
                Log.e("Failed to get reports", e.toString())
                _reports.value = mutableListOf()
            }

        }
    }

    fun addGunShot(
        timestamp: Long,
        coord_lat: Float,
        coord_long: Float,
        coord_alt: Float,
        gun: Gun
    ) {
        val call = pagdServer.sendRecord(Gunshot(timestamp, coord_lat, coord_long, coord_alt, gun))

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.e("ServerAPICall", response.message())
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("ServerAPICall", "Failed to add gunshot")
            }
        })

    }

    fun getGunShots(
        timestamp: Int?,
        coord_lat: String?,
        coord_long: String?,
        coord_alt: String?
    ): List<Gunshot>? {
        val call = pagdServer.getGunshot(timestamp, coord_lat, coord_long, coord_alt)
        var result: List<Gunshot>? = null

        call.enqueue(object : Callback<List<Gunshot>> {
            override fun onResponse(call: Call<List<Gunshot>>, response: Response<List<Gunshot>>) {
                val gunshots = response.body()
                if (response.isSuccessful && gunshots != null) {
                    result = gunshots
                }
            }

            override fun onFailure(call: Call<List<Gunshot>>, t: Throwable) {
                Log.e("ServerAPICall", "Failed to get gunshots")
            }
        })

        return result
    }

    private fun stringListToCategoryList(category: List<String>): List<Category> {
        val result = mutableListOf<Category>()
        for (s in category) {
            result.add(Category(s, true))
        }
        return result
    }


    // TODO get gunshots result based on radius from the users position


    /**
     *  ViewModel functions
     */


    override fun onCleared() {
        super.onCleared()
        audioClassifier.stopRecording()
        audioClassifier.removeGunshotListener()
    }
}