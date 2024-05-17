package com.example.accelometer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.IOException

class LocationService: Service(){

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_START_GPS -> startGPS()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(){
        val notification = NotificationCompat.Builder(this,"location")
            .setContentTitle("Akcelerometr")
            .setContentText("Aktivní měření")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        startForeground(1, notification.build())
    }

    private fun startGPS(){
        val notification = NotificationCompat.Builder(this,"location")
            .setContentTitle("Akcelerometr")
            .setContentText("Aktivní měření GPS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        startForeground(1, notification.build())
    }

    private fun stop(){
        try{
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(START_STICKY_COMPATIBILITY)
            Log.d("I TADY JSEM", "BOHOOHOHO")
        }catch (e: IOException){
            Log.e("ERROR při STOPU", e.message.toString())
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_GPS = "ACTION_START_GPS"
    }
}