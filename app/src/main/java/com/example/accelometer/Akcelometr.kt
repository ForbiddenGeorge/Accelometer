package com.example.accelometer

import Writer
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.hardware.SensorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class Akcelometr : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var aSensor: Sensor
    private lateinit var aSensorDataX: TextView
    private lateinit var aSensorDataY: TextView
    private lateinit var aSensorDataZ: TextView
    private lateinit var startButton: Button
    private lateinit var jmenoSouboru: EditText
    val csvWriter = Writer(this)

    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private val alpha = 0.2f
    private var isSensorRunning = false
    //Časovač
    private var startTime: Long = 0
    private lateinit var stopwatchTime: TextView
    //Latence
    private var latency = 0
    private var stopwatchText = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_akcelometr)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        aSensorDataX = findViewById(R.id.aSensorDataX)
        aSensorDataY = findViewById(R.id.aSensorDataY)
        aSensorDataZ = findViewById(R.id.aSensorDataZ)
        startButton = findViewById(R.id.startButton)
        stopwatchTime = findViewById(R.id.TimeRunData)
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        latency = sharedPreferences.getInt("INT_KEY",0)
        Log.d("Latency", latency.toString())
        aSensorDataX.text = "x=0"
        aSensorDataY.text = "y=0"
        aSensorDataZ.text = "z=0"

        jmenoSouboru = findViewById(R.id.nazevSouboru)

        startButton.setOnClickListener {
            if (isSensorRunning) {
                stopSensor()
            } else {
                startSensor()
            }
        }
    }

    private fun startSensor() {
        sensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_GAME)
        isSensorRunning = true
        startButton.text = "Stop"
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val jmenoSouboruInput = "LA$currentTime" + "_" + jmenoSouboru.text.toString().replace(" ", "_")

        Log.d("Jméno souboru", jmenoSouboruInput + ".csv")


        //csvWriter.createFile(jmenoSouboruInput + ".csv")
        startTime = SystemClock.elapsedRealtime()
        updateStopwatch()
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(this)
        isSensorRunning = false
        startButton.text = "Start"
        aSensorDataX.text = "x=0"
        aSensorDataY.text = "y=0"
        aSensorDataZ.text = "z=0"
    }

    override fun onResume() {
        super.onResume()
        if (isSensorRunning) {
            sensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun updateStopwatch() {
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedTime = currentTime - startTime
        val miliseconds = (elapsedTime % 1000).toInt()
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60

        stopwatchText = String.format("%02d:%02d:%02d:%03d", hours, minutes % 60, seconds % 60, miliseconds)
        stopwatchTime.text = stopwatchText

        if (isSensorRunning) {
            // Schedule the next update after 1 second
            Handler(Looper.getMainLooper()).postDelayed({
                updateStopwatch()
            }, 1)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Access the sensor data from the event object
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            filteredX = applyExponentialSmoothingFilter(x, filteredX)
            filteredY = applyExponentialSmoothingFilter(y, filteredY)
            filteredZ = applyExponentialSmoothingFilter(z, filteredZ)

            aSensorDataX.text = "x=$filteredX"
            aSensorDataY.text = "y=$filteredY"
            aSensorDataZ.text = "z=$filteredZ"

            val sensorDataToFile = arrayOf(stopwatchText,filteredX.toString(), filteredY.toString(), filteredZ.toString())
            csvWriter.writeData(sensorDataToFile)
            //Log.d("SensorData", "Linear Acceleration: x=$x, y=$y, z=$z")
            Log.d("File_line",stopwatchText + "," + filteredX.toString() + "," +filteredY.toString() + "," + filteredZ.toString())
        }
    }

    private fun applyExponentialSmoothingFilter(value: Float, previousFilteredValue: Float): Float {
        return alpha * value + (1 - alpha) * previousFilteredValue
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}