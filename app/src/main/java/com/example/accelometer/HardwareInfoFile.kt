package com.example.accelometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log

/**
* Object for gathering device hardware information
 */
object HardwareInfoFile {
    private var fileName: String = "DHI_" + Build.MODEL +".txt"
    fun createHardwareInfo(sensorManager: SensorManager, context: Context) {

        //Create a file
        val txtWriter = Writer(context)
        txtWriter.createFile(fileName)
        phoneModel(txtWriter)

        //Write a paragraph for each accessible sensor
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in deviceSensors) {
            when (sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        ?.let { sensorInfo(it, txtWriter) }
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                        ?.let { sensorInfo(it, txtWriter) }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                        ?.let { sensorInfo(it, txtWriter) }
                }
                Sensor.TYPE_GRAVITY -> {
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                        ?.let { sensorInfo(it, txtWriter) }
                }
                //Others sensors can be added here
            }
        }

        //Close the file
        txtWriter.closeFile()
        Log.d("Hardware Info", "DHI_" + Build.MODEL + ".txt was created and saved")
    }


    private fun sensorInfo(sensor: Sensor, txtWriter: Writer) {
        //Function for proper data format
        val fileGRData = arrayOf(
            "Jméno: ${sensor.name}\n",
            "Typ: ${sensor.stringType}\n",
            "Vendor: ${sensor.vendor}\n",
            "Verze: ${sensor.version}\n",
            "Rozsah měření: ${sensor.maximumRange}\n",
            "Rozlišení: ${sensor.resolution}\n",
            "Spotřeba energie: ${sensor.power} mA\n",
            "Zpoždění: ${sensor.maxDelay} µs\n",
            "Minimální vzorkovací frekvence: ${sensor.minDelay} µs\n",
            "ID: ${sensor.id}\n",
            "Popis: ${sensor.stringType} (${sensor.type})\n"
        )
        txtWriter.writeHardwareData(fileGRData)
        val divider = arrayOf("-------------------------------\n\n")
        txtWriter.writeHardwareData(divider)

    }

    //Get the build model of the device
    private fun phoneModel(txtWriter: Writer) {
        txtWriter.writeHardwareData(arrayOf("Model telefonu: ${Build.MODEL}\n\n\n"))
    }
}