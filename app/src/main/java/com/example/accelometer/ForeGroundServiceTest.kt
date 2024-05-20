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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.accelometer.R.color.da_blue

class ForeGroundServiceTest: Service(), GPSDataListener, SensorDataListener {
    private var sensorManager: SensorManager? = null
    private var gpsManager: GPSManager? = null
    private lateinit var selectedSensorsTypes: IntArray
    private var gpsEnabledValid: Boolean = false
    private lateinit var sensorHandlerThread: HandlerThread
    private lateinit var sensorHandler: Handler
    private var latency: Int? = 0
    private lateinit var wakeLock: PowerManager.WakeLock
    override fun onGPSDataReceived(gpsData: Bundle) {
        sendGPSDataBroadcast(gpsData)
        Log.d("GPS FOREGROUND", "Něco se děje")
    }

    override fun onSensorDataReceived(sensorData: SensorData) {
        //val sensorDataNew = sensorData.getFloatArray("values") // Corrected line
        sendSensorDataBroadcast(sensorData)
    }

    @SuppressLint("ResourceAsColor")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStartCommand", "YES")
        createNotificationChannel()
        // Build the notification
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Akcelerometr")
            .setContentText("Aktivní měření...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(da_blue)
            .setColorized(true)
            .build()
        startForeground(1, notification)
        selectedSensorsTypes = intent?.getIntArrayExtra("selectedSensors") ?: intArrayOf()
        gpsEnabledValid = intent?.getBooleanExtra("gpsEnabled", false) ?:false
        latency = intent?.getIntExtra("latency", 0)
        if (sensorManager == null) {
            sensorManager = SensorManager(this)
        }
        // Initialize gpsManager if it's null
        if (gpsManager == null) {
            gpsManager = GPSManager(this, this)
        }
        if(selectedSensorsTypes.isNotEmpty()){
            Log.d("Foreground Service", "Started sensor updates")
            sensorManager = SensorManager(this)
            sensorManager!!.startSensorUpdates(selectedSensorsTypes,this, latency!!)
        }

        if(gpsEnabledValid){
            Log.d("Foreground Service", "Started GPS updates")
            gpsManager = GPSManager(this, this)
            gpsManager!!.startGpsUpdates()
        }
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorService::WakeLock")
        wakeLock.acquire(100*60*1000L /*100 minutes*/)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //val gpsManager = GPSManager(this,this)
        if(gpsEnabledValid){
            gpsManager?.stopGpsUpdates()
            Log.d("Foreground Service", "Stoped GPS updates")
        }
        if(selectedSensorsTypes.isNotEmpty()){
            sensorManager?.stopSensorUpdates()
            Log.d("Foreground Service", "Stoped sensor updates")
        }
        wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val CHANNEL_ID = "ForegroundServiceChannel"
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
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.lightColor = da_blue
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

}