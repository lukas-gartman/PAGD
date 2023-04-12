package pagd.signalprocessing

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.material.snackbar.Snackbar
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import pagd.signalprocessing.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        checkPermission()

        val options = TfLiteInitializationOptions.builder()
            .setEnableGpuDelegateSupport(true)
            .build()
        TfLiteVision.initialize(this, options)

        /* Load model */
        val fileDescriptor = assets.openFd("signature_model.tflite");
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor);
        val fileChannel = inputStream.channel;
        val startOffset = fileDescriptor.startOffset;
        val declaredLength = fileDescriptor.declaredLength;
        val filemap =  fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        val signature_interpreter = Interpreter(filemap)

        /* Load model */
        val fileDescriptor2 = assets.openFd("model.tflite");
        val inputStream2 = FileInputStream(fileDescriptor2.fileDescriptor);
        val fileChannel2 = inputStream2.channel;
        val startOffset2 = fileDescriptor2.startOffset;
        val declaredLength2 = fileDescriptor2.declaredLength;
        val filemap2 =  fileChannel2.map(FileChannel.MapMode.READ_ONLY, startOffset2, declaredLength2);

        val model_interpreter = Interpreter(filemap2)

        val f = AudioProcessor(signature_interpreter, model_interpreter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            println("Permission already granted")
        }
    }
}

class AudioProcessor(private val signature_interpreter: Interpreter, private val model_interpreter: Interpreter) {
    /* Audio record parameters */
    val SAMPLE_RATE = 8000
    val FRAME_SAMPLES = 16384

    /* Other parameters not needing to be changed */
    val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    val RECORDER_BUFFER_SIZE = SAMPLE_RATE * 10

    /* Create needed objects */

    private val buffer = FloatArray(FRAME_SAMPLES)
    private val full_buffer = FloatArray(FRAME_SAMPLES)

    private val inputs = java.util.HashMap<String, Any>() //Input hashmap to spectrogram signature
    private val outputs = java.util.HashMap<String, Any>() //output hashmap to spectrogram signature
    //Output tensor for spectrogram signature
    private val output_tensor = Array<Array<FloatArray>>(256) {Array<FloatArray>(257) { floatArrayOf(1.0f) }}

    @SuppressLint("MissingPermission")
    private val audio_recorder = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, RECORDER_BUFFER_SIZE)

    //Now start the audio recording
    init {
        outputs["spectrogram"] = output_tensor //Add output tensor to output hashmap

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

    //Function that thread runs
    private fun thread_work() {
        while(true){
            /* Get new audio frame */
            val read_n = audio_recorder.read(buffer, 0, FRAME_SAMPLES,
                AudioRecord.READ_NON_BLOCKING)
            //println(read_n)

            val timestamp = System.currentTimeMillis()

            /* Shift full buffer */
            for (i in 0 .. FRAME_SAMPLES-read_n-1)
                full_buffer[i] = full_buffer[i+read_n]

            /* Add frame to full buffer */
            for (i in 0 .. read_n-1)
                full_buffer[FRAME_SAMPLES-read_n+i] = buffer[i] * 32767

            inputs["wave"] = full_buffer //Set audio wave as input
            signature_interpreter.runSignature(inputs, outputs, "spectrogram") //Run spectrogram function

            val spectrogram = outputs["spectrogram"] as Array<Array<FloatArray>>
/*
            for (i in spectrogram.indices) {
                for (j in spectrogram[i].indices) {
                    print(spectrogram[i][j][0])
                    print(" ")
                }
                println("")
            }

            //Thread.sleep(1_000)
*/

            val output2 = arrayOf(floatArrayOf(1.0f))
            model_interpreter.run(arrayOf(spectrogram), output2)
            println(output2[0][0])
        }
    }
}
