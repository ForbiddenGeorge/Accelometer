package com.example.accelometer

import CustomDialog
import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class Sensory : ComponentActivity() {
    //založení proměnných
    private lateinit var sensorManager: SensorManager

    private lateinit var linearniAkcelometr: TextView
    private lateinit var linearniAkcelometrData: TextView

    private lateinit var akcelometr: TextView
    private lateinit var akcelometrData: TextView

    private lateinit var gravitace: TextView
    private lateinit var gravitaceData: TextView

    private lateinit var gyroskop: TextView
    private lateinit var gyroskopData: TextView

    private lateinit var model: TextView

    private var warning: Boolean = false

    @SuppressLint("SetTextI18n") // Aby nechodily warningy skrze hardcoded text
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_senzory)
        //Napojení proměnných na UI elementy
        model = findViewById(R.id.ModelInfo)
        linearniAkcelometr = findViewById(R.id.Senzor_Linearni_Akcelometr_Nadpis)
        linearniAkcelometrData = findViewById(R.id.Senzor_Linearni_Akcelometr_Data)
        akcelometr = findViewById(R.id.Senzor_Akcelometr_Nadpis)
        akcelometrData = findViewById(R.id.Senzor_Akcelometr_Data)
        gravitace = findViewById(R.id.Senzor_Gravitace_Nadpis)
        gravitaceData = findViewById(R.id.Senzor_Gravitace_Data)
        gyroskop = findViewById(R.id.Senzor_Gyroskop_Nadpis)
        gyroskopData = findViewById(R.id.Gyroskop_data)
        //Manažer senozorů pro jejich získání
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        //Získání modelu telefonu a jeho zapsání
        phoneModel()

        //Projití všech senzorů, najití těch chtěných a vypsání infa jak do UI tak do souboru
        for (sensor in deviceSensors){
            when (sensor.type){
                Sensor.TYPE_ACCELEROMETER -> {
                    akcelometr.text = "Akcelometr: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                        ?.let { akcelometrData.text = sensorInfo(it) }
                }
                Sensor.TYPE_LINEAR_ACCELERATION-> {
                    linearniAkcelometr.text = "Lineární akcelometr: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                        ?.let { linearniAkcelometrData.text = sensorInfo(it) }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroskop.text = "Gyroskop: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                        ?.let { gyroskopData.text = sensorInfo(it) }
                }
                Sensor.TYPE_GRAVITY -> {
                    gravitace.text = "Gravitační senzor: Přítomen"
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                        ?.let { gravitaceData.text = sensorInfo(it) }
                }
                //Tady mohou jít další sensory
            }
        }
        //Senzory nejsou k najití
        notThere()
    }

    //Získání modelu telefonu
    @SuppressLint("SetTextI18n")
    private fun phoneModel(){
        model.text = "Model telefonu: ${Build.MODEL}"
    }

    //Pokud nenajde senzory, update UI
    @SuppressLint("SetTextI18n")
    fun notThere(){
        //val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        if (gravitace.text == null) {
            gravitace.text = "Gravitační senzor: Nepřítomen"
            warning = true
        }
        if (gyroskop.text == null) {
            gyroskop.text = "Gyroskop: Nepřítomen"
            warning = true
        }
        if (akcelometr.text == null) {
            akcelometr.text = "Akcelometr: Nepřítomen"
            warning = true
        }
        if (linearniAkcelometr.text == null) {
            linearniAkcelometr.text = "Lineární akcelerometr Nepřítomen"
            warning = true
        }
        if(warning){
            CustomDialog.showMessage(this, "Senzory nenalezeny",
                "Některé senzory nebyly nalezeny. " +
                        "Zkontrolujte že vaše zařízení dané senzory obsahuje. " +
                        "V případě chyby kontaktujte autora aplikace. Některé funkce budou nepřístupné.")
            warning = false
        }

    }

    //Výpis dat o senzorech a uložení do souboru
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