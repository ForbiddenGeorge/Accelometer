package com.example.accelometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.ActivityCompat
import java.text.DecimalFormat

class GPSManager(private val context: Context, private val listener: GPSDataListener) {

    private var locationManager: LocationManager? = null
    private var handlerThread: HandlerThread
    private var handler: Handler
    private val decimalFormat = DecimalFormat("#.###")
    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handlerThread = HandlerThread("GPSHandlerThread").apply {
            start()
        }
        handler = Handler(handlerThread.looper)
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle permission request if not already granted
            return
        }

        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            50L, // Update interval in milliseconds
            0.15f, // Update distance in meters
            locationListener,
            handler.looper
        )
    }

    fun stopGpsUpdates() {
        locationManager?.removeUpdates(locationListener)
        Log.d("GPSManager", "GPS updates stopped")
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val gpsData = Bundle().apply {
                val altitude = decimalFormat.format(location.altitude).replace(',', '.').toDouble()
                var speed = (location.speed * 3.6).toFloat()
                    speed = decimalFormat.format(speed).replace(',', '.').toFloat()
                putDouble("latitude", location.latitude)
                putDouble("longitude", location.longitude)
                putDouble("altitude", altitude)
                putFloat("speed", speed)
                putLong("time", location.time)
            }
            listener.onGPSDataReceived(gpsData)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
        override fun onProviderEnabled(provider: String) {

        }
        override fun onProviderDisabled(provider: String) {

        }
    }
}