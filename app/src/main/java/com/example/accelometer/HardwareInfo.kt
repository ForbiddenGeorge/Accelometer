package com.example.accelometer

import android.hardware.Sensor
import android.hardware.SensorManager
import Writer
import android.content.Context
import android.os.Build
import android.util.Log

object HardwareInfo {
    private var jmenoSouboru: String = "Device_Hardware_Information.txt"

    fun createHardwareInfo(sensorManager: SensorManager, context: Context) {
        // Založení instance Writeru a vytvoření souboru
        val txtWriter = Writer(context)
        txtWriter.createFile(jmenoSouboru)

        //Zapsání modelu telefonu do souboru
        phoneModel(txtWriter)

        //Zapsaní informací o jednotlivých senzorech do souboru
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
                //Zde mohou jít další senzory
            }
        }

        // Uzavření souboru
        txtWriter.closeFile()
        Log.d("Hardware Info", "Device_Hardware_Information.txt byl vytvořen a uložen")
    }

    //Funkce pro správné formátovaní dat do souboru o senzorech
    private fun sensorInfo(sensor: Sensor, txtWriter: Writer) {
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

    //Získání modelu telefonu
    private fun phoneModel(txtWriter: Writer) {
        txtWriter.writeHardwareData(arrayOf("Model telefonu: ${Build.MODEL}\n\n\n"))
    }
}