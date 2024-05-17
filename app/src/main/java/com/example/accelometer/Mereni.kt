package com.example.accelometer

import CustomDialog
import FTPSender
import Writer
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
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
    //GPS
    private lateinit var gps: Location
    private lateinit var longitude: TextView
    private lateinit var latitude: TextView
    private lateinit var altitude: TextView
    private lateinit var speed: TextView
    private lateinit var satellites: TextView
    private lateinit var gpsTime: TextView
    private var gpsTrue: Boolean = false
    //Checkboxy
    private lateinit var checkBoxLinearniAkcelometr: CheckBox
    private lateinit var checkBoxAkcelometr: CheckBox
    private lateinit var checkBoxGyroskop: CheckBox
    private lateinit var hardwareSoubor: CheckBox
    private lateinit var checkboxGPS: CheckBox
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
    //GPS data
    private var longitudeData: Double = 0.0
    private var latitudeData: Double = 0.0
    private var altitudeData: Double = 0.0
    private var speedData = 0f
    private var satellitesData = 0
    private var gpsTimeData: Long = 0 //64bit int
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
    private val decimalFormat = "%.5f"
    private var isSensorRunning = false
    private var startTimeMillis: Long = 0
    private var hardwareSend: Boolean = false
    //Permise pro vysoké frekvenční snímání
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
    private val ftpSender = FTPSender()
    //Inicializace
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mereni_test)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        PermissionUtils.checkAndRequestStoragePermission(this)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            0
        )
        //// Inicializace proměnných ////
        //Ošetřit aby nepadala aplikace pokud tam není
        //Lineární akcelometr
        //Checkboxy
        checkBoxLinearniAkcelometr = findViewById(R.id.CK1)
        checkBoxAkcelometr = findViewById(R.id.CK2)
        checkBoxGyroskop = findViewById(R.id.CK3)
        checkboxGPS = findViewById(R.id.CKGPS)
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
        //GPS
        latitude = findViewById(R.id.GPS_Latitude_Data)
        longitude = findViewById(R.id.GPS_Longitude_Data)
        satellites = findViewById(R.id.GPS_Sattelite_Data)
        speed = findViewById(R.id.GPS_Speed_Data)
        altitude = findViewById(R.id.GPS_Altitude_Data)
        gpsTime = findViewById(R.id.GPS_Time_Data)
        latitude.text = "0°"
        longitude.text = "0°"
        altitude.text = "0 m"
        satellites.text = "0"
        speed.text = "0 m/s"
        gpsTime.text = "00:00:00:00:00:00"

        startButton = findViewById(R.id.startButton)
        stopwatchTime = findViewById(R.id.TimeRunData)
        latency = sharedPreferences.getInt("INT_KEY",0)
        Log.d("Latency", latency.toString())


        jmenoSouboru = findViewById(R.id.nazevSouboru)

        startButton.setOnClickListener {
            if (isSensorRunning) {
                stopSensor()
            } else {
                if(!ftpSender.isConnectedToInternet(this)){
                    CustomDialog.showMessage(this,"Chyba připojení",
                            "Zařízení není připojeno k internetu, pro FTP přenos, připojte se během měření k internetu")
                }
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
        if(checkBoxAkcelometr.isChecked || checkBoxGyroskop.isChecked || checkBoxLinearniAkcelometr.isChecked || checkboxGPS.isChecked){
            /*if(checkboxGPS.isChecked){
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)
                }
            }else{
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                startService(this)
            }
            }*/
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                startService(this)
            }
            if(checkBoxLinearniAkcelometr.isChecked){
                sensorManager.registerListener(this, laSensor, latency * 1000)
                checkBoxLinearniAkcelometr.isClickable = false
            }
            if(checkBoxAkcelometr.isChecked){
                sensorManager.registerListener(this, aSensor, latency * 1000)
                checkBoxAkcelometr.isClickable = false
            }
            if( checkBoxGyroskop.isChecked){
                sensorManager.registerListener(this, gSensor, latency * 1000)
                checkBoxGyroskop.isClickable = false
            }
            if( hardwareSoubor.isChecked){
                hardwareSend = true
                hardwareSoubor.isClickable = false
            }
            if(checkboxGPS.isChecked){
                gpsTrue = true
                checkboxGPS.isClickable = false
            }

            isSensorRunning = true
            startButton.text = "Stop"
            val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val jmenoSouboruInput = "F$currentTime" + "_" + jmenoSouboru.text.toString().replace(" ", "_")
            jmenoSouboruCele = "$jmenoSouboruInput.csv"
            Log.d("File name", jmenoSouboruCele)
            csvWriter.createFile(jmenoSouboruCele)
            val fDHIname = arrayOf(Build.MODEL)
            csvWriter.writeData(fDHIname)
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
        var zprava = Vysledek(false, "", 0)
        isSensorRunning = false
        if(checkBoxLinearniAkcelometr.isChecked){
            sensorManager.unregisterListener(this, laSensor)
            checkBoxLinearniAkcelometr.isClickable = true
        }
        if(checkBoxAkcelometr.isChecked){
            sensorManager.unregisterListener(this, aSensor)
            checkBoxAkcelometr.isClickable = true
        }
        if( checkBoxGyroskop.isChecked){
            sensorManager.unregisterListener(this, gSensor)
            checkBoxGyroskop.isClickable = true
        }
        if(checkboxGPS.isChecked){
            gpsTrue = false
            checkboxGPS.isClickable = true
        }
        hardwareSoubor.isClickable = true
        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            startService(this)
            Log.d("KONEC", "BYL ZAVOLÁN  KONEC!!!!")
        }

        csvWriter.closeFile()
        scheduledExecutor?.shutdown()
        Toast.makeText(this, "Soubor $jmenoSouboruCele uložen!", Toast.LENGTH_LONG).show()

        ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), jmenoSouboruCele, hardwareSoubor.isChecked)
        zprava = runBlocking {
            // Waiting for the FTP operation to finish and capturing its result
            ftpSender.uploadFileToFTPAsync()
        }

        if (zprava.status)
        {
            Toast.makeText(this,"Soubor úspěšně odeslán",Toast.LENGTH_SHORT).show()

        }else{
            CustomDialog.showMessage(this,"Chyba " + zprava.kod, zprava.chyba)
        }
        resetingValues()
    }

    //Tohle musí jít nějak zefektivnit, zkrášltít
    @SuppressLint("SetTextI18n")
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
    @SuppressLint("SetTextI18n")
    private fun observeLocationUpdates(context: Context) {
        val locationClient = DefaultLocationClient(
            context,
            LocationServices.getFusedLocationProviderClient(context)
        )

        locationClient.getLocationUpdates(1000L)
            .onEach { location ->
                // Perform actions with the received location data
                latitudeData = location.latitude
                longitudeData = location.longitude
                speedData = location.speed
                altitudeData = location.altitude
                gpsTimeData = location.time
                latitude.text = String.format("%.3f", latitudeData) + "°"
                longitude.text = String.format("%.3f", longitudeData) + "°"
                speed.text = String.format("%.2f", speedData) + " m/s"
                altitude.text = String.format("%.2f", altitudeData) + " m"
                gpsTime.text =  formatMillisToDateTimeString(gpsTimeData)
            }
            .launchIn(lifecycleScope) // Myslím že se to nezastaví kvůli tady tomuto
    }

    private fun formatMillisToDateTimeString(milliseconds: Long): String {
        val yearDivider = 31_536_000_000L
        val years = milliseconds / yearDivider
        var remainingMillis = milliseconds % yearDivider

        val monthDivider = 2_628_288_000L
        val months = remainingMillis / monthDivider
        remainingMillis %= monthDivider

        val dayDivider = 86_400_000L
        val days = remainingMillis / dayDivider
        remainingMillis %= dayDivider

        val hourDivider = 3_600_000L
        val hours = remainingMillis / hourDivider
        remainingMillis %= hourDivider

        val minuteDivider = 60_000L
        val minutes = remainingMillis / minuteDivider
        remainingMillis %= minuteDivider

        val seconds = remainingMillis / 1000L
        var yearsText = years.toString()
        var monthsText = months.toString()
        var daysText = days.toString()
        var hoursText = hours.toString()
        var minutesText = minutes.toString()
        var secondsText = seconds.toString()
        if (years < 10){yearsText = "0$years"}
        if (months < 10){monthsText = "0$months"}
        if (days < 10){daysText = "0$days"}
        if (hours < 10){hoursText = "0$hours"}
        if (minutes < 10){minutesText = "0$minutes"}
        if (seconds < 10){secondsText = "0$seconds"}
        gpsTimeData = "$yearsText$monthsText$daysText$hoursText$minutesText$secondsText".toLong()
        return yearsText + ":" + monthsText + ":" + daysText + ":" + hoursText + ":" + minutesText + ":" + secondsText
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
        if(gpsTrue) {
            observeLocationUpdates(this)
        }
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedMillis = currentTimeMillis - startTimeMillis
        /*satellitesData = getSatelliteCount(this)
        satellites.text = satellitesData.toString()*/
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
            gFilteredZ.toString(),
            latitudeData.toString(),
            longitudeData.toString(), //Android má nějakou funkci pro konverzi těchto GPS dat do stringu, viz dokumentace
            altitudeData.toString(),
            speedData.toString(),
            satellitesData.toString(),
            gpsTimeData.toString()
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
        //GPS
        latitude.text = "0° N/S"
        longitude.text = "0° W/E"
        altitude.text = "0 m"
        satellites.text = "0"
        speed.text = "0 m/s"
        gpsTime.text = "00:00:00"

        laFilteredX = 0.0f
        laFilteredY = 0.0f
        laFilteredZ = 0.0f
        aFilteredX = 0.0f
        aFilteredY = 0.0f
        aFilteredZ = 0.0f
        gFilteredX = 0.0f
        gFilteredY = 0.0f
        gFilteredZ = 0.0f

        altitudeData = 0.0
        latitudeData = 0.0
        longitudeData = 0.0
        satellitesData = 0
        gpsTimeData = 0
        speedData = 0f

        jmenoSouboru.setText("")
        hardwareSoubor.isChecked = false
        hardwareSend = false
        gpsTrue = false
    }

}

