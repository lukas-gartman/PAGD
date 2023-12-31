package com.example.pagdapp.data.model.audioclassifier

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pagdapp.data.model.waveRecorder.WaveConfig
import com.example.pagdapp.data.model.waveRecorder.WaveHeaderWriter
import com.example.pagdapp.data.model.waveRecorder.WaveHeaderWriterUpdate
import edu.mines.jtk.dsp.BandPassFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject


class PAGDClassifier @Inject constructor(
    private val context: Context,
    private val signature_interpreter: Interpreter,
    private val model_interpreter: Interpreter
) : IAudioClassifier {

    private var isRecording = false
    private val _result = MutableSharedFlow<Float>()
    val result: Flow<Float> get() = _result
    private val _resultString = MutableLiveData<String>()
    val resultString get() = _resultString
    private var audioClassifierListener: AudioClassifierListener? = null
    private var _classificationResult = MutableLiveData<AudioClassificationResult>()
    private var classificationResult: LiveData<AudioClassificationResult> = _classificationResult
    private val _probabilityThreshold = MutableLiveData(0.5f)
    private val probabilityThreshold: LiveData<Float> get() = _probabilityThreshold

    private val _delay = MutableLiveData(500L)
    private val delay: LiveData<Long> = _delay
    private val classificationFlow = MutableSharedFlow<AudioClassificationResult>()
    private var thisAIModelsCategories = mutableListOf<Category>()
    private var categoriesToInclude = mutableListOf<String>()

    var onTimeElapsed: ((Long) -> Unit)? = null
    var onAmplitudeListener: ((Int) -> Unit)? = null
    private var uriToFile: Uri? = null




    private var filepathforoldversion =
        Environment.getExternalStorageDirectory().absolutePath + "testRecording"

    private var channelModulus = 2


    /* Audio record parameters */

    val SAMPLE_RATE = 8000

    // val FRAME_SAMPLES = 16384   for longer spectogram
    val FRAME_SAMPLES = 2048

    /* Other parameters not needing to be changed */
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
     val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT // Original
  //  val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT // For Recording
    val RECORDER_BUFFER_SIZE = SAMPLE_RATE * 10

    /* Create needed objects */
    var waveConfig: WaveConfig = WaveConfig(
        sampleRate = SAMPLE_RATE,
        channels = CHANNEL_CONFIG,
        audioEncoding = AUDIO_FORMAT
    )

    private val buffer = FloatArray(FRAME_SAMPLES) // original
    private val full_buffer = FloatArray(FRAME_SAMPLES) // original

  //  private val buffer = ShortArray(FRAME_SAMPLES) // For recording
 //   private val full_buffer = ShortArray(FRAME_SAMPLES) // for recording

    private val inputs = java.util.HashMap<String, Any>() //Input hashmap to spectrogram signature
    private val outputs = java.util.HashMap<String, Any>() //output hashmap to spectrogram signature


    // private val output_tensor =
    //       Array<Array<FloatArray>>(256) { Array<FloatArray>(257) { floatArrayOf(1.0f) } } for longer spectogram
    private val output_tensor =
        Array<Array<FloatArray>>(64) { Array<FloatArray>(65) { floatArrayOf(1.0f) } }


    private val lowcut = 1000.0 / SAMPLE_RATE
    private val highcut = 3999.0 / SAMPLE_RATE
    private val bpf = BandPassFilter(lowcut, highcut, 0.1, 0.01)


    @SuppressLint("MissingPermission")
    private val audio_recorder =
        AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, RECORDER_BUFFER_SIZE)


    init {
        try {
            outputs["spectrogram"] = output_tensor //Add output tensor to output hashmap

            if (audio_recorder.state != AudioRecord.STATE_INITIALIZED) {
                println("AudioRecord not able to be instantiated!")
            }
            extractLabels(arrayListOf("Gun1", "Gun2", "Gun4", "woops", "missed one", "Gun3"))
            includeAllCategories()
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    //Function that thread runs
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun thread_work() = withContext(Dispatchers.IO) {

        while (isActive && isRecording) {

            val timestamp = System.currentTimeMillis()
            /* Get new audio frame */
            val read_n = audio_recorder.read(
                buffer, 0, FRAME_SAMPLES,
                AudioRecord.READ_NON_BLOCKING
            )


            /* Shift full buffer */
            for (i in 0..FRAME_SAMPLES - read_n - 1)
                full_buffer[i] = full_buffer[i + read_n]

            /* Add frame to full buffer */
            for (i in 0..read_n - 1)
                full_buffer[FRAME_SAMPLES - read_n + i] = buffer[i] // (buffer[i] * 32767)

            val y = FloatArray(full_buffer.size)
            bpf.apply(full_buffer,y)
            inputs["wave"] = y //Set audio wave as input
            signature_interpreter.runSignature(
                inputs,
                outputs,
                "spectrogram"
            )

            //Run spectrogram function
            val spectrogram = outputs["spectrogram"] as Array<Array<FloatArray>>

            val output2 = arrayOf(floatArrayOf(1.0f))
            model_interpreter.run(arrayOf(spectrogram), output2)

            Log.e("classifyAudio", output2[0][0].toString())
            if (output2[0][0] >= probabilityThreshold.value!!) {
                val audioClassificationResult = AudioClassificationResult(
                    timestamp,
                    "Na",
                    "Na",
                    output2[0][0]
                )
                classificationFlow.emit(audioClassificationResult)
                println(output2[0][0])
            }

            delay(delay.value!!)
            Log.e("thread_work", output2[0][0].toString())

        }


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun startRecording() {
        if (audio_recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        isRecording = true
        audio_recorder.startRecording()
        audio_recorder.read(buffer, 0, FRAME_SAMPLES, AudioRecord.READ_BLOCKING)

/*

        val outputDirectory = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioRecorder")
        outputDirectory.mkdirs()
        val outputFile = File(outputDirectory, "audioRecording.mp4")


 */
        CoroutineScope(Dispatchers.IO).launch {
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                writeAudioDataToStorageUpdate(false)
            } else {
                writeAudioDataToStorage()
            }

             */
            thread_work()
        }
    }

    override fun stopRecording() {
        if (audio_recorder.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
            return
        }
        isRecording = false
        audio_recorder.stop()

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uriToFile?.let { WaveHeaderWriterUpdate(it, waveConfig, context).writeHeader() }

        } else {
            WaveHeaderWriter(
                Environment.getExternalStorageDirectory().absolutePath + "testRecording",
                waveConfig
            ).writeHeader()

        }

         */


    }

    override fun includeCategory(category: String) {
        Log.e("includeCategory", "$category - PAGD")
        categoriesToInclude.add(category)
    }

    override fun excludeCategory(category: String) {
        Log.e("excludeCategory", "$category - PAGD")
        categoriesToInclude.removeIf {
            it == category
        }
    }

    override fun includeAllCategories() {
        categoriesToInclude.clear()
        for (category in thisAIModelsCategories) {
            categoriesToInclude.add(category.title)
        }
        Log.e("switchClassifier", "included all PAGD")
    }

    override fun excludeAllCategories() {
        categoriesToInclude.clear()
        Log.e("switchClassifier", "excluded all PAGD")
    }

    override fun setThreshold(threshold: Float) {
        _probabilityThreshold.postValue(threshold)
    }

    override fun setDelay(delay: Long) {
        _delay.postValue(delay)
    }

    override fun getDelay(): LiveData<Long> {
        TODO("Not yet implemented")
    }

    override fun getResult(): LiveData<String> {
        return resultString
    }

    override fun getAudioClassificationResult(): LiveData<AudioClassificationResult> {
        return classificationResult
    }

    override fun getCategories(): List<Category> {
        return thisAIModelsCategories
    }

    override fun setAudioClassificationListener(listener: AudioClassifierListener) {
        this.audioClassifierListener = listener
    }

    override fun removeGunshotListener() {

    }

    override fun getResultAsFlow(): Flow<AudioClassificationResult> {
        return classificationFlow
    }

    override fun allCategoriesIncluded(): Boolean {
        for (category in thisAIModelsCategories) {
            if (!category.isIncluded) {
                Log.e("switchClassifier", category.title + "- PAGDS")
                return false
            }

        }
        return true
    }


    private fun extractLabels(classList: List<String>) {
        for (category in classList) {
            thisAIModelsCategories.add(Category(category, true))
        }
    }

    private fun calculateAmplitudeMax(data: ByteArray): Int {
        val shortData = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            .get(shortData)

        return shortData.maxOrNull()?.toInt() ?: 0
    }


    private suspend fun writeAudioDataToStorageUpdate(applyFilter: Boolean = false) {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val data = ByteArray(bufferSize)
        val file = File(MediaStore.Audio.Media.RELATIVE_PATH)

        val fileName = if (applyFilter) {
            "Audio_Recording_filtered"
        } else {
            "Audio_Recording_unfiltered"
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/AudioRecorder/")
        }

        val uri =
            context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        uriToFile = uri
        val outputStream = uri?.let { context.contentResolver.openOutputStream(it) }

        try {
            while (isRecording) {
                val operationStatus = audio_recorder.read(data, 0, bufferSize)

                if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                    if (applyFilter) {


                        outputStream?.write(data)
                    } else {
                        outputStream?.write(data)
                    }
                }

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(calculateAmplitudeMax(data))
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds: Long =
                            file.length() / (channelModulus * SAMPLE_RATE)
                        it(audioLengthInSeconds)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        outputStream?.close()

        values.clear()

    }


    private suspend fun writeAudioDataToStorage() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ByteArray(bufferSize)
        val file = File(Environment.getExternalStorageDirectory(), "testRecording")
        val outputStream = file.outputStream()
        while (isRecording) {
            val operationStatus = audio_recorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                outputStream.write(data)

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

    }

    private fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val bytes = ByteArray(floats.size * 4)
        val buffer = ByteBuffer.wrap(bytes).asFloatBuffer()
        buffer.put(floats)
        return bytes
    }

}
