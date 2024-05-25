package com.example.accelometer

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Sensors Activity fro display hardware information about accessed sensors
 */
class SensorsActivity : ComponentActivity() {
    //Initialize UI
    private lateinit var sensorManager: SensorManager

    private lateinit var linearAccelerometer: TextView
    private lateinit var linearAccelerometerData: TextView

    private lateinit var accelerometer: TextView
    private lateinit var accelerometerData: TextView

    private lateinit var gravitation: TextView
    private lateinit var gravitationData: TextView

    private lateinit var gyroscope: TextView
    private lateinit var gyroscopeData: TextView

    private lateinit var model: TextView

    private var warning: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initialize UI
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sensorsui)
        model = findViewById(R.id.ModelInfo)
        linearAccelerometer = findViewById(R.id.Senzor_Linearni_Akcelometr_Nadpis)
        linearAccelerometerData = findViewById(R.id.Senzor_Linearni_Akcelometr_Data)
        accelerometer = findViewById(R.id.Senzor_Akcelometr_Nadpis)
        accelerometerData = findViewById(R.id.Senzor_Akcelometr_Data)
        gravitation = findViewById(R.id.Senzor_Gravitace_Nadpis)
        gravitationData = findViewById(R.id.Senzor_Gravitace_Data)
        gyroscope = findViewById(R.id.Senzor_Gyroskop_Nadpis)
        gyroscopeData = findViewById(R.id.Gyroskop_data)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        phoneModel()

        //Check if sensors are accessible
        for (sensor in deviceSensors){
            when (sensor.type){
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometer.text = "Akcelometr: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        ?.let { accelerometerData.text = sensorInfo(it) }
                }
                Sensor.TYPE_LINEAR_ACCELERATION-> {
                    linearAccelerometer.text = "Lineární akcelometr: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                        ?.let { linearAccelerometerData.text = sensorInfo(it) }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscope.text = "Gyroskop: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                        ?.let { gyroscopeData.text = sensorInfo(it) }
                }
                Sensor.TYPE_GRAVITY -> {
                    gravitation.text = "Gravitační senzor: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                        ?.let { gravitationData.text = sensorInfo(it) }
                }
                //Space for more sensors
            }
        }
        notThere()
    }

    //Get model of the phone
    @SuppressLint("SetTextI18n")
    private fun phoneModel(){
        model.text = "Model telefonu: ${Build.MODEL}"
    }

    //If sensors are not present, inform user and update UI
    @SuppressLint("SetTextI18n")
    fun notThere(){
        if (gravitation.text == null) {
            gravitation.text = "Gravitační senzor: Nepřítomen"
            warning = true
        }
        if (gyroscope.text == null) {
            gyroscope.text = "Gyroskop: Nepřítomen"
            warning = true
        }
        if (accelerometer.text == null) {
            accelerometer.text = "Akcelometr: Nepřítomen"
            warning = true
        }
        if (linearAccelerometer.text == null) {
            linearAccelerometer.text = "Lineární akcelerometr Nepřítomen"
            warning = true
        }
        if(warning){
            CustomDialog.showMessage(this, "Senzory nenalezeny",
                "Některé senzory nebyly nalezeny. " +
                        "Zkontrolujte, že vaše zařízení dané senzory obsahuje. " +
                        "V případě chyby kontaktujte autora aplikace. Některé funkce budou nepřístupné.")
            warning = false
        }

    }

    //Data formatting
    private fun sensorInfo(sensor: Sensor): String {
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
            "Popis: ${sensor.stringType} (${sensor.type})\n")
        return fileGRData.joinToString("")
    }
}