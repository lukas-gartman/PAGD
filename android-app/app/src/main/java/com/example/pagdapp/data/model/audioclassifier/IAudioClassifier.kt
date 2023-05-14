package com.example.pagdapp.data.model.audioclassifier
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

interface IAudioClassifier {

    fun startRecording()
    fun stopRecording()
    fun includeCategory(category: String)
    fun excludeCategory(category: String)
    fun includeAllCategories()
    fun excludeAllCategories()
    fun setThreshold(threshold: Float)
    fun setDelay(delay: Long)
    fun getDelay(): LiveData<Long>
    fun getResult(): LiveData<String>
    fun getAudioClassificationResult(): LiveData<AudioClassificationResult>
    fun getCategories(): List<Category>
    fun setAudioClassificationListener(listener: AudioClassifierListener)
    fun removeGunshotListener()
    fun getResultAsFlow(): Flow<AudioClassificationResult>
    fun allCategoriesIncluded(): Boolean
}