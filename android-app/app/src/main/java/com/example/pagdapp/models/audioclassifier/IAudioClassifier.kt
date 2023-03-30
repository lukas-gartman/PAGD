package com.example.pagdapp.models.audioclassifier
import androidx.lifecycle.LiveData

interface IAudioClassifier {

    fun startRecording()
    fun stopRecording()
    fun includeCategory(category: String)
    fun excludeCategory(category: String)
    fun includeAllCategories()
    fun excludeAllCategories()
    fun setThreshold(threshold: Float)
    fun getResult(): LiveData<String>
    fun getAudioClassificationResult(): LiveData<AudioClassificationResult>
    fun getCategories(): List<String>
    fun setAudioClassificationListener(listener: AudioClassifierListener)
    fun removeGunshotListener()
}