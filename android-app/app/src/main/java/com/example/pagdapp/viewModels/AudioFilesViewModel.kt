package com.example.pagdapp.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AudioFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val audioFilesLiveData = MutableLiveData<List<String>>()

    init {
        loadAudioFiles()
    }

    fun getAudioFiles(): LiveData<List<String>> {
        return audioFilesLiveData
    }

    private fun loadAudioFiles() {
        // TODO: load audio files from storage or database
        val audioFiles = listOf(
            "audio_1.mp3",
            "audio_2.mp3",
            "audio_3.mp3"
        )
        audioFilesLiveData.postValue(audioFiles)
    }
}
