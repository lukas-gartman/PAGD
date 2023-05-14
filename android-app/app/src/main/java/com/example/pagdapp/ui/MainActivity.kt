package com.example.pagdapp.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.pagdapp.R
import com.example.pagdapp.databinding.ActivityMainBinding
import com.example.pagdapp.databinding.FragmentMapsBinding
import com.example.pagdapp.services.LocationService
import com.example.pagdapp.services.TrackingService
import com.example.pagdapp.ui.adapters.MarkerAdapter
import com.example.pagdapp.ui.fragments.MapsFragment
import com.example.pagdapp.ui.fragments.ReportAndGunFragment
import com.example.pagdapp.ui.fragments.SettingsFragment
import com.example.pagdapp.ui.viewModels.MainViewModel
import com.example.pagdapp.ui.viewModels.ReportViewModel
import com.example.pagdapp.ui.viewModels.SettingsViewModel
import com.example.pagdapp.utils.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.example.pagdapp.utils.IntentUtils
import com.example.pagdapp.utils.PermissionHandler
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapBinding: FragmentMapsBinding
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


        handleIntent(intent)

        //Request permissions
        permissionHandler.requestAllPermissions()

        //Initiate the buttons
        initButtons()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setupNavigationDrawerMenu()

        // Start service if API is 28 or higher
        if (Build.VERSION.SDK_INT >= 28 && settingsViewModel.isRunning.value == false) {
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                startService(this)

            }
        }


        FirebaseMessaging.getInstance().subscribeToTopic("all");
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val gunshotData = intent?.let { IntentUtils.createGunShotFromIntent(it) }
        if (intent?.action == ACTION_SHOW_MAP_FRAGMENT && gunshotData != null) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            mainViewModel.updateClickGunshotNotification(gunshotData)
            if (currentFragment !is MapsFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, MapsFragment.newInstance())
                    .commitNow()


                binding.bottomNavigationView.selectedItemId = R.id.Home
            }
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapsFragment.newInstance())
                .commitNow()

            binding.bottomNavigationView.selectedItemId = R.id.Home
        }
    }


    private fun fragmentNavigation(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }


    private fun setupNavigationDrawerMenu() {
        val classifierMenu = binding.navView.menu.findItem(R.id.display_ai_title).subMenu

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_pagd_legacy_5 -> {
                    settingsViewModel.setActiveClassifier("PAGD Legacy model 5")
                }
                R.id.nav_item_pagd_legacy_8 -> {
                    settingsViewModel.setActiveClassifier("PAGD Legacy model 8")
                }
                R.id.nav_item_pagd_legacy_9 -> {
                    settingsViewModel.setActiveClassifier("PAGD Legacy model 9")
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

        val switchHeatmap =
            displaySettingsActionView?.findViewById<SwitchMaterial>(R.id.widgetHeatmap)

        mainViewModel.showLocation.observe(this) { showLocation ->
            switchLocation?.isChecked = showLocation
        }

        mainViewModel.showResult.observe(this) { showResult ->
            switchResult?.isChecked = showResult
        }

        mainViewModel.showHeatmap.observe(this) { showHeatmap ->
            switchHeatmap?.isChecked = showHeatmap
        }


        switchResult?.setOnCheckedChangeListener { _, isChecked ->
            findViewById<View>(R.id.tvResult)?.isVisible = isChecked
            mainViewModel.setResult(isChecked)

        }

        switchLocation?.setOnCheckedChangeListener { _, isChecked ->
            findViewById<View>(R.id.tvLocation)?.isVisible = isChecked
            mainViewModel.setLocation(isChecked)

        }

        switchHeatmap?.setOnCheckedChangeListener { _, isChecked ->
            mainViewModel.toggleHeatmap(isChecked)
        }

        switchListening?.setOnClickListener {
            val switch = it as SwitchMaterial

            if (switch.isChecked) {
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)

                }
            } else {
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
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

        dateRangePicker.addOnPositiveButtonClickListener { dateRange ->
            val startDate = dateRange.first
            var endDate = dateRange.second

            val calendar = Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            endDate = calendar.timeInMillis

            mainViewModel.setDateFromTo(Pair(startDate, endDate))
            reportViewModel.fetchSelectedGunshots()
        }



        dateRangePicker.addOnCancelListener {
            dateRangePicker.dismiss()
        }
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


}


