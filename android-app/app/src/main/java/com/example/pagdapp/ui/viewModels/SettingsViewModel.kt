package com.example.pagdapp.ui.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.pagdapp.data.model.audioclassifier.Category
import com.example.pagdapp.data.repository.SharedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedRepository: SharedRepository
) :
    ViewModel() {


    val activeClassifier: LiveData<String> get() = sharedRepository.activeClassifierName

    val classificationResult = sharedRepository.classifierResult

    private val _delay = MutableLiveData(500L)
    val delay : LiveData<Long> = _delay

    val listeningDelay : LiveData<Long>? = sharedRepository.activeClassifier.value?.getDelay()

    val intervalDelay: LiveData<Long> get() = sharedRepository.intervalDelay

    private val _threshold = MutableLiveData(0.5F)
    val threshold: LiveData<Float> = _threshold

    private val _categories = MutableLiveData<List<Category>>()
    var categories: LiveData<List<Category>> = _categories

    private val _checkAllCategories = MutableLiveData(sharedRepository.activeClassifier.value?.allCategoriesIncluded())
    val checkAllCategories: LiveData<Boolean?> = _checkAllCategories
    val isRunning: LiveData<Boolean> get() = sharedRepository.isRunning
    val isListening: LiveData<Boolean> get() = sharedRepository.isListening

    init {
        updateCategories()
    }


    fun updateCategories() {
        _categories.value = sharedRepository.activeClassifier.value!!.getCategories()
    }

    fun setActiveClassifier(classifierName: String) {
        sharedRepository.switchClassifier(classifierName)
    }

    fun updateThreshold() {
        threshold.value?.let { sharedRepository.activeClassifier.value!!.setThreshold(it) }
    }

    fun setThreshold(threesHold: Float) {
        _threshold.postValue(threesHold)
    }

    fun updateDelay() {
        Log.e("setDelay", "setDelay2 ${delay.value}")
        delay.value?.let { sharedRepository.activeClassifier.value!!.setDelay(it) }
    }


    fun setDelay(threesHold: Long) {
        Log.e("setDelay", "setDelay1 $threesHold")
        _delay.postValue(threesHold)
    }


    fun updateCategory(category: String, isChecked: Boolean) {
        if (isChecked) {
            sharedRepository.activeClassifier.value!!.includeCategory(category)
        } else {
            sharedRepository.activeClassifier.value!!.excludeCategory(category)
        }

    }

    fun updateAllCategories(checkAll: Boolean) {

        if (checkAll) {
            sharedRepository.activeClassifier.value!!.includeAllCategories()
            updateCategories()
        } else {
            sharedRepository.activeClassifier.value!!.excludeAllCategories()
            updateCategories()
        }
        _checkAllCategories.value = checkAll
    }



    private fun stringListToCategoryList(categories: List<String>): List<Category> {
        val result = mutableListOf<Category>()
        for (category in categories) {

            result.add(Category(category, true))
        }
        return result
    }

    fun updateListeningInterval(value: Long) {
        sharedRepository.updateListeningDelay(value)
    }

}