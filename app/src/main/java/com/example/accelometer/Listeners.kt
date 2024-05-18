package com.example.accelometer

import android.os.Bundle

interface GPSDataListener {
    fun onGPSDataReceived(gpsData: Bundle)
}

interface SensorDataListener {
    fun onSensorDataReceived(sensorData: SensorData)
}