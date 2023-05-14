package com.example.pagdapp.ui.viewModels

import android.location.Location
import androidx.lifecycle.*
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.data.model.audioclassifier.IAudioClassifier
import com.example.pagdapp.data.repository.SharedRepository
import com.example.pagdapp.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class MainViewModel @Inject constructor(
    private val sharedRepository: SharedRepository
) : ViewModel() {


    val isSendingReports: LiveData<Boolean> = sharedRepository.isSendingReports

    private val _userLocation = MutableLiveData<Location>()

    private val _showResult = MutableLiveData(false)
    val showResult: LiveData<Boolean> = _showResult

    private val _showLocation = MutableLiveData(false)
    val showLocation: LiveData<Boolean> = _showLocation

    private val _showHeatmap = MutableLiveData(false)
    val showHeatmap: LiveData<Boolean> = _showHeatmap


    val dateFromToInMilli: androidx.core.util.Pair<Long, Long>
        get() = androidx.core.util.Pair(
            sharedRepository.dateFromToInMilli.first,
            sharedRepository.dateFromToInMilli.second
        )


    val gunshotNotifications: LiveData<GunshotData> = sharedRepository.gunshotNotifications

    private val _clickGunshotNotification = MutableLiveData<Event<GunshotData>>()
    val clickGunshotNotification: LiveData<Event<GunshotData>> = _clickGunshotNotification

    private val _isFreshStart = MutableLiveData<Boolean>(true)
    val isFreshStart: LiveData<Boolean> get() = _isFreshStart


    fun setDateFromTo(date: androidx.core.util.Pair<Long, Long>) {
        sharedRepository.setDateFromTo(date)
    }

    fun setLocation(show: Boolean) {
        _showLocation.postValue(show)
    }

    fun setResult(show: Boolean) {
        _showResult.postValue(show)
    }

    fun toggleHeatmap(show: Boolean) {
        _showHeatmap.postValue(show)
    }


    fun updateUserLocation(location: Location) {
        _userLocation.postValue(location)
    }


    fun toggleReport() {
        sharedRepository.setSendReport(!isSendingReports.value!!)

    }


    override fun onCleared() {
        super.onCleared()
        sharedRepository.activeClassifier.value!!.removeGunshotListener()
    }

    fun updateClickGunshotNotification(gunshotData: GunshotData) {
        _clickGunshotNotification.postValue(Event(gunshotData))
    }

    fun setFreshStart(value: Boolean) {
        _isFreshStart.value = value
    }
}