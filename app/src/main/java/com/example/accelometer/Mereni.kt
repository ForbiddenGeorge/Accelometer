package com.example.accelometer

import CustomDialog
import FTPSender
import Writer
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Mereni : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    //Lineární Akcelometr
    private lateinit var laSensor: Sensor
    private lateinit var laSensorDataX: TextView
    private lateinit var laSensorDataY: TextView
    private lateinit var laSensorDataZ: TextView
    //Akcelometr
    private lateinit var aSensor: Sensor
    private lateinit var aSensorDataX: TextView
    private lateinit var aSensorDataY: TextView
    private lateinit var aSensorDataZ: TextView
    //Gyroskop
    private lateinit var gSensor: Sensor
    private lateinit var gSensorDataX: TextView
    private lateinit var gSensorDataY: TextView
    private lateinit var gSensorDataZ: TextView
    //Checkboxy
    private lateinit var checkBoxLinearniAkcelometr: CheckBox
    private lateinit var checkBoxAkcelometr: CheckBox
    private lateinit var checkBoxGyroskop: CheckBox
    private lateinit var hardwareSoubor: CheckBox
    private var boll: Boolean = false
    //soubor
    private val csvWriter = Writer(this)
    private lateinit var jmenoSouboru: EditText
    private lateinit var jmenoSouboruCele: String
    //Filtrovaná Senzorová data
    private var laFilteredX = 0f
    private var laFilteredY = 0f
    private var laFilteredZ = 0f

    private var aFilteredX = 0f
    private var aFilteredY = 0f
    private var aFilteredZ = 0f

    private var gFilteredX = 0f
    private var gFilteredY = 0f
    private var gFilteredZ = 0f
    //filtrovací konstanta
    private val alpha = 0.2f
    //Časovač
    private var startTime: Long = 0
    private lateinit var stopwatchTime: TextView
    private var miliseconds = 0
    private var stopwatchText = "00:00:000"
    //Latence
    private var latency = 0
    //Tlačítka
    private lateinit var startButton: Button
    //Neklasifikováno
    private val decimalFormat = "%.6f"
    private var isSensorRunning = false
    private var startTimeMillis: Long = 0
    private var hardwareSend: Boolean = false
    //Permise pro vysoké frekvenční snímání
    private val highSampleRequestCode = 123
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.v("PERMISSION", "ACCESS Granted")
            } else {
                // Permission denied, handle it (e.g., show a message to the user)
                Log.v("PERMISSION", "ACCESS Denied")
            }
        }
    //Pravidelné zaznamenávání dat
    private var scheduledExecutor: ScheduledExecutorService? = null
    //FTP
    public val ftpSender = FTPSender()
    private lateinit var erorek:String
    //Inicializace
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mereni_test)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        PermissionUtils.checkAndRequestStoragePermission(this)
        //// Inicializace proměnných ////
        //Ošetřit aby nepadala aplikace pokud tam není
        //Lineární akcelometr
        //Checkboxy
        checkBoxLinearniAkcelometr = findViewById(R.id.CK1)
        checkBoxAkcelometr = findViewById(R.id.CK2)
        checkBoxGyroskop = findViewById(R.id.CK3)
        hardwareSoubor = findViewById(R.id.CK4)
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        boll = sharedPreferences.getBoolean("Gyroscope_check", false)
        if (!boll){
            checkBoxGyroskop.isClickable = false
        }else{
            gSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        }
        boll = sharedPreferences.getBoolean("Accelometer_check", true)
        if (!boll){
            checkBoxAkcelometr.isClickable = false
        }else{
            aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        }
        boll = sharedPreferences.getBoolean("Linear_Accelometr_check", false)
        if (!boll){
            checkBoxLinearniAkcelometr.isClickable = false

        }else{
            laSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
        }
        laSensorDataX = findViewById(R.id.laSensorDataX)
        laSensorDataY = findViewById(R.id.laSensorDataY)
        laSensorDataZ = findViewById(R.id.laSensorDataZ)
        laSensorDataX.text = "x=0"
        laSensorDataY.text = "y=0"
        laSensorDataZ.text = "z=0"

        //Akcelometr
        aSensorDataX = findViewById(R.id.aSensorDataX)
        aSensorDataY = findViewById(R.id.aSensorDataY)
        aSensorDataZ = findViewById(R.id.aSensorDataZ)
        aSensorDataX.text = "x=0"
        aSensorDataY.text = "y=0"
        aSensorDataZ.text = "z=0"

        //Gyroskop
        gSensorDataX = findViewById(R.id.gSensorDataX)
        gSensorDataY = findViewById(R.id.gSensorDataY)
        gSensorDataZ = findViewById(R.id.gSensorDataZ)
        gSensorDataX.text = "x=0"
        gSensorDataY.text = "y=0"
        gSensorDataZ.text = "z=0"

        startButton = findViewById(R.id.startButton)
        stopwatchTime = findViewById(R.id.TimeRunData)
        latency = sharedPreferences.getInt("INT_KEY",0)
        Log.d("Latency", latency.toString())

        jmenoSouboru = findViewById(R.id.nazevSouboru)

        startButton.setOnClickListener {
            if (isSensorRunning) {
                stopSensor()
            } else {
                startSensor()
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            requestPermissionLauncher.launch(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
        }else{
            Log.v("PERMISSION", "ACCESS granted early")
        }


    }

    //Počátek snímání, rozjetí celého systému
    private fun startSensor() {
        if(checkBoxAkcelometr.isChecked || checkBoxGyroskop.isChecked || checkBoxLinearniAkcelometr.isChecked){
            if(checkBoxLinearniAkcelometr.isChecked){
                sensorManager.registerListener(this, laSensor, latency * 1000)
            }
            if(checkBoxAkcelometr.isChecked){
                sensorManager.registerListener(this, aSensor, latency * 1000)
            }
            if( checkBoxGyroskop.isChecked){
                sensorManager.registerListener(this, gSensor, latency * 1000)
            }
            if( hardwareSoubor.isChecked){
                hardwareSend = true
            }

            isSensorRunning = true
            startButton.text = "Stop"
            val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val jmenoSouboruInput = "F$currentTime" + "_" + jmenoSouboru.text.toString().replace(" ", "_")
            jmenoSouboruCele = "$jmenoSouboruInput.csv"
            Log.d("File name", jmenoSouboruCele)
            csvWriter.createFile(jmenoSouboruCele)
            startTimeMillis = System.currentTimeMillis()
            startTime = SystemClock.elapsedRealtime()
            updateStopwatch()
            scheduledExecutor = Executors.newScheduledThreadPool(2)
            startHighResolutionTimer()
        }else{
            Toast.makeText(this,"Nebyl zvolen žádný senzor", Toast.LENGTH_LONG).show()
        }

    }

    //Zastavení celého systému
    private fun stopSensor() {
        isSensorRunning = false
        if(checkBoxLinearniAkcelometr.isChecked){
            sensorManager.unregisterListener(this, laSensor)
        }
        if(checkBoxAkcelometr.isChecked){
            sensorManager.unregisterListener(this, aSensor)
        }
        if( checkBoxGyroskop.isChecked){
            sensorManager.unregisterListener(this, gSensor)
        }
        csvWriter.closeFile()
        scheduledExecutor?.shutdown()
        Toast.makeText(this, "Soubor $jmenoSouboruCele uložen!", Toast.LENGTH_LONG).show()
        val directory = csvWriter.getAppSubdirectory().toString()
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
       // val ftp = sharedPreferences.getBoolean("FTP_CHECK", true)
        val ftp = true
        if(ftp)
        {
            ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), jmenoSouboruCele, hardwareSoubor.isChecked)
            erorek = ftpSender.uploadFileToFTPAsync()
        }
        Log.d("Erorek", erorek)
        if (erorek == "")
        {
            Toast.makeText(this,"Soubor úspěšně odeslán",Toast.LENGTH_SHORT).show()

        }else{
            CustomDialog.showMessage(this,"Chyba",erorek)
        }
        resetingValues()
    }

    //Tohle musí jít nějak zefektivnit, zkrášltít
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val lax = event.values[0].toString()
            val lay = event.values[1].toString()
            val laz = event.values[2].toString()


            laFilteredX = applyExponentialSmoothingFilter(lax.toFloat(), laFilteredX)
            laFilteredY = applyExponentialSmoothingFilter(lay.toFloat(), laFilteredY)
            laFilteredZ = applyExponentialSmoothingFilter(laz.toFloat(), laFilteredZ)

            laSensorDataX.text = "x=${String.format(decimalFormat, laFilteredX)}"
            laSensorDataY.text = "y=${String.format(decimalFormat, laFilteredY)}"
            laSensorDataZ.text = "z=${String.format(decimalFormat, laFilteredZ)}"
        }
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0].toString()
            val ay = event.values[1].toString()
            val az = event.values[2].toString()

            aFilteredX = applyExponentialSmoothingFilter(ax.toFloat(), aFilteredX)
            aFilteredY = applyExponentialSmoothingFilter(ay.toFloat(), aFilteredY)
            aFilteredZ = applyExponentialSmoothingFilter(az.toFloat(), aFilteredZ)

            aSensorDataX.text = "x=${String.format(decimalFormat, aFilteredX)}"
            aSensorDataY.text = "y=${String.format(decimalFormat, aFilteredY)}"
            aSensorDataZ.text = "z=${String.format(decimalFormat, aFilteredZ)}"
        }
        if(event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gx = event.values[0].toString()
            val gy = event.values[1].toString()
            val gz = event.values[2].toString()

            gFilteredX = applyExponentialSmoothingFilter(gx.toFloat(), gFilteredX)
            gFilteredY = applyExponentialSmoothingFilter(gy.toFloat(), gFilteredY)
            gFilteredZ = applyExponentialSmoothingFilter(gz.toFloat(), gFilteredZ)

            gSensorDataX.text = "x=${String.format(decimalFormat, gFilteredX)}"
            gSensorDataY.text = "y=${String.format(decimalFormat, gFilteredY)}"
            gSensorDataZ.text = "z=${String.format(decimalFormat, gFilteredZ)}"
        }

    }

    //Log když se změní přesnost senzoru
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.w("SensorAccuracy", "Accuracy has changed $accuracy")
    }

    //Zapnutí časovače pro přesné zaznamenávání dat
    private fun startHighResolutionTimer() {
        val delayMillis = (latency).toLong()

        scheduledExecutor?.scheduleAtFixedRate(
            {
                readAndWriteSensorData()
            },
            0,
            delayMillis,
            TimeUnit.MILLISECONDS
        )
    }

    //Zapsání akutálních dat do souboru
    private fun readAndWriteSensorData() {
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedMillis = currentTimeMillis - startTimeMillis

        val sensorDataToFile = arrayOf(
            elapsedMillis.toString(),
            laFilteredX.toString(),
            laFilteredY.toString(),
            laFilteredZ.toString(),
            aFilteredX.toString(),
            aFilteredY.toString(),
            aFilteredZ.toString(),
            gFilteredX.toString(),
            gFilteredY.toString(),
            gFilteredZ.toString()
        )
        csvWriter.writeData(sensorDataToFile)
    }

    //Práce UI stopek
    private fun updateStopwatch() {
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedTime = currentTime - startTime
        miliseconds = elapsedTime.toInt()
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60

        stopwatchText = String.format("%02d:%02d:%03d", minutes % 60, seconds % 60, (miliseconds % 1000))
        stopwatchTime.text = stopwatchText

        if (isSensorRunning) {
            Handler(Looper.getMainLooper()).postDelayed({
                updateStopwatch()
            }, 0)
        }else{
            stopwatchTime.text = "00:00:000"
        }
    }

    //Filtr pro zlepšení posloupnosti dat, musím probrat s profesorem jestli se bude používat
    private fun applyExponentialSmoothingFilter(value: Float, previousFilteredValue: Float): Float {
        return alpha * value + (1 - alpha) * previousFilteredValue
    }

    //Vynulování proměnných, resetování UI
    private fun resetingValues(){
        startButton.text = "Start"
        laSensorDataX.text = "x=0"
        laSensorDataY.text = "y=0"
        laSensorDataZ.text = "z=0"
        aSensorDataX.text = "x=0"
        aSensorDataY.text = "y=0"
        aSensorDataZ.text = "z=0"
        gSensorDataX.text = "x=0"
        gSensorDataY.text = "y=0"
        gSensorDataZ.text = "z=0"

        laFilteredX = 0.0f
        laFilteredY = 0.0f
        laFilteredZ = 0.0f
        aFilteredX = 0.0f
        aFilteredY = 0.0f
        aFilteredZ = 0.0f
        gFilteredX = 0.0f
        gFilteredY = 0.0f
        gFilteredZ = 0.0f

        jmenoSouboru.setText("")
        hardwareSoubor.isChecked = false
        hardwareSend = false
    }

}

