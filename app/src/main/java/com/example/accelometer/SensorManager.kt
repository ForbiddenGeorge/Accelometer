package com.example.accelometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager



class SensorManager(private val context: Context) {
    private var sensorManager: SensorManager? = null
    private lateinit var sensorListener: SensorDataListener
    private var registeredSensors: MutableList<Sensor?> = mutableListOf()

    fun startSensorUpdates(selectedSensorTypes: IntArray, dataListener: SensorDataListener, latency: Int) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorListener = dataListener

        val accelerometerData = FloatArray(3)
        val gyroscopeData = FloatArray(3)
        val otherSensorData = FloatArray(3)

        for (sensorType in selectedSensorTypes) {
            val sensor = sensorManager?.getDefaultSensor(sensorType)
            sensor?.let {
                val sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        /*
                        val sensorData = Bundle().apply {
                            putFloatArray("values", event.values)
                        }
                        notifySensorData(sensorData)
                        */
                        when (sensorType) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                event.values.copyInto(accelerometerData)
                            }
                            Sensor.TYPE_GYROSCOPE -> {
                                event.values.copyInto(gyroscopeData)
                            }
                            else -> {
                                event.values.copyInto(otherSensorData)
                            }
                        }

                        val sensorData = SensorData(
                            accelerometerData = accelerometerData.clone(),
                            gyroscopeData = gyroscopeData.clone(),
                            otherSensorData = otherSensorData.clone()
                        )
                        notifySensorData(sensorData)
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                        // Not needed for this example
                    }
                }

                sensorManager?.registerListener(sensorListener, it, latency)
                registeredSensors.add(it)
            }
        }
    }

    fun stopSensorUpdates() {
        sensorManager?.let {
            for (sensor in registeredSensors) {
                it.unregisterListener(null, sensor)
            }
            registeredSensors.clear()
        }
    }

    private fun notifySensorData(sensorData: SensorData) {
            sensorListener.onSensorDataReceived(sensorData)
    }
}

