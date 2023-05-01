package com.example.pagdapp.ui.viewModels

import android.location.Location
import androidx.lifecycle.*
import com.example.pagdapp.data.model.audioclassifier.IAudioClassifier
import com.example.pagdapp.data.repository.SharedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class MainViewModel @Inject constructor(
    @Named("PAGDClassifier") private val audioClassifierPAGD: IAudioClassifier,
    @Named("YamnetClassifier") private val audioClassifierYamnet: IAudioClassifier,
    private val sharedRepository: SharedRepository
) : ViewModel() {



    val isSendingReports: LiveData<Boolean> = sharedRepository.isSendingReports

    private val _userLocation = MutableLiveData<Location>()

    private val _showResult = MutableLiveData(false)
    val showResult: LiveData<Boolean> = _showResult

    private val _showLocation = MutableLiveData(false)
    val showLocation: LiveData<Boolean> = _showLocation

    val dateFromToInMilli: androidx.core.util.Pair<Long, Long>
        get() = androidx.core.util.Pair(
            sharedRepository.dateFromToInMilli.first,
            sharedRepository.dateFromToInMilli.second
        )


    fun setDateFromTo(date: androidx.core.util.Pair<Long, Long>) {
        sharedRepository.setDateFromTo(date)
    }

    fun setLocation(show: Boolean) {
        _showLocation.postValue(show)
    }

    fun setResult(show: Boolean) {
        _showResult.postValue(show)
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
}