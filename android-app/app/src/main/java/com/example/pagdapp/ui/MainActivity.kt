package com.example.pagdapp.ui

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.pagdapp.R
import com.example.pagdapp.ui.adapters.MarkerAdapter
import com.example.pagdapp.databinding.ActivityMainBinding
import com.example.pagdapp.data.model.Position
import com.example.pagdapp.data.remote.socketServices.SocketService
import com.example.pagdapp.databinding.FragmentMapsBinding
import com.example.pagdapp.services.GunshotService
import com.example.pagdapp.services.TrackingService
import com.example.pagdapp.ui.fragments.MapsFragment
import com.example.pagdapp.ui.fragments.ReportAndGunFragment
import com.example.pagdapp.ui.fragments.SettingsFragment
import com.example.pagdapp.ui.viewModels.MainViewModel
import com.example.pagdapp.ui.viewModels.ReportViewModel
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.example.pagdapp.utils.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.example.pagdapp.utils.Constants.NOTIFICATION_PERMISSION_CODE
import com.example.pagdapp.utils.PermissionHandler
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.Socket
import io.socket.emitter.Emitter
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapBinding: FragmentMapsBinding
    private val gson: Gson = Gson()
    private var mSocket: Socket? = null
    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val reportViewModel: ReportViewModel by viewModels()

    private lateinit var toggle: ActionBarDrawerToggle

    private var currentFragment: Fragment? = null


    // Create an adapter for the RecyclerView
    private lateinit var markerAdapter: MarkerAdapter


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

        setTheme(R.style.Theme_TestApp)

        // Bind the layout
        mapBinding = FragmentMapsBinding.inflate(layoutInflater)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.menu.getItem(2).isEnabled = false


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapsFragment.newInstance())
                .commitNow()
        }

        //Request permissions
        permissionHandler.requestAllPermissions()

        //Initiate the buttons
        initButtons()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setupNavigationDrawerMenu()


        // Start gunshot service
        Intent(applicationContext, GunshotService::class.java).apply {
            action = GunshotService.ACTION_START
            startService(this)

        }

    }


    private fun setupNavigationDrawerMenu() {
        val classifierMenu = binding.navView.menu.findItem(R.id.display_ai_title).subMenu

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_pagd_legacy -> {
                    settingsViewModel.setActiveClassifier("PAGD Legacy model")
                }
                R.id.nav_item_yamnet -> {
                    settingsViewModel.setActiveClassifier("Yamnet model")

                }
            }
            settingsViewModel.updateCategories()
            true
        }

        val displaySettingsMenuItem = binding.navView.menu.findItem(R.id.display_settings)
        val displaySettingsActionView = displaySettingsMenuItem.actionView
        val listeningSettingsActionView =
            binding.navView.menu.findItem(R.id.setListening).actionView

        val switchListening =
            listeningSettingsActionView?.findViewById<SwitchMaterial>(R.id.widgetListening)
        val listeningSliderInterval =
            listeningSettingsActionView?.findViewById<Slider>(R.id.listeningSlider)
        val switchResult =
            displaySettingsActionView?.findViewById<SwitchMaterial>(R.id.widgetResult)
        val switchLocation =
            displaySettingsActionView?.findViewById<SwitchMaterial>(R.id.widgetLocation)

        mainViewModel.showLocation.observe(this) { showLocation ->
            switchLocation?.isChecked = showLocation
        }

        mainViewModel.showResult.observe(this) { showResult ->
            switchResult?.isChecked = showResult
        }


        switchResult?.setOnCheckedChangeListener { _, isChecked ->
            findViewById<View>(R.id.tvResult)?.isVisible = isChecked
            mainViewModel.setResult(isChecked)

        }

        switchLocation?.setOnCheckedChangeListener { _, isChecked ->
            findViewById<View>(R.id.tvLocation)?.isVisible = isChecked
            mainViewModel.setLocation(isChecked)

        }

        switchListening?.setOnClickListener {
            val switch = it as SwitchMaterial

            if (switch.isChecked) {
                Intent(applicationContext, GunshotService::class.java).apply {
                    action = GunshotService.ACTION_START
                    startService(this)

                }
            } else {
                Intent(applicationContext, GunshotService::class.java).apply {
                    action = GunshotService.ACTION_STOP
                    startService(this)

                }
            }
        }

        listeningSliderInterval?.addOnChangeListener { _, value, _ ->
            settingsViewModel.updateListeningInterval(value.toLong() * 1000L)
        }

        settingsViewModel.activeClassifier.observe(this) { classifierName ->
            classifierMenu?.forEach { item ->
                item.isChecked = classifierName == item.title
            }
        }

        settingsViewModel.isRunning.observe(this) { isRunning ->
            classifierMenu?.forEach { item ->
                item.isEnabled = !isRunning
            }
        }

        settingsViewModel.isListening.observe(this) { isListening ->
            switchListening?.isChecked = isListening
        }

        settingsViewModel.intervalDelay.observe(this) { listeningInterval ->
            listeningSliderInterval?.value = (listeningInterval / 1000).toFloat()
        }
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
            R.id.gunshots -> {
                openGunCalendar()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openGunCalendar() {

        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates for gunshots")
                .setSelection(
                    mainViewModel.dateFromToInMilli
                ).setPositiveButtonText("Save dates")
                .setNegativeButtonText("Cancel")
                .build()

        dateRangePicker.show(supportFragmentManager, "date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener {
            mainViewModel.setDateFromTo(it)
            reportViewModel.fetchSelectedGunshots()
        }


        dateRangePicker.addOnCancelListener {
            dateRangePicker.dismiss()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mSocket?.disconnect()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SHOW_MAP_FRAGMENT ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, MapsFragment.newInstance())
                    .commitNow()
        }
    }


    private fun sendThroughSocket() {
        if (!SocketService.getSocket().connected()) {
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
            permissionHandler.requestSinglePermission(NOTIFICATION_PERMISSION_CODE)
            return
        }
        notificationManager.notify(0, builder.build())
    }


    private fun initButtons() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            val newFragment = when (it.itemId) {
                R.id.Home -> MapsFragment()
                R.id.Settings -> SettingsFragment()
                R.id.Report -> ReportAndGunFragment()
                R.id.AI_Model -> {
                    Intent(this, TestActivity::class.java).also { intent ->
                        startActivity(intent)
                    }
                    return@setOnItemSelectedListener true
                }
                else -> null
            }

            if (newFragment != null && currentFragment?.javaClass != newFragment.javaClass) {
                fragmentNavigation(newFragment)
                currentFragment = newFragment
            }

            return@setOnItemSelectedListener true
        }

        settingsViewModel.isRunning.observe(this) { isRecording ->
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
            if (settingsViewModel.isRunning.value == true) {
                Intent(applicationContext, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_STOP
                    startService(this)
                }

            } else {
                if (permissionHandler.areAllPermissionsGranted()) {
                    Intent(applicationContext, TrackingService::class.java).apply {
                        action = TrackingService.ACTION_START
                        startService(this)
                    }

                } else {
                    permissionHandler.requestAllPermissions()
                }
            }
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


}


