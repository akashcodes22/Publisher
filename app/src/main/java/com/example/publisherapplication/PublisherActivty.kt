package com.example.publisherapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID

class PublisherActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var client: Mqtt5BlockingClient? = null
    private var isLocationUpdatesActive = false
    private var clientID = ""


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_publisher)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
            setMinUpdateIntervalMillis(5000L) // Minimum time interval for updates
            try {
                client?.connect()
            } catch (e:Exception){
                Toast.makeText(this@PublisherActivity,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
            }
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    println("Latitude: $latitude, Longitude: $longitude")
                    val textToSend = "Latitude: $latitude, Longitude: $longitude"

                    try{
                        client?.publishWith()?.topic("assignment/location")?.payload(textToSend.toByteArray())?.send()
                    } catch (e : Exception){
                        Toast.makeText(this@PublisherActivity, "An error occurred when sending a message to the broker", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        if(isLocationUpdatesActive) {
            stopLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if(isLocationUpdatesActive) {
            startLocationUpdates()
        }
    }

    fun stopLocationUpdates(view: View) {
        stopLocationUpdates()
    }

    fun startLocationUpdates(view: View) {
        val inputField = findViewById<EditText>(R.id.studentID_Input)
        val textInput = inputField.text.toString()

        if (textInput.isEmpty()) {
            Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show()
            return
        }

        clientID = textInput
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        isLocationUpdatesActive = true;
        if (client?.state?.isConnected == false) {
            try {
                client?.connect()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PublisherActivity,
                    "An error occurred when connecting to broker",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    fun stopLocationUpdates() {
        isLocationUpdatesActive = false
        if (client?.state?.isConnected == true) {
            try {
                client?.disconnect()
            } catch (e:Exception){
                Toast.makeText(this,"An error occurred when disconnecting from broker", Toast.LENGTH_SHORT).show()
            }
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}