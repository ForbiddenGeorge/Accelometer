package com.example.accelometer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
 /**
 * Main Activity, displaying main menu UI + other initializers
 */
class MainActivity : ComponentActivity(), View.OnClickListener {
    private lateinit var d1: CardView
    private lateinit var d2: CardView
    private lateinit var d3: CardView
    private lateinit var sensorManager: SensorManager
    object SensorHelper {
        var accelerometerMinDelay: Int = 0
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainui)
        //Request notification permission (for foreground service)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.POST_NOTIFICATIONS
            ),
    18751
        )

        //Other Activities
        d1 = findViewById(R.id.Mereni)
        d1.setOnClickListener(this)
        d2 = findViewById(R.id.Sensory)
        d2.setOnClickListener(this)
        d3 = findViewById(R.id.Settings)
        d3.setOnClickListener(this)

        //Get minimal delay allowed by the device
        findMinDelay()
        //Check and request storage permissions
        PermissionUtils.checkAndRequestStoragePermission(this)



        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        //Calling HardwareInfoFile object for creating file about hardware specification, if not previously created
        val hardwareInfoFile = sharedPreferences.getString("DHI", null)
        if(hardwareInfoFile == null){
            HardwareInfoFile.createHardwareInfo(sensorManager, this)
            val editor = sharedPreferences.edit()
            editor.apply {
                putString("DHI", "DHI_" + Build.MODEL +".txt")
            }.apply()
        }
    }

    override fun onClick(v: View) {
        val intent: Intent = when (v.id) {
            //On card click, open new activity
            R.id.Mereni ->Intent(this, MeasureActivity::class.java)
            R.id.Sensory ->Intent(this, SensorsActivity::class.java)
            R.id.Settings -> Intent(this, SettingsActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
    }

    //Find minimal sampling delay
    private fun findMinDelay() {
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        for (sensor in deviceSensors) {
            when (sensor.type) {
                Sensor.TYPE_ACCELEROMETER ->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Accelerometer_check", true)
                    }.apply()
                    SensorHelper.accelerometerMinDelay = sensor.minDelay
                }
                Sensor.TYPE_LINEAR_ACCELERATION ->{
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putBoolean("Linear_Accelerometer_check", true)
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
                        SensorHelper.accelerometerMinDelay = sensor.minDelay * 4
                    }
                }
            }
        }
        Log.d("minDelay", "Min delay was found, it is ${SensorHelper.accelerometerMinDelay} μs")
    }


}

