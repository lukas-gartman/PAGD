package com.example.testapp

import android.annotation.SuppressLint
import android.media.*
import comirva.audio.util.MFCC

class AudioProcessor {
    /* Audio record parameters */
    val SAMPLE_RATE = 16384
    val FRAME_SECONDS = 2.0

    /* Mel frequency spectrum parameters */
    val WINDOW_SIZE = 2048
    val NUMBER_MELS = 128
    val NUMBER_COEFFICIENTS = 20
    val USE_FIRST_COEFFICIENT = true
    val MIN_FREQ = 1
    val MAX_FREQ = SAMPLE_RATE/2

    /* Other parameters not needing to be changed */

    val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    val RECORDER_BUFFER_SIZE = SAMPLE_RATE * 10
    val FRAME_SAMPLES = (SAMPLE_RATE*FRAME_SECONDS).toInt()
    val NUMBER_WINDOWS = FRAME_SAMPLES / WINDOW_SIZE

    /* Create needed objects */

    private val buffer = FloatArray(FRAME_SAMPLES)
    private val dbuffer = DoubleArray(FRAME_SAMPLES)

    private val mfcc_processor = MFCC(
        SAMPLE_RATE.toFloat(), WINDOW_SIZE, NUMBER_COEFFICIENTS, USE_FIRST_COEFFICIENT,
        MIN_FREQ.toDouble(), MAX_FREQ.toDouble(), NUMBER_MELS)

    @SuppressLint("MissingPermission")
    private val audio_recorder = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, RECORDER_BUFFER_SIZE)

    //Now start the audio recording
    init {
        if (audio_recorder!!.state != AudioRecord.STATE_INITIALIZED) {
            println("AudioRecord not able to be instantiated!")
        }
        audio_recorder.startRecording()
        audio_recorder.read(buffer, 0, FRAME_SAMPLES, AudioRecord.READ_BLOCKING)
        val thread = Thread {
            thread_work()
        }
        thread.start()
    }

    private fun thread_work() {
        while(true){
            /* Get new audio frame */
            val read_n = audio_recorder.read(buffer, 0, FRAME_SAMPLES,
                AudioRecord.READ_NON_BLOCKING)

            val timestamp = System.currentTimeMillis()

            /* Shift double buffer */
            for (i in 0 .. FRAME_SAMPLES-read_n-1)
                dbuffer[i] = dbuffer[i+read_n]

            /* Add frame to double buffer */
            for (i in 0 .. read_n-1)
                dbuffer[FRAME_SAMPLES-read_n+i] = buffer[i].toDouble()

            /* Get MFCC */
            val mfccs = mfcc_processor.process(dbuffer)
            // Height = NUMBER_WINDOWS, Width = NUMBER_COEFFICIENTS
        }
    }
}