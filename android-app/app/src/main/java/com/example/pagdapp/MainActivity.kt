package com.example.pagdapp

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pagdapp.adapters.MarkerAdapter
import com.example.pagdapp.databinding.ActivityMainBinding

import com.example.pagdapp.fragments.MapsFragment
import com.example.pagdapp.fragments.ReportAndGunFragment
import com.example.pagdapp.fragments.SettingsFragment
import com.example.pagdapp.models.Position
import com.example.pagdapp.models.dbModels.Gun
import com.example.pagdapp.repositories.mockupServer.SocketHandler
import com.example.pagdapp.viewModels.MainViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException

const val TAG = "MapsActivity"
const val TAG2 = "ApiServer"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val RECORD_PERMISSION = 100
        private const val COARSE_PERMISSION = 200
        private const val FINE_PERMISSION = 201
        private const val NOTIFICATION_PERMISSION = 300

    }


    private lateinit var binding: ActivityMainBinding
    private val gson: Gson = Gson()
    private var mSocket: Socket? = null
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle

    // Create an adapter for the RecyclerView
    private lateinit var markerAdapter : MarkerAdapter

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions.entries.forEach {
                handlePermissions(it.key)
                Log.e("DEBUG", "${it.key} = ${it.value}")
            }
        }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                // Close the navigation drawer if it is open
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else if (currentFragment !is MapsFragment) {
                // Set the Home menu item as selected on bottom navigation view
                binding.bottomNavigationView.selectedItemId = R.id.Home
            } else {
                // Handle the back button press as usual
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Bind the layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.item1 -> Toast.makeText(applicationContext, "Clicked 1", Toast.LENGTH_SHORT)
                    .show()
            }
            true
        }


        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.menu.getItem(2).isEnabled = false


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapsFragment.newInstance())
                .commitNow()
        }

        //Request permissions
        requestAllPermissions()

        //Initiate the buttons
        initButtons()


        val weaponTypes = mutableListOf(
            Gun("AK-47", "Assault Rifle"),
            Gun("Sig9", "Handgun")
        )


      //  val layoutManager = LinearLayoutManager(this)
      //  val recyclerView = view.findViewById<RecyclerView>(R.id.settings_recycler_view)
     //   recyclerView.layoutManager = layoutManager
/*
        mainViewModel.manualReports.observe(this) {
            markerAdapter = MarkerAdapter(it, weaponTypes)
        }


 */
/*
        binding.navView.findViewById<RecyclerView>(R.id.marker_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
         //   adapter = markerAdapter
        }


 */
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        mainViewModel.isSendingReports.observe(this) { isSending ->
            if (isSending) {
                menu?.findItem(R.id.toggle_report)?.setIcon(R.drawable.report_sending_on)
            } else {
                menu?.findItem(R.id.toggle_report)?.setIcon(R.drawable.report_sending_off)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }

        when (item.itemId) {
            R.id.toggle_report -> {
                mainViewModel.toggleReport()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handlePermissions(permission: String) {

        when (permission) {

            Manifest.permission.RECORD_AUDIO -> {
                requestRecordPermission()
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                requestFineLocation()
            }
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                requestCoarseLocation()
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                requestNotificationPermission()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        mSocket?.disconnect()
    }


    private fun sendThroughSocket() {
        if (!SocketHandler.getSocket().connected()) {
            Toast.makeText(this, "You must connect first", Toast.LENGTH_SHORT).show()
        }
        // SocketHandler.getSocket().emit("sendData", gson.toJson(markerList))
    }

    private var eventUpdate = Emitter.Listener { args ->

        if (args[0] != null) {

            val listType = object : TypeToken<List<Position>>() {}.type
            val list: List<Position?> = gson.fromJson(args[0].toString(), listType)


            runOnUiThread {
                val channelId = createNotificationChannel("my_channel_id", "My Channel")
                showNotification(channelId, "Alert!", "New gunshots in your area!")
            }
        }


    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "This is a notification channel for my app"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            return channelId
        }
        return ""
    }

    private fun showNotification(channelId: String, title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_add_location_alt_24)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission()
            return
        }
        notificationManager.notify(0, builder.build())
    }

    private fun requestCoarseLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //   onMapReady(mMap)
            return
        }

        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle("Coarse location needed")
                .setMessage(
                    "This permission is needed to allow localisation of potential gunshots." +
                            "Your location will be anonymous"
                )
                .setPositiveButton("Ok") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        COARSE_PERMISSION
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.create().show()

        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                COARSE_PERMISSION
            )

        }
    }

    private fun requestFineLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // onMapReady(mMap)
            return
        }

        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle("Fine location permission needed")
                .setMessage(
                    "This permission is needed to allow localisation of potential gunshots." +
                            "Your location will be anonymous"
                )
                .setPositiveButton("Ok") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        FINE_PERMISSION
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.create().show()

        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_PERMISSION
            )

        }
    }

    private fun requestRecordPermission() {

        // Check if the Record permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            AlertDialog.Builder(this)
                .setTitle("Record permission needed")
                .setMessage("This permission is needed to record the surrounding of potential gunshots")
                .setPositiveButton("Ok") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        RECORD_PERMISSION
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.create().show()

        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_PERMISSION
            )
        }

    }

    private fun requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            AlertDialog.Builder(this)
                .setTitle("Notification permission needed")
                .setMessage("This permission is needed to notify you of potential gunshots")
                .setPositiveButton("Ok") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.create().show()

        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION
            )
        }

    }

    private fun requestAllPermissions() {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }


    private fun loginProgress() {
        /*
        val progressBar = binding.progressBar
        val loginBtn = binding.btnConnect
        val cardBtn = binding.cardBtn
        loginBtn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        loginBtn.text = ""
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()
        mSocket?.on(Socket.EVENT_CONNECT) {

            runOnUiThread{
                progressBar.visibility = View.GONE
                loginBtn.text = "Connected"
                cardBtn.setBackgroundColor(Color.GREEN)

            }
        }

         */

    }

    private fun initButtons() {

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.Home -> {
                    fragmentNavigation(MapsFragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.Settings -> {
                    fragmentNavigation(SettingsFragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.Report -> {
                    fragmentNavigation(ReportAndGunFragment.newInstance())
                    return@setOnItemSelectedListener true
                }
                R.id.AI_Model -> {
                    Intent(this, TestActivity::class.java).also { intent ->
                        startActivity(intent)
                    }
                    return@setOnItemSelectedListener true
                }
                else -> {
                    return@setOnItemSelectedListener true
                }
            }
        }

        mainViewModel.isRecording.observe(this) { isRecording ->
            if (isRecording) {

                val color = Color.argb(170, 0, 255, 0)
                binding.fab.setImageResource(R.drawable.ic_mic_on)
                binding.fab.backgroundTintList = ColorStateList.valueOf(color)
            } else {
                val color = Color.argb(250, 254, 32, 32)
                binding.fab.setImageResource(R.drawable.ic_mic_off)
                binding.fab.backgroundTintList = ColorStateList.valueOf(color)
            }
        }

        binding.fab.setOnClickListener {
            mainViewModel.toggleRecording()
        }


    }

    private fun fragmentNavigation(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }

    private fun initEmitListener() {
        mSocket?.on("eventUpdate", eventUpdate)
    }

    private fun parseJsonArrayToPositions(jsonArray: JSONArray): List<Position> {
        val positions = mutableListOf<Position>()
        try {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val latitude = obj.getDouble("latitude")
                val longitude = obj.getDouble("longitude")
                val time = obj.getLong("time")
                positions.add(Position(latitude, longitude, time))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return positions
    }
}


