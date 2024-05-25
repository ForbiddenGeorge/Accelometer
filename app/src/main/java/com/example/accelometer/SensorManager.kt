package com.example.accelometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.text.DecimalFormat

/**
 * Class for managing sensor registering/unregistering and proper sensor data formatting
 */
class SensorManager(private val context: Context): SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorListener: SensorDataListener
    private var registeredSensors: MutableList<Sensor?> = mutableListOf()
    private val lock = Any()
    private lateinit var accelerometerSensor: Sensor
    private lateinit var linearAccelerometerSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
    private val sensorDataArray = FloatArray(9) { 0.0f }
    private val decimalFormat = DecimalFormat("#.######")

    fun startSensorUpdates(selectedSensorTypes: IntArray, dataListener: SensorDataListener, latency: Int) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //To ensure all sensors from previous measurement are unregistered
        stopSensorUpdates()
        sensorListener = dataListener
        registerSensors(selectedSensorTypes,latency)
    }
    private fun registerSensors(selectedSensorTypes: IntArray, latency: Int) {
        //register selected sensors for measurement
        for (sensor in selectedSensorTypes) {
            if (sensor == Sensor.TYPE_LINEAR_ACCELERATION) {
                linearAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
                sensorManager.registerListener(this, linearAccelerometerSensor, latency)
                registeredSensors.add(linearAccelerometerSensor)
                Log.d("Sensors", "Registered linear")
            }
            if (sensor == Sensor.TYPE_ACCELEROMETER) {
                accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
                sensorManager.registerListener(this, accelerometerSensor, latency)
                registeredSensors.add(accelerometerSensor)
                Log.d("Sensors", "Registered accel")
            }
            if (sensor == Sensor.TYPE_GYROSCOPE) {
                gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
                sensorManager.registerListener(this, gyroscopeSensor, latency)
                registeredSensors.add(gyroscopeSensor)
                Log.d("Sensors", "Registered gyro")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.w("Sensor Accuracy", "Sensor accuracy has changed")
    }
    override fun onSensorChanged(event: SensorEvent) {
        //Save the updated data to correct place in the array
        if(event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION){
            sensorDataArray[0] = decimalFormat.format(event.values[0]).replace(',', '.').toFloat()
            sensorDataArray[1] = decimalFormat.format(event.values[1]).replace(',', '.').toFloat()
            sensorDataArray[2] = decimalFormat.format(event.values[2]).replace(',', '.').toFloat()
        }
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            sensorDataArray[3] = decimalFormat.format(event.values[0]).replace(',', '.').toFloat()
            sensorDataArray[4] = decimalFormat.format(event.values[1]).replace(',', '.').toFloat()
            sensorDataArray[5] = decimalFormat.format(event.values[2]).replace(',', '.').toFloat()
        }
        if(event.sensor.type == Sensor.TYPE_GYROSCOPE){
            sensorDataArray[6] =decimalFormat.format(event.values[0]).replace(',', '.').toFloat()
            sensorDataArray[7] =decimalFormat.format(event.values[1]).replace(',', '.').toFloat()
            sensorDataArray[8] =decimalFormat.format(event.values[2]).replace(',', '.').toFloat()
        }
        val sensorData = SensorData(sensorDataArray)
        notifySensorData(sensorData)
    }

    fun stopSensorUpdates() {
        //Unregister all the registered sensors
        synchronized(lock) {
            sensorManager.let {
                for (sensor in registeredSensors) {
                    Log.d("SensorManager", "Unregistering sensor: ${sensor?.name}")
                    it.unregisterListener(this, sensor)
                }
                Log.d("SensorManager", "Registered Sensor Count Before Clear: ${registeredSensors.count()}")
                registeredSensors.clear()
                Log.d("SensorManager", "Registered Sensor Count After Clear: ${registeredSensors.count()}")
            }
            Log.d("SensorManager", "SensorManager is null")
        }
    }

    private fun notifySensorData(sensorData: SensorData) {
        //Send updated data array
        sensorListener.onSensorDataReceived(sensorData)
    }
}

