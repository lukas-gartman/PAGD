package com.example.pagdapp.data.model.audioclassifier

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.IOException
import java.lang.Runnable
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 * A real-time audio classifier that uses TensorFlow Lite.
 *
 * @param context The Android application context.
 * @param model The file path of the TensorFlow Lite audio classification model.
 *
 * This class allows you to record audio and classify it in real-time using a TensorFlow Lite
 * audio classification model. The class provides methods to start and stop recording, as well
 * as to get the classification result.
 *
 * To use this class, you must provide a TensorFlow Lite audio classification model file.
 * You can create an instance of this class by passing a context and the path to the model file
 * to the constructor. Once you have an instance of the class, you can start and stop recording
 * by calling the "startRecording" and "stopRecording" methods, respectively. You can get the
 * classification result by calling the "getResult" method.
 *
 * Note that this class uses a `ThreadPoolExecutor` to run the classification logic on a separate
 * thread, in order to avoid blocking the calling thread.
 * @property probabilityThreshold The minimum probability score required for a classification result to be considered valid.
 * @property format The format specification for the audio input required by the TensorFlow Lite model.
 * @property result The LiveData object containing the most recent classification result.

 */
class YamnetClassifier(context: Context, model: String) : IAudioClassifier {

    private var isRecording = false
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var audioRecord: AudioRecord
    private val _probabilityThreshold = MutableLiveData(0.5f)
    private val probabilityThreshold: LiveData<Float> get() = _probabilityThreshold
    private val _delay = MutableLiveData(500L)
    private val delay: LiveData<Long> = _delay
    private var categoriesToInclude = mutableListOf<String>()
    private var format: TensorAudio.TensorAudioFormat? = null
    private val result = MutableLiveData<String>()
    private var executor: ScheduledThreadPoolExecutor? = null
    private var thisAIModelsCategories = mutableListOf<Category>()
    private var _classificationResult = MutableLiveData<AudioClassificationResult>()
    private var classificationResult: LiveData<AudioClassificationResult> = _classificationResult
    private var audioClassifierListener: AudioClassifierListener? = null

    private val _classificationFlow = MutableSharedFlow<AudioClassificationResult>()
    val classificationFlow: SharedFlow<AudioClassificationResult> = _classificationFlow



    init {
        try {
            classifier = AudioClassifier.createFromFile(context, model)

            // Create the tensor variable that will store the recording for inference
            // and build the format specification for the recorder.
            tensorAudio = classifier.createInputTensorAudio()

            // create audio input objects
            format = classifier.requiredTensorAudioFormat
            audioRecord = classifier.createAudioRecord()

            // Get the categories from the AI-model
            tensorAudio.load(audioRecord)
            val output = classifier.classify(tensorAudio)
            extractLabels(output)
             includeAllCategories()


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Starts recording audio and classifying it in real-time.
     *
     * This function starts the "AudioRecord" object and creates a "ThreadPoolExecutor"
     * to run the classification logic on a separate thread. The "Handler" object is
     * used to schedule the classification task to run every 500 milliseconds.
     *
     * Note that this function does not block the calling thread. Instead, it schedules
     * the classification task to run on a separate thread using the "ThreadPoolExecutor".
     */
    override fun startRecording() {
        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        audioRecord.startRecording()
        isRecording = true
        /*
        executor = ScheduledThreadPoolExecutor(1)
        executor!!.scheduleAtFixedRate(
            classifyAudio(),
            0,
            500,
            TimeUnit.MILLISECONDS
        )

         */

        Log.e("recording", "startRecording: ", )
        CoroutineScope(Dispatchers.IO).launch {
            classifyAudio2()
        }

    }

    /**
     * Stops recording audio.
     *
     * This function stops the "AudioRecord" object from recording audio.
     */
    override fun stopRecording() {
        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
            return
        }
        isRecording = false
        audioRecord.stop()
        executor?.shutdown()
        Log.e("recording", "stopRecording: ", )
    }

    override fun excludeCategory(category: String) {
        Log.e("excludeCategory", "$category - yamnet")
        /*
        categoriesToInclude.removeIf {
            it == category
        }

         */

        thisAIModelsCategories.map {
            if(it.title == category) {
                it.isIncluded = false
                return@map
            }
        }
    }

    override fun includeCategory(category: String) {
        thisAIModelsCategories.map {
            if(it.title == category) {
                it.isIncluded = true
                return@map
            }
        }
    }

    override fun includeAllCategories() {
        thisAIModelsCategories.forEach { it.isIncluded = true }
        Log.e("switchClassifier", "included all Yamnet" )
    }

    override fun excludeAllCategories() {
        thisAIModelsCategories.forEach { it.isIncluded = false }
        Log.e("switchClassifier", "excluded all Yamnet" )
    }

    override fun setThreshold(threshold: Float) {
        _probabilityThreshold.postValue(threshold)
    }

    override fun setDelay(delay: Long) {
        _delay.postValue(delay)
    }

    override fun getDelay(): LiveData<Long> {
       return delay
    }

    /**
     * Gets the current classification result.
     *
     * This function returns the most recent classification result as a string.
     * If no classification has been performed yet, this function returns null.
     */
    override fun getResult(): LiveData<String> {
        return result
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
        this.audioClassifierListener = null
    }


    /**
     * Classifies the current audio recording and updates the classification result.
     *
     * This function loads the current audio recording into the "TensorAudio" object,
     * runs the classification model on the audio data, and updates the "result"
     * property with the classification result. The "probabilityThreshold" property is
     * used to filter out low-probability classifications.
     */
    private fun classifyAudio(): Runnable {
        return Runnable {
            val timestamp = System.currentTimeMillis()
            val numberOfSamples = tensorAudio.load(audioRecord)
            val output = classifier.classify(tensorAudio)

            val filteredModelOutput = output[0].categories.filter { category ->
                category.score > probabilityThreshold.value!! &&
                        categoriesToInclude.any { it == category.label }
            }

            val outputStr =
                filteredModelOutput.sortedBy { -it.score }
                    .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }


            val resultWithHighestScore = filteredModelOutput.maxByOrNull { it.score }

            if (resultWithHighestScore != null) {
                val categoryName = resultWithHighestScore.label.substringBefore(",")
                val specificType = resultWithHighestScore.label.substringAfter(",").trim()
                val audioClassificationResult = AudioClassificationResult(
                    timestamp,
                    categoryName,
                    specificType,
                    resultWithHighestScore.score
                )
                _classificationResult.postValue(audioClassificationResult)
                audioClassifierListener?.onAudioClassification(audioClassificationResult)


            }

            Log.e("classifyAudio", outputStr)
            result.postValue(outputStr)


        }

    }

    private suspend fun classifyAudio2() = withContext(Dispatchers.IO) {
        while (isActive && isRecording) {
            val timestamp = System.currentTimeMillis()

            val numberOfSamples = tensorAudio.load(audioRecord)
            val output = classifier.classify(tensorAudio)

            val filterByScore = output[0].categories.filter { category ->
                category.score >= probabilityThreshold.value!!
            }

            val filterByCategory = filterByScore.filter { category ->
                thisAIModelsCategories.any { includedCategory ->
                    category.label == includedCategory.title && includedCategory.isIncluded
                }
            }

            val outputStr =
                filterByCategory.sortedBy { -it.score }
                    .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }

            Log.e("classifyAudio", filterByScore.toString())
            val resultWithHighestScore = filterByCategory.maxByOrNull { it.score }

            if (resultWithHighestScore != null) {
                val categoryName = resultWithHighestScore.label.substringBefore(",")
                val specificType = resultWithHighestScore.label.substringAfter(",").trim()
                val audioClassificationResult = AudioClassificationResult(
                    timestamp,
                    categoryName,
                    specificType,
                    resultWithHighestScore.score
                )
                _classificationResult.postValue(audioClassificationResult)
                 flow {
                    emit(audioClassificationResult)
                }
                _classificationFlow.emit(audioClassificationResult)
                audioClassifierListener?.onAudioClassification(audioClassificationResult)



            }


            result.postValue(outputStr)
            delay(delay.value!!)

        }
    }

/*
    override fun getResultAsFlow(): Flow<AudioClassificationResult> {
        return classificationFlow
    }


 */
    override fun getResultAsFlow(): Flow<AudioClassificationResult> {
        return classificationFlow
    }

    override fun allCategoriesIncluded(): Boolean {
        for (category in thisAIModelsCategories) {
            if (!category.isIncluded) {
                Log.e("switchClassifier", category.title + "- Yamnet")
                return false
            }

        }
        return true
    }

    /**
     * Gets the list of available categories.
     *
     * This function returns the list of available categories sorted by name. If the categories have not
     * been loaded yet, this function returns null.
     */
    private fun extractLabels(classList: List<Classifications>) {
        // Iterate over the classifications in the output list
        for (classification in classList) {
            // Iterate over the categories in each classification, sorted by name
            for (category in classification.categories.sortedBy { it.label }) {
                // Add the label to the list of all categories
                thisAIModelsCategories.add(Category(category.label, true))
            }
        }
    }


}