package com.example.accelometer

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView

class MainActivity : ComponentActivity(), View.OnClickListener {
    //Zavedení proměnných
    private lateinit var d1: CardView
    private lateinit var d2: CardView
    private lateinit var d3: CardView
    private lateinit var sensorManager: SensorManager
    //objekt důležitý pro settings, podmínka pro minimální vzorkovací frekvenci
    object SensorHelper {
        var accelerometerMinDelay: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_test)

        //Napojení proměnných na UI
        d1 = findViewById(R.id.Mereni)
        d1.setOnClickListener(this)
        d2 = findViewById(R.id.Sensory)
        d2.setOnClickListener(this)
        d3 = findViewById(R.id.Settings)
        d3.setOnClickListener(this)

        //Získání hardwarového limitu pro senzory
        findMinDelay()
        //Získání povolení k přístupu do externího úložiště
        //PermissionUtils.checkAndRequestStoragePermission(this)

        //Potvrzení že máme povolení k externímu úložišti
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val isPermissionGranted = sharedPreferences.getBoolean("storage_permission_granted", false)
        if(!isPermissionGranted)
        {
            Toast.makeText(this, "Bez povolení nelze data ukládat do dokumentů", Toast.LENGTH_LONG).show()
        }

        //Založení .txt souboru s infem o telefonu
        val DHI = sharedPreferences.getString("DHI", null)
        if(DHI == null){
            HardwareInfo.createHardwareInfo(sensorManager, this)
            val editor = sharedPreferences.edit()
            editor.apply {
                putString("DHI", "DHI_" + Build.MODEL +".txt")

            }.apply()
        }
    }

    //Na kliknutí otevřít novou aktivitu
    override fun onClick(v: View) {
        val intent: Intent = when (v.id) {
            R.id.Mereni ->Intent(this, MereniTest::class.java)
            R.id.Sensory ->Intent(this, Sensory::class.java)
            R.id.Settings -> Intent(this, Settings::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
    }

    //Najití nejvetší minimální vzorkovací frekvence ze seznamu chtěných senzorů
    private fun findMinDelay() {
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        for (sensor in deviceSensors) {
            when (sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Accelometer_check", true)
                    }.apply()
                    SensorHelper.accelerometerMinDelay = sensor.minDelay
                }
                Sensor.TYPE_LINEAR_ACCELERATION ->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Linear_Accelometr_check", true)
                    }.apply()
                    if (sensor.minDelay > SensorHelper.accelerometerMinDelay){
                        SensorHelper.accelerometerMinDelay = sensor.minDelay
                    }
                }
                Sensor.TYPE_GRAVITY ->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Gravity_check", true)
                    }.apply()
                    if (sensor.minDelay > SensorHelper.accelerometerMinDelay){
                        SensorHelper.accelerometerMinDelay = sensor.minDelay
                    }
                }
                Sensor.TYPE_GYROSCOPE->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Gyroscope_check", true)
                    }.apply()
                    if (sensor.minDelay > SensorHelper.accelerometerMinDelay){
                        SensorHelper.accelerometerMinDelay = sensor.minDelay * 5
                    }
                }
            }
        }
        Log.d("minDelay", "Min delay was found, it is ${SensorHelper.accelerometerMinDelay} μs")
    }


}

