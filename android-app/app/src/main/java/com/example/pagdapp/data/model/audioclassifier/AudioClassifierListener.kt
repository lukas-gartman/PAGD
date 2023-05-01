package com.example.pagdapp.data.model.audioclassifier

interface AudioClassifierListener {

    fun onAudioClassification(result: AudioClassificationResult)
}