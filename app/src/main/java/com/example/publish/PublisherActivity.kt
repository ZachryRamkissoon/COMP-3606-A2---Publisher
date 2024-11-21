package com.example.publish

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
import com.example.publish.R
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID
import org.json.JSONObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class PublisherActivity : AppCompatActivity() {
    private var client: Mqtt5BlockingClient? = null
    private var clientID = ""
    private var isEnabled = false

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_publish)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fused = LocationServices.getFusedLocationProviderClient(this);
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker816036717.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()

        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
            setMinUpdateIntervalMillis(5000L)
            try {
                client?.connect()

            } catch (e:Exception){
                Toast.makeText(this@PublisherActivity,"Error when connecting to broker", Toast.LENGTH_SHORT).show()
            }
        }.build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                for (location in result.locations) {
                    val textToSend = JSONObject().apply {
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                        put("id", clientID)
                        put("timestamp", location.time)
                        put("speed", location.speed)
                    }.toString()
                    try{
                        client?.publishWith()?.topic("assignment/location")?.payload(textToSend.toByteArray())?.send()
                        println(textToSend)
                    } catch (e : Exception){
                        Toast.makeText(this@PublisherActivity, "Error when sending data to broker", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPublish() {
        isEnabled = true;

        if (client?.state?.isConnected == false) {

            try {
                client?.connect()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PublisherActivity, "Error when connecting to broker", Toast.LENGTH_SHORT).show()
            }
        }

        fused.requestLocationUpdates(request, callback, Looper.getMainLooper()
        )
    }

    private fun stopPublish() {
        isEnabled = false
        if (client?.state?.isConnected == true) {
            try {
                client?.disconnect()
            } catch (e:Exception){
                Toast.makeText(this,"Error when disconnecting from broker", Toast.LENGTH_SHORT).show()
            }
        }

        fused.removeLocationUpdates(callback)
    }

    fun stopPublish(view: View) {
        stopPublish()
    }

    fun startPublish(view: View) {
        val et = findViewById<EditText>(R.id.sid)
        val studentID = et.text.toString()

        if (studentID.isEmpty()) {
            Toast.makeText(this, "Please enter your ID", Toast.LENGTH_SHORT).show()
            return
        }

        clientID = studentID
        startPublish()
    }

    override fun onPause() {
        super.onPause()
        if (isEnabled) {
            stopPublish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isEnabled) {
            startPublish()
        }
    }

}