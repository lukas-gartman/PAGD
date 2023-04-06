package com.example.pagdapp.models.audioclassifier

interface AudioClassifierListener {

    fun onAudioClassification(result: AudioClassificationResult)
}