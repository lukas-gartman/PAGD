package com.example.pagdapp.data.model.waveRecorder
/*
 * MIT License
 *
 * Copyright (c) 2019 squti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */



import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The waveRecorder class used to record Waveform audio file using AudioRecord class to get the audio stream in PCM encoding
 * and then convert it to WAVE file (WAV due to its filename extension) by adding appropriate headers. This class uses
 * Kotlin Coroutine with IO dispatcher to writing input data on storage asynchronously.
 * @property filePath the path of the file to be saved.
 */
class WaveRecorder(private val context: Context, private var filePath: String) {
    /**
     * Configuration for recording audio file.
     */
    var waveConfig: WaveConfig = WaveConfig()

    /**
     * Register a callback to be invoked in every recorded chunk of audio data
     * to get max amplitude of that chunk.
     */
    var onAmplitudeListener: ((Int) -> Unit)? = null

    /**
     * Register a callback to be invoked in recording state changes
     */
    var onStateChangeListener: ((RecorderState) -> Unit)? = null

    /**
     * Register a callback to get elapsed recording time in seconds
     */
    var onTimeElapsed: ((Long) -> Unit)? = null

    /**
     * Activates Noise Suppressor during recording if the device implements noise
     * suppression.
     */
    var noiseSuppressorActive: Boolean = false

    /**
     * The ID of the audio session this waveRecorder belongs to.
     * The default value is -1 which means no audio session exist.
     */
    var audioSessionId: Int = -1
        private set

    private var isRecording = false
    private var isPaused = false
    private lateinit var audioRecorder: AudioRecord
    private var noiseSuppressor: NoiseSuppressor? = null
    private var channelModulus = 2
    private var uriToFile : Uri? = null

    /**
     * Starts audio recording asynchronously and writes recorded data chunks on storage.
     */
    @SuppressLint("MissingPermission")
    fun startRecording() {

        if (!isAudioRecorderInitialized()) {
            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                waveConfig.sampleRate,
                waveConfig.channels,
                waveConfig.audioEncoding,
                AudioRecord.getMinBufferSize(
                    waveConfig.sampleRate,
                    waveConfig.channels,
                    waveConfig.audioEncoding
                )
            )
            if (waveConfig.channels == AudioFormat.CHANNEL_IN_STEREO)
                channelModulus = 4

            audioSessionId = audioRecorder.audioSessionId

            isRecording = true

            audioRecorder.startRecording()

            if (noiseSuppressorActive) {
                noiseSuppressor = NoiseSuppressor.create(audioRecorder.audioSessionId)
            }

            onStateChangeListener?.let {
                it(RecorderState.RECORDING)
            }

            GlobalScope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    writeAudioDataToStorageUpdate()
                } else {
                    writeAudioDataToStorage()
                }

            }
        }
    }


    private suspend fun writeAudioDataToStorageUpdate() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ByteArray(bufferSize)
        val file = File(filePath)

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "Audio Recording")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/AudioRecorder/")
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        uriToFile = uri
        val outputStream = uri?.let { context.contentResolver.openOutputStream(it) }

        try {
            while (isRecording) {
                val operationStatus = audioRecorder.read(data, 0, bufferSize)

                if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                    if (!isPaused) {

                        outputStream?.write(data)

                    }
                }

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(calculateAmplitudeMax(data))
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds: Long =
                            file.length() / (channelModulus * waveConfig.sampleRate)
                        it(audioLengthInSeconds)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        outputStream?.close()
        noiseSuppressor?.release()

        values.clear()

    }

    private suspend fun writeAudioDataToStorage() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ByteArray(bufferSize)
        val file = File(filePath)
        val outputStream = file.outputStream()
        while (isRecording) {
            val operationStatus = audioRecorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                if (!isPaused) outputStream.write(data)

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(calculateAmplitudeMax(data))
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds: Long =
                            file.length() / (channelModulus * waveConfig.sampleRate)
                        it(audioLengthInSeconds)
                    }
                }


            }
        }

        outputStream.close()
        noiseSuppressor?.release()
    }

    private fun calculateAmplitudeMax(data: ByteArray): Int {
        val shortData = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            .get(shortData)

        return shortData.maxOrNull()?.toInt() ?: 0
    }

    /** Changes @property filePath to @param newFilePath
     * Calling this method while still recording throws an IllegalStateException
     */
    fun changeFilePath(newFilePath: String) {
        if (isRecording)
            throw IllegalStateException("Cannot change filePath when still recording.")
        else
            filePath = newFilePath
    }

    /**
     * Stops audio recorder and release resources then writes recorded file headers.
     */
    fun stopRecording() {

        if (isAudioRecorderInitialized()) {
            isRecording = false
            isPaused = false
            audioRecorder.stop()
            audioRecorder.release()
            audioSessionId = -1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                uriToFile?.let { WaveHeaderWriterUpdate(it, waveConfig, context).writeHeader() }
                onStateChangeListener?.let {
                    it(RecorderState.STOP)
                }
            } else {
                WaveHeaderWriter(filePath, waveConfig).writeHeader()
                onStateChangeListener?.let {
                    it(RecorderState.STOP)
                }
            }

        }

    }

    private fun isAudioRecorderInitialized(): Boolean =
        this::audioRecorder.isInitialized && audioRecorder.state == AudioRecord.STATE_INITIALIZED

    fun pauseRecording() {
        isPaused = true
        onStateChangeListener?.let {
            it(RecorderState.PAUSE)
        }
    }

    fun resumeRecording() {
        isPaused = false
        onStateChangeListener?.let {
            it(RecorderState.RECORDING)
        }
    }

}