package com.example.pagdapp.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pagdapp.databinding.ActivityTestBinding

import com.example.pagdapp.data.model.waveRecorder.WaveRecorder
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.io.IOException
import java.util.*


class TestActivity : AppCompatActivity() {

    private var isRecording = false
    private var hasStopped = false
    private lateinit var binding: ActivityTestBinding
    private lateinit var waveRecorder: WaveRecorder
    private lateinit var classifier : AudioClassifier
    private lateinit var tensorAudio: TensorAudio

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var modelPath = "coral-model_yamnet_classification_coral_1.tflite"
    private var model2Path = "lite-model_yamnet_classification_tflite_1.tflite"
    var probabilityThreshold: Float = 0.3f




    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.e("DEBUG", result.resultCode.toString())
        } else {
            // The permission is not granted, show an error message
            Log.e("DEBUG", result.resultCode.toString())
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions.entries.forEach {
                Log.e("DEBUG", "${it.key} = ${it.value}")
            }
        }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonStopRecording.isEnabled = false

        /**
         * This path points to application cache directory.
         * you could change it based on your usage
         */
        //  val filePath:String = externalCacheDir?.absolutePath + "/audioFile.wav"
       // val dirPath = "$externalMediaDirs/audioFile.wav"
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss:ms", Locale.getDefault())
        val currentTime = Calendar.getInstance().timeInMillis
        val currentTimeString = simpleDateFormat.format(Date(currentTime))

        val fileName = "/recording_$currentTimeString"
         val dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
      //  val dirPath = Environment.getExternalStorageDirectory().absolutePath

        val waveRecorder = WaveRecorder(applicationContext,dirPath + fileName)

        // TensorFlow ...

      //  val model = CoralModelYamnetClassificationCoral1.newInstance(this)

        // Releases model resources if no longer used.
      //  model.close()
        try {
            // Initialization

            classifier = AudioClassifier.createFromFile(applicationContext, model2Path)
        } catch (e : IOException) {
            e.printStackTrace()
        }





        //Create the audio recorder and start recording
        val record = classifier.createAudioRecord()


        binding.buttonStartRecording.setOnClickListener {
            if (!hasPermission()) {
                requestPermissions()
                Toast.makeText(this, "Permissions needed", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
              //  waveRecorder.startRecording()



                record.startRecording()

                // Create the tensor variable that will store the recording for inference
                // and build the format specification for the recorder.
                tensorAudio = classifier.createInputTensorAudio()

                // Show the audio recorder specs that were defined by the model's metadata in the previous step.
                val format = classifier.requiredTensorAudioFormat
                val recorderSpecs = "Number Of Channels: ${format.channels}\n" +
                       "Sample Rate: ${format.sampleRate}"

                binding.recorderSpecsTextView.text = recorderSpecs

                isRecording = true
                binding.buttonStopRecording.isEnabled = isRecording

                val handler = Handler(Looper.getMainLooper())

                handler.postDelayed(object : Runnable {
                    override fun run() {

                        var finaloutput = mutableListOf<org.tensorflow.lite.support.label.Category>()


                        val numberOfSamples = tensorAudio.load(record)
                        val output = classifier.classify(tensorAudio)




                        val filteredModelOutput = output[0].categories.filter {
                            it.score > probabilityThreshold
                        }


                        val outputStr =
                            filteredModelOutput.sortedBy { -it.score }
                                .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }

                        Log.e("MYOUTPUT1", output.toString())


                        if (outputStr.isNotEmpty())
                            runOnUiThread {
                                binding.outputStr.text = outputStr
                            }

                        if (isRecording) {
                            handler.postDelayed(this, 500)
                        }
                    }
                }, 1)
            }
        }



        binding.buttonStopRecording.setOnClickListener {
            if (!isRecording) {
                Toast.makeText(this, "You are not recording", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
           // waveRecorder.stopRecording()
            record.stop()
            isRecording = false
            binding.buttonStopRecording.isEnabled = isRecording
        }

        binding.buttonPauseRecording.setOnClickListener {
            waveRecorder.pauseRecording()
        }

        // Add these lines of code in onCreate() method
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "We Have Permission", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You Denied the permission", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "You Denied the permission", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun hasPermission(): Boolean {
        val hasPermission: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPermission =
                (
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED
                                && Environment.isExternalStorageManager())
        } else {
            hasPermission =
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                        &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                        == PackageManager.PERMISSION_GRANTED)
        }

        return hasPermission
    }

    private fun requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse(String.format("package:%s", arrayOf(applicationContext.packageName)))
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO
            )
            requestPermissionLauncher.launch(intent)
            requestMultiplePermissions.launch(permissions)

        } else {
            val permissions =  arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            requestMultiplePermissions.launch(permissions)
        }

    }

}