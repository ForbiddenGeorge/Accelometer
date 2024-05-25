package com.example.accelometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.ActivityCompat
import java.text.DecimalFormat

/**
* Class that manages location updates
 */
class GPSManager(private val context: Context, private val listener: GPSDataListener) {

    private var locationManager: LocationManager? = null
    private var handlerThread: HandlerThread
    private var handler: Handler
    private val decimalFormat = DecimalFormat("#.###")
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var satelliteCount: Int = 0
    private var usedSatellites: Int = 0
    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        handlerThread = HandlerThread("GPSHandlerThread").apply {
            start()
        }
        handler = Handler(handlerThread.looper)
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        //Checks if permissions are granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        //Starts location updates
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            50L, //Update interval in milliseconds
            0.15f, //Update distance in meters
            locationListener,
            handler.looper
        )
        // Register GNSS status callback
        if (gnssStatusCallback == null) {
            gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    usedSatellites = 0
                    satelliteCount = status.satelliteCount
                    for (i in 0 until status.satelliteCount) {
                        if (status.usedInFix(i)) {
                            usedSatellites++
                        }
                    }
                }
            }
            locationManager?.registerGnssStatusCallback(gnssStatusCallback!!, handler)
        }
    }

    fun stopGpsUpdates() {
        //Stops location and GNSS updates
        locationManager?.removeUpdates(locationListener)
        Log.d("GPSManager", "GPS updates stopped")
        gnssStatusCallback?.let {
            locationManager?.unregisterGnssStatusCallback(it)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            //Packaging and sending all the new data
            val gpsData = Bundle().apply {
                val altitude = decimalFormat.format(location.altitude).replace(',', '.').toDouble()
                var speed = (location.speed * 3.6).toFloat()
                    speed = decimalFormat.format(speed).replace(',', '.').toFloat()
                putDouble("latitude", location.latitude)
                putDouble("longitude", location.longitude)
                putDouble("altitude", altitude)
                putFloat("speed", speed)
                putLong("time", location.time)
                putInt("satelliteCount", satelliteCount)
                putInt("usedSatellites", usedSatellites)
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