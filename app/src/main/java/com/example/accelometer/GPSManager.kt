package com.example.accelometer

import android.os.Bundle

class GPSManager(private val gpsDataListener: GPSDataListener) {




    private fun sendGPSData(gpsData: Bundle) {
        gpsDataListener.onGPSDataReceived(gpsData)
    }

}