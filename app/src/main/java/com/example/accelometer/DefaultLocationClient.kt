package com.example.accelometer

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch


data class LocationWithSatelliteCount(
    val location: Location,
    val satelliteCount: Int,
    val timestamp: Long
)
class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if(!context.hasLocationPermission()){
                throw LocationClient.LocationException("Missing location permission")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if(!isGpsEnabled && isNetworkEnabled) {
                throw LocationClient.LocationException("GPS not turned on")
            }

            val request = LocationRequest.Builder(interval)

            val locationCallback = object : LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.requestLocationUpdates(
    builder: LocationRequest.Builder,
    locationCallback: LocationCallback,
    mainLooper: Looper?
) {
    val request = builder.build()
    // Start location updates
    this.requestLocationUpdates(request, locationCallback, mainLooper)
}
/*
@SuppressLint("MissingPermission")
fun getSatelliteCount(context: Context, executor: Executor, callback: (Int) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gnssStatusListener = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                // This method is called when the satellite status changes
                // You can get the count of satellites from the status object
                callback(status.satelliteCount)
            }
        }
        // Register the listener to receive satellite status updates
        locationManager.registerGnssStatusCallback(executor, gnssStatusListener)
    }
}*/