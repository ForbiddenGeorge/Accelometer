package com.example.accelometer

import android.os.Bundle

/**
* Listeners/Interfaces for GPS and Sensor data updates
*/
interface GPSDataListener {
    fun onGPSDataReceived(gpsData: Bundle)
}

interface SensorDataListener {
    fun onSensorDataReceived(sensorData: SensorData)
}