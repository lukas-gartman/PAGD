package com.example.testapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.testapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.util.*

const val TAG = "MapsActivity"
const val TAG2 = "ApiServer"


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var markedLocation: Location? = null
    private lateinit var serverApi: RetrofitInstance
    private val markerList = mutableListOf<Position>()
    private val gson: Gson = Gson()
    private var mSocket: Socket? = null
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Instantiate the server api
        val serverApi = RetrofitInstance.getInstance()


        // The following lines connects the Android app to the server.
        binding.btnConnect.setOnClickListener {
            SocketHandler.setSocket()
            loginProgress()


            // Register listener
            initEmitListener()

            CoroutineScope(Dispatchers.IO).launch {

            }

        }



    }

    private fun initEmitListener() {

        mSocket?.on("eventUpdate", eventUpdate)
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                baseContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                baseContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    lastLocation = location
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(
                        MarkerOptions().position(currentLocation).title("Current Location")
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                    val message = "Your location: ${location.latitude}, ${location.longitude}"
                    binding.position.text = message
                }
            }

        val serverApi = RetrofitInstance.getInstance()
        placeMarker(googleMap)


        binding.sendBtn.setOnClickListener {
            //sendLocation(serverApi)
            sendThroughSocket()
        }

        binding.getBtn.setOnClickListener {
            getLocation(serverApi)
        }

        binding.clearBtn.setOnClickListener {
            clearMarkers(serverApi)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mSocket?.disconnect()
    }

    private fun getLocation(serverApi: ServerApi) {
        val call = serverApi.getPosition()
        call.enqueue(object : Callback<List<Position>> {
            override fun onResponse(
                call: Call<List<Position>>,
                response: Response<List<Position>>
            ) {

                for (marker in response.body()!!) {
                    val currentMark = LatLng(marker.latitude, marker.longitude)
                    mMap.addMarker(MarkerOptions().position(currentMark))
                }
            }

            override fun onFailure(call: Call<List<Position>>, t: Throwable) {
                Log.e("Get Request", "Error: ${t.message}")
            }

        })
    }

    private fun sendLocation(serverApi: ServerApi) {
        if (markerList.size == 0) {
            return
        }
        /*
            val currentMarker = Position(
                markedLocation!!.latitude,
                markedLocation!!.longitude,
                markedLocation!!.time)


         */
        // launch the api request on a background thread
        val call = serverApi.sendPosition(markerList)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@MapsActivity,
                        "sent ${markerList.size} locations",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("POSTREQUEST", "Error: ${t.message}")
            }

        })

    }

    private fun clearMarkers(serverApi: ServerApi) {
        val call = serverApi.deletePositions()
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    markerList.clear()
                    Toast.makeText(this@MapsActivity, response.body(), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("Delete request", "Error: ${t.message}")
            }

        })
        mMap.clear()
        markedLocation = null
    }

    private fun placeMarker(googleMap: GoogleMap) {


        // Add click listener to the map
        mMap.setOnMapClickListener { latLng ->

            val simpleDateFormat = SimpleDateFormat("HH:mm:ss:ms", Locale.getDefault())
            val currentTime = Calendar.getInstance().timeInMillis
            val currentTimeString = simpleDateFormat.format(Date(currentTime))

            // Add marker on the map at the location where the user clicked
            val marker: Marker? = mMap.addMarker(
                MarkerOptions().position(latLng)
                    .title("Marker")
                    .snippet("lat: ${latLng.latitude}, lng: ${latLng.longitude}")

            )

            if (marker != null) {
                markerList.add(
                    Position(
                        marker.position.latitude,
                        marker.position.longitude,
                        currentTime
                    )
                )
            }



            markedLocation = Location("")
            if (marker != null) {
                markedLocation!!.latitude = marker.position.latitude
                markedLocation!!.longitude = marker.position.longitude
                markedLocation!!.time = System.currentTimeMillis()
            }
        }
    }

    private fun sendThroughSocket() {
        if(!SocketHandler.getSocket().connected()){
            Toast.makeText(this, "You must connect first", Toast.LENGTH_SHORT).show()
        }
        SocketHandler.getSocket().emit("sendData", gson.toJson(markerList))
    }

    private var eventUpdate = Emitter.Listener { args ->

        if (args[0] != null) {

            val listType = object : TypeToken<List<Position>>() {}.type
            val list : List<Position?> = gson.fromJson(args[0].toString(), listType)


            runOnUiThread {
                val channelId = createNotificationChannel("my_channel_id", "My Channel")
                showNotification(channelId, "Alert!", "New gunshots in your area!")
            }
        }


    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "This is a notification channel for my app"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            return channelId
        }
        return ""
    }

    private fun showNotification(channelId: String, title: String, message: String) {
        val intent = Intent(this, MapsActivity::class.java)
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(0, builder.build())
    }

     private fun loginProgress(){
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

/*
         withContext(Dispatchers.IO) {
             val job = withTimeoutOrNull(15000L) {
                 this@MapsActivity.runOnUiThread {
                     loginBtn.text = ""
                 }

                 SocketHandler.establishConnection()
             }

             if(job == null){
                 this@MapsActivity.runOnUiThread {
                     loginBtn.text = "Failed"
                     progressBar.visibility = View.GONE
                     loginBtn.isEnabled = true
                 }


             }
         }

 */
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


