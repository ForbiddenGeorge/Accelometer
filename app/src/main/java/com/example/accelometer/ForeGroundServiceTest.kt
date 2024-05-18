package com.example.accelometer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ForeGroundServiceTest: Service(), GPSDataListener, SensorDataListener {
    private lateinit var selectedSensorsTypes: IntArray
    private var gpsEnabledValid: Boolean = false
    private lateinit var sensorHandlerThread: HandlerThread
    private lateinit var sensorHandler: Handler
    private var latency: Int? = 0

    override fun onGPSDataReceived(gpsData: Bundle) {
        /*val gpsDataNew = Bundle().apply {
            putDouble("latitude", latitude)
            putDouble("longitude", longitude)
            putDouble("altitude", altitude)
            putFloat("speed", speed)
            putLong("time", time)
        }
        sendGPSDataBroadcast(gpsDataNew)*/
        Log.d("GPS FOREGROUND", "Něco se děje")
    }

    override fun onSensorDataReceived(sensorData: SensorData) {
        //val sensorDataNew = sensorData.getFloatArray("values") // Corrected line

        sensorData.let {
            sendSensorDataBroadcast(sensorData)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStartCommand", "YES")
        createNotificationChannel()
        // Build the notification
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.combined_logo)
            .build()
        startForeground(1, notification)
        selectedSensorsTypes = intent?.getIntArrayExtra("selectedSensors") ?: intArrayOf()
        gpsEnabledValid = intent?.getBooleanExtra("gpsEnabled", false) ?:false
        latency = intent?.getIntExtra("latency", 0)
        if(selectedSensorsTypes.isNotEmpty()){
            val sensorManager = SensorManager(this)
            sensorManager.startSensorUpdates(selectedSensorsTypes,this, latency!!)
        }

        4
        if(gpsEnabledValid){
            val gpsManager = GPSManager(this)
        }
        //val gpsManager = GPSManager(this)
        //gpsManager.startGpsUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //GPSManager.stopGpsUpdates()
        val sensorManager = SensorManager(this)
        sensorManager.stopSensorUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private val CHANNEL_ID = "ForegroundServiceChannel"
        fun startService(context: Context, selectedSensors: IntArray, gpsEnabled: Boolean, latency: Int) { //Zavolá se
            val startIntent = Intent(context, ForeGroundServiceTest::class.java)
            startIntent.putExtra("selectedSensors", selectedSensors)
            startIntent.putExtra("gpsEnabled", gpsEnabled)
            startIntent.putExtra("latency", latency)
            // Put extras if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Zavolá se
                context.startForegroundService(startIntent)
            }
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForeGroundServiceTest::class.java)
            context.stopService(stopIntent)
        }
    }
    //Tady to pak bude napojené dál
    private fun sendGPSDataBroadcast(gpsData: Bundle) {
        val intent = Intent("GPS_DATA_ACTION")
        intent.putExtras(gpsData)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendSensorDataBroadcast(sensorData: SensorData) {
        val intent = Intent("SENSOR_DATA_ACTION")
        intent.putExtra("sensorData", sensorData)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

}