package com.example.accelometer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
* Class for creating foreground service, notification channel, and starting and stopping data collection
 */

class ForeGroundService: Service(), GPSDataListener, SensorDataListener {
    private var sensorManager: SensorManager? = null
    private var gpsManager: GPSManager? = null
    private lateinit var selectedSensorsTypes: IntArray
    private var gpsEnabledValid: Boolean = false
    private var latency: Int? = 0
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onGPSDataReceived(gpsData: Bundle) {
        //Passes collected data to MeasureActivity
        sendGPSDataBroadcast(gpsData)
    }

    override fun onSensorDataReceived(sensorData: SensorData) {
        //Passes collected data to MeasureActivity
        sendSensorDataBroadcast(sensorData)
    }
    @SuppressLint("ResourceAsColor")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStartCommand", "Started")

        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Akcelerometr")
            .setContentText("Aktivní měření...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(R.color.da_blue)
            .setColorized(true)
            .build()
        startForeground(1, notification)

        selectedSensorsTypes = intent?.getIntArrayExtra("selectedSensors") ?: intArrayOf()
        gpsEnabledValid = intent?.getBooleanExtra("gpsEnabled", false) ?: false
        latency = intent?.getIntExtra("latency", 0)

        if (sensorManager == null) {
            sensorManager = SensorManager(this)
        }

        if (gpsManager == null) {
            gpsManager = GPSManager(this, this)
        }

        if (selectedSensorsTypes.isNotEmpty()) {
            Log.d("Foreground Service", "Started sensor updates")
            sensorManager = SensorManager(this)
            sensorManager!!.startSensorUpdates(selectedSensorsTypes, this, latency!!)
        }

        if (gpsEnabledValid) {
            Log.d("Foreground Service", "Started GPS updates")
            gpsManager = GPSManager(this, this)
            gpsManager!!.startGpsUpdates()
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorService::WakeLock")
        wakeLock.acquire(1000 * 60 * 1000L /*1000 minutes*/)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //Closes those services that are currently active
        if (gpsEnabledValid) {
            gpsManager?.stopGpsUpdates()
            Log.d("Foreground Service", "Stopped GPS updates")
        }
        if (selectedSensorsTypes.isNotEmpty()) {
            sensorManager?.stopSensorUpdates()
            Log.d("Foreground Service", "Stopped sensor updates")
        }
        wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        fun startService(context: Context, selectedSensors: IntArray, gpsEnabled: Boolean, latency: Int) {
            //Passes info about what sensors to enable to the intent and begins the foreground service
            val startIntent = Intent(context, ForeGroundService::class.java)
            startIntent.putExtra("selectedSensors", selectedSensors)
            startIntent.putExtra("gpsEnabled", gpsEnabled)
            startIntent.putExtra("latency", latency)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopService(context: Context) {
            // Stops the foreground service
            val stopIntent = Intent(context, ForeGroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    private fun sendGPSDataBroadcast(gpsData: Bundle) {
        //Passes collected data to MeasureActivity
        val intent = Intent("GPS_DATA_ACTION")
        intent.putExtras(gpsData)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendSensorDataBroadcast(sensorData: SensorData) {
        //Passes collected data to MeasureActivity
        val intent = Intent("SENSOR_DATA_ACTION")
        intent.putExtra("sensorData", sensorData)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        //Creates notification channel, shows persistent notification to the user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            serviceChannel.lightColor = R.color.da_blue
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}