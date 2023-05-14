package com.example.pagdapp.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.model.audioclassifier.AudioClassificationResult
import com.example.pagdapp.data.model.audioclassifier.IAudioClassifier
import com.example.pagdapp.utils.Constants
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Named


class SharedRepository @Inject constructor(
    private val context: Context,
    @Named("PAGDClassifierModel5") private val audioClassifierPAGD_5: IAudioClassifier,
    @Named("PAGDClassifierModel8") private val audioClassifierPAGD_8: IAudioClassifier,
    @Named("PAGDClassifierModel9") private val audioClassifierPAGD_9: IAudioClassifier,
    @Named("YamnetClassifier") private val audioClassifierYamnet: IAudioClassifier
) {



    private val scope = CoroutineScope(Dispatchers.IO)
    private var collectionJob: Job? = null

    private val _isSendingReports = MutableLiveData(false)
    val isSendingReports: LiveData<Boolean> = _isSendingReports

    private val _activeClassifier = MutableLiveData(audioClassifierYamnet)
    val activeClassifier: LiveData<IAudioClassifier> get() = _activeClassifier

    private val _activeClassifierName = MutableLiveData<String>()
    val activeClassifierName: LiveData<String> get() = _activeClassifierName

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> get() = _isRunning

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    private val _intervalDelay = MutableLiveData(Constants.CONTINUOUS_NETWORK_CALL_MIDDLE)
    val intervalDelay: LiveData<Long> = _intervalDelay

    private val _classifierResult = MutableLiveData<AudioClassificationResult>()
    val classifierResult: LiveData<AudioClassificationResult> = _classifierResult

    private val _notificationFlow = MutableSharedFlow<GunshotData>()
    val notificationFlow: Flow<GunshotData> = _notificationFlow

    private var _dateFromToInMilliseconds = androidx.core.util.Pair(
        MaterialDatePicker.thisMonthInUtcMilliseconds(),
        MaterialDatePicker.todayInUtcMilliseconds()
    )

    private val _gunshotNotifications = MutableLiveData<GunshotData>()
    val gunshotNotifications : LiveData<GunshotData> = _gunshotNotifications

    val dateFromToInMilli: Pair<Long, Long>
        get() = Pair(
            _dateFromToInMilliseconds.first,
            _dateFromToInMilliseconds.second
        )

    fun setDateFromTo(date: androidx.core.util.Pair<Long, Long>) {
        _dateFromToInMilliseconds = date
    }

    fun setIsRunning(isRunning: Boolean) {
        _isRunning.postValue(isRunning)
        _isRunning.value = isRunning
    }

    fun setSendReport(boolean: Boolean) {
        _isSendingReports.postValue(boolean)
    }

    fun updateListening(boolean: Boolean) {
        _isListening.postValue(boolean)
    }

    fun updateListeningDelay(interval : Long) {
        _intervalDelay.postValue(interval)
    }


    init {
        val sharedPreferences = context.getSharedPreferences("PAGDApp", Context.MODE_PRIVATE)
        switchClassifier(
            sharedPreferences.getString("SELECTED_CLASSIFIER", "PAGD Legacy model").toString()
        )
    }

    fun switchClassifier(classifierType: String) {
        if (classifierType == _activeClassifierName.value) {
            return
        }

        val sharedPreferences = context.getSharedPreferences("PAGDApp", Context.MODE_PRIVATE)

        when (classifierType) {

            "PAGD Legacy model 5" -> {
                _activeClassifier.value = audioClassifierPAGD_5
                _activeClassifierName.value = "PAGD Legacy model 5"
                sharedPreferences.edit().putString("SELECTED_CLASSIFIER", "PAGD Legacy model 5")
                    .apply()
            }
            "PAGD Legacy model 8" -> {
                _activeClassifier.value = audioClassifierPAGD_8
                _activeClassifierName.value = "PAGD Legacy model 8"
                sharedPreferences.edit().putString("SELECTED_CLASSIFIER", "PAGD Legacy model 8")
                    .apply()
            }
            "PAGD Legacy model 9" -> {
                _activeClassifier.value = audioClassifierPAGD_9
                _activeClassifierName.value = "PAGD Legacy model 9"
                sharedPreferences.edit().putString("SELECTED_CLASSIFIER", "PAGD Legacy model 9")
                    .apply()
            }
            "Yamnet model" -> {
                _activeClassifier.value = audioClassifierYamnet
                _activeClassifierName.value = "Yamnet model"
                sharedPreferences.edit().putString("SELECTED_CLASSIFIER", "Yamnet model").apply()
            }

        }
        _activeClassifier.value!!.allCategoriesIncluded()
        collectFlow()

    }

    /*
    fun collectResult(): Flow<AudioClassificationResult>? {
        return activeClassifier.value?.getResultAsFlow()
    }

     */

    fun getActiveClassifierResultFlow(): Flow<AudioClassificationResult> {
        return activeClassifier.value?.getResultAsFlow() ?: emptyFlow()
    }

    private fun collectFlow() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            activeClassifier.value?.getResultAsFlow()?.collect { result ->
                Log.e("TESTFLOW", result.toString())
                _classifierResult.postValue(result)
            }
        }
    }

    fun updateGunshotNotification(gunshotData: GunshotData){
        _gunshotNotifications.postValue(gunshotData)
    }

    suspend fun updateNotificationFlow(message: GunshotData) {
        _notificationFlow.emit(message)
        Log.e("updateNotificationFlow", message.toString() )
    }

}
