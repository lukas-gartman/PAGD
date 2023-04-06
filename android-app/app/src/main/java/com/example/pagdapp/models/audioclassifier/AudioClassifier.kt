package com.example.pagdapp.models.audioclassifier

import android.content.Context
import android.media.AudioRecord
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.IOException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
class AudioClassifier(context: Context, model: String) : IAudioClassifier {

    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var audioRecord: AudioRecord
    private val _probabilityThreshold = MutableLiveData(0.5f)
    private val probabilityThreshold: LiveData<Float>
        get() = _probabilityThreshold
    private var categoriesToInclude = mutableListOf<String>()
    private var format: TensorAudio.TensorAudioFormat? = null
    private val result = MutableLiveData<String>()
    private var executor: ScheduledThreadPoolExecutor? = null
    private var thisAIModelsCategories = mutableListOf<String>()
    private var _classificationResult = MutableLiveData<AudioClassificationResult>()
    private var classificationResult: LiveData<AudioClassificationResult> = _classificationResult
    private var audioClassifierListener : AudioClassifierListener? = null

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


        } catch (e: IOException) {
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

        executor = ScheduledThreadPoolExecutor(1)
        executor!!.scheduleAtFixedRate(
            classifyAudio(),
            0,
            500,
            TimeUnit.MILLISECONDS
        )

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
        audioRecord.stop()
        executor?.shutdown()
    }

    override fun excludeCategory(category: String) {
        categoriesToInclude.removeIf {
            it == category
        }
    }

    override fun includeCategory(category: String) {
        categoriesToInclude.add(category)
    }

    override fun includeAllCategories() {
        categoriesToInclude.clear()
        for (category in thisAIModelsCategories) {
            categoriesToInclude.add(category)
        }

    }

    override fun excludeAllCategories() {
        categoriesToInclude.clear()

    }

    override fun setThreshold(threshold: Float) {
        _probabilityThreshold.postValue(threshold)
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


    override fun getCategories(): List<String> {
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
                    categoryName,
                    specificType,
                    resultWithHighestScore.score
                )
                _classificationResult.postValue(audioClassificationResult)
                audioClassifierListener?.onAudioClassification(audioClassificationResult)


            }

            result.postValue(outputStr)


        }

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
                thisAIModelsCategories.add(category.label)
            }
        }
    }



}