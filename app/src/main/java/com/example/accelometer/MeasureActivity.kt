package com.example.accelometer

import CustomDialog
import FTP
import Writer
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MeasureActivity: ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    //Senzory TEORETICKY NEPOTŘEBUJI
    private lateinit var linearAccelerationlSensor: Sensor
    private lateinit var accelerationSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
    //List senzorů
    private val selectedSensors = mutableListOf<Sensor?>()
    //UI pro data senzorů
    private lateinit var linearAccelerationTextView: TextView
    private lateinit var accelerationTextView: TextView
    private lateinit var gyroscopeTextView: TextView
    //Raw data
    private var linearAccelerationValueX = 0f
    private var linearAccelerationValueY = 0f
    private var linearAccelerationValueZ = 0f
    private var accelerationValueX = 0f
    private var accelerationValueY = 0f
    private var accelerationValueZ = 0f
    private var gyroscopeValueX = 0f
    private var gyroscopeValueY = 0f
    private var gyroscopeValueZ = 0f
    //UI pro GPS data
    private lateinit var longitudeTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var altitudeTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var gpsTimeTextView: TextView
    private lateinit var satteliteCountTextView: TextView
    //Raw data
    private var longitudeValue: Double = 0.0
    private var latitudeValue: Double = 0.0
    private var altitudeValue: Double = 0.0
    private var speedValue = 0f
    private var timeValue:Long = 0L
    private var satelliteValue: Int = 0
    private var usedSatelliteValue: Int = 0
    //Checkboxy
    private lateinit var linearAccelerationCheckBox: CheckBox
    private lateinit var accelerationCheckBox: CheckBox
    private lateinit var gyroscopeCheckBox: CheckBox
    private lateinit var hardwareFileCheckBox: CheckBox
    private lateinit var gpsCheckBox: CheckBox
    private var accesibleSensor: Boolean = false
    //Soubor
    private val csvWriter = Writer(this)
    private lateinit var fileName: EditText
    private lateinit var fileNameWhole: String
    private var sendHardwareFile: Boolean = false
    //Stopky
    private lateinit var stopwatchTextView: TextView
    private lateinit var stopWatch: Chronometer
    private var startTime: Long = 0
    private var miliseconds:Long = 0
    private var stopwatchText = "00:00:000"
    //Tlačitko
    private lateinit var Button: Button
    //Stav měření
    private var isMeasuringActive: Boolean = false
    //FTP
    private val ftpSender = FTP()
    private var scheduler: ScheduledExecutorService? = Executors.newScheduledThreadPool(2)
    //Spoždění
    private var latency = 0
    //Vlákno
    private val uiHandler = Handler(Looper.getMainLooper())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start the foreground service
            Log.d("notify", "YAY")
        } else {
            // Permission denied, handle accordingly
            Toast.makeText(this, "Notification permission is required for this app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.measureui)
        PermissionUtils.checkAndRequestStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtils.checkAndRequestNotificationPermission(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtils.checkAndRequestHighSamplePermission(this)
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        linearAccelerationCheckBox = findViewById(R.id.CK1)
        accelerationCheckBox = findViewById(R.id.CK2)
        gyroscopeCheckBox = findViewById(R.id.CK3)
        gpsCheckBox = findViewById(R.id.CKGPS)
        hardwareFileCheckBox = findViewById(R.id.CK4)
        initializeGPSTextViews()
        initializeSensorsTextViews()
        getAccessibleSensors()
        //Tlačítko
        Button = findViewById(R.id.startButton)
        //Stopky
        stopwatchTextView = findViewById(R.id.TimeRunData)
        stopwatchTextView.text = stopwatchText
        stopWatch = findViewById(R.id.stopWatch)
        //Latence
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        latency = sharedPreferences.getInt("INT_KEY",0)
        Log.d("latency", latency.toString())
        //Okno pro název souboru
        fileName = findViewById(R.id.nazevSouboru)
        //Logika tlačítka
        Button.setOnClickListener {
            Button.isClickable = false
            if(!isMeasuringActive){
                if(!ftpSender.isConnectedToInternet(this)){
                    CustomDialog.showMessage(this,"Chyba připojení",
                        "Zařízení není připojeno k internetu, pro FTP přenos, připojte se během měření k internetu")
                }
                startMeasuring()
            }else{
                stopMeasuring()
            }
            Button.postDelayed({
                Button.isClickable = true
            }, 1250)
        }
        gpsCheckBox.setOnClickListener {

            if(gpsCheckBox.isChecked){
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    // Request location permissions
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                           android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        789
                    )
                }
                if(!isLocationEnabled(this)){
                    CustomDialog.showMessage(this,"Poloha",
                        "Zařízení nemá zapnuté snímání polohy. Aktivujte snímání polohy a akci proveďte znovu")
                    gpsCheckBox.isChecked = false
                }
            }
        }
        resetValues()
    }
    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("InlinedApi")
    private fun checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                88
            )
        }
    }
    private fun stopMeasuring(){
        stopWatch()
        isMeasuringActive = false
        ForeGroundService.stopService(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsDataReceiver)
        enableDisableCheckBoxes(true)
        stopItAll()
    }

    private fun stopItAll() {
        var zprava = FTPResult(false,"",0)
        csvWriter.closeFile()
        scheduler?.shutdown()
        Toast.makeText(this, "Soubor $fileNameWhole uložen!", Toast.LENGTH_SHORT).show()
        if(!isConnectedToInternet(this)){
            CustomDialog.showMessage(this,
                "Internet",
                "Vaše zařízení není připojené k internetu." +
                        " Odeslání souboru neproběhlo")
        }else{
            CoroutineScope(Dispatchers.IO).launch {
                zprava = runFTP()
                withContext(Dispatchers.Main) {
                    if (zprava.status) {
                        Toast.makeText(this@MeasureActivity, "Soubor úspěšně odeslán", Toast.LENGTH_SHORT).show()
                    } else {
                        CustomDialog.showMessage(this@MeasureActivity, "Chyba " + zprava.kod, zprava.chyba)
                    }
                }
            }
        }
        resetValues()
    }

    private fun startMeasuring() {
       //doRegisterSensorsChecked()
        val selectedSensorTypes = getSelectedSensorTypes() // Správné číselné hodnoty
        if(gpsCheckBox.isChecked && !isLocationEnabled(this)){
            CustomDialog.showMessage(this,"Poloha",
                "Zařízení nemá zapnuté snímání polohy. Aktivujte snímání polohy a akci proveďte znovu")
            gpsCheckBox.isChecked = false
        }else{
            if(selectedSensorTypes.isNotEmpty() || gpsCheckBox.isChecked){

                ForeGroundService.startService(
                    this,
                    selectedSensorTypes,
                    gpsCheckBox.isChecked,
                    latency
                )
                startItAll()
                //Senzory
                if(selectedSensorTypes.isNotEmpty()){
                    LocalBroadcastManager.getInstance(this).registerReceiver(
                        sensorDataReceiver,
                        IntentFilter("SENSOR_DATA_ACTION")
                    )
                }
                //GPS
                if(gpsCheckBox.isChecked){
                    LocalBroadcastManager.getInstance(this).registerReceiver(
                        gpsDataReceiver,
                        IntentFilter("GPS_DATA_ACTION")
                    )
                }
                enableDisableCheckBoxes(false)
            }else{
                Toast.makeText(this,"Nebyl zvolen žádný senzor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedSensorTypes(): IntArray {
        val selectedSensorTypes = mutableListOf<Int>()
        if (linearAccelerationCheckBox.isChecked) {
            selectedSensorTypes.add(Sensor.TYPE_LINEAR_ACCELERATION)
        }
        if (accelerationCheckBox.isChecked) {
            selectedSensorTypes.add(Sensor.TYPE_ACCELEROMETER)
        }
        if (gyroscopeCheckBox.isChecked) {
            selectedSensorTypes.add(Sensor.TYPE_GYROSCOPE)
        }
        return selectedSensorTypes.toIntArray()
    }

    private fun runFTP(): FTPResult {
            ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), fileNameWhole, hardwareFileCheckBox.isChecked)
            val outcome = runBlocking {
                ftpSender.queueFTP(true)
            }
            return outcome
    } //V pořádku

    private fun startItAll() {
        dataFileSetup()
        startTime = SystemClock.elapsedRealtime()
        miliseconds = System.currentTimeMillis()
        scheduler = Executors.newScheduledThreadPool(1)
        stopWatch()
        startPeriodical()
        Button.text = "Stop"
        isMeasuringActive = true
        if(!isConnectedToInternet(this)) {
            CustomDialog.showMessage(this,
                "Internet",
                "Vaše zařízení není připojené k internetu." +
                        " Pokud chcete po ukončení měření odeslat soubory, " +
                        "připojte se během měření k internetu.")
        }
    } //V pořádku

    private fun stopWatch(){
        if(!isMeasuringActive){
            startTime = SystemClock.elapsedRealtime()
            stopWatch.base = startTime
            stopWatch.start()
            isMeasuringActive = true
            uiHandler.post(updateStopwatchRunnable)
        }else{
            stopWatch.stop()
            uiHandler.removeCallbacks(updateStopwatchRunnable)
            isMeasuringActive = false
        }
    } //V pořádku

    private val updateStopwatchRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isMeasuringActive) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startTime
                val milliseconds = (elapsedMillis % 1000).toInt()
                val seconds = (elapsedMillis / 1000).toInt()
                val minutes = seconds / 60
                val formattedTime = String.format("%02d:%02d:%03d", minutes % 60, seconds % 60, milliseconds)
                stopwatchTextView.text = formattedTime
                uiHandler.postDelayed(this, 0)
            }
        }
    } //V pořádku

    private fun startPeriodical(){
        /*scheduler?.scheduleAtFixedRate(
            {
                writeDataToFile()
                updateUI()
            },
            300,
            latency.toLong(),
            TimeUnit.MILLISECONDS
        )*/

        val initialDelay = 0L
        val period = latency.toLong()

        if (period <= 0) {
            Log.e("MereniTest", "Invalid initialDelay or period for scheduling: $initialDelay, $period")
            return
        }

        try {
            scheduler?.scheduleAtFixedRate({
                // Your periodic task
                writeDataToFile()
                updateUI()
            }, initialDelay, period, TimeUnit.MILLISECONDS)
        } catch (e: IllegalArgumentException) {
            Log.e("MereniTest", "Error scheduling periodic task", e)
        }
    } //V pořádku

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        if(gpsCheckBox.isChecked){
            latitudeTextView.text = latitudeValue.toString() + "°"
            longitudeTextView.text = longitudeValue.toString() + "°"
            altitudeTextView.text = altitudeValue.toString() + "m.n.m."
            speedTextView.text = (speedValue).toString() + "km/h"
            satteliteCountTextView.text = usedSatelliteValue.toString() + "/" + satelliteValue.toString()
            //gpsTimeTextView.text = timeValue.toString()
        }
        if(linearAccelerationCheckBox.isChecked){
            linearAccelerationTextView.text =
                "X = $linearAccelerationValueX \n\nY= $linearAccelerationValueY \n\nZ= $linearAccelerationValueZ "
        }
        if(accelerationCheckBox.isChecked){
            accelerationTextView.text =
                "X = $accelerationValueX \n\nY= $accelerationValueY \n\nZ= $accelerationValueZ "
        }
        if(gyroscopeCheckBox.isChecked){
            gyroscopeTextView.text =
                "X = $gyroscopeValueX \n\nY= $gyroscopeValueY \n\nZ= $gyroscopeValueZ "

        }
    }

    private fun dataFileSetup(){
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        fileNameWhole = "F$currentTime" + "_" + fileName.text.toString().replace(" ", "_") + ".csv"
        Log.d("Whole file name", fileNameWhole)
        csvWriter.createFile(fileNameWhole)
        val nameOfDevice = arrayOf(Build.MODEL)
        csvWriter.writeData(nameOfDevice)
    } //V pořádku

    private fun writeDataToFile(){
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedMillis = currentTimeMillis - miliseconds
        val dataToFile = arrayOf(
            elapsedMillis.toString(),
            linearAccelerationValueX.toString(),
            linearAccelerationValueY.toString(),
            linearAccelerationValueZ.toString(),
            accelerationValueX.toString(),
            accelerationValueY.toString(),
            accelerationValueZ.toString(),
            gyroscopeValueX.toString(),
            gyroscopeValueY.toString(),
            gyroscopeValueZ.toString(),
            latitudeValue.toString(),
            longitudeValue.toString(),
            altitudeValue.toString(),
            speedValue.toString(),
            usedSatelliteValue.toString(),
            timeValue.toString(),
        )
        csvWriter.writeData(dataToFile)
    } //V pořádku

    private fun doRegisterSensorsChecked(){
            if(linearAccelerationCheckBox.isChecked){
                selectedSensors.add(0,sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION))
            }
            if(accelerationCheckBox.isChecked){
                selectedSensors.add(0,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
            }
            if(gyroscopeCheckBox.isChecked){
                selectedSensors.add(0,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
            }
            if(hardwareFileCheckBox.isChecked){
                sendHardwareFile = true
            }
            enableDisableCheckBoxes(false)
    }

    private fun enableDisableCheckBoxes(DisableOrEnable: Boolean) {
        linearAccelerationCheckBox.isClickable = DisableOrEnable
        accelerationCheckBox.isClickable = DisableOrEnable
        gyroscopeCheckBox.isClickable = DisableOrEnable
        hardwareFileCheckBox.isClickable = DisableOrEnable
        gpsCheckBox.isClickable = DisableOrEnable
    }

    private fun getAccessibleSensors(){
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        accesibleSensor = sharedPreferences.getBoolean("Gyroscope_check", false)
        if (!accesibleSensor){
            gyroscopeCheckBox.isClickable = false
        }else{
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        }
        accesibleSensor = sharedPreferences.getBoolean("Accelometer_check", true)
        if (!accesibleSensor){
            accelerationCheckBox.isClickable = false
        }else{
            accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        }
        accesibleSensor = sharedPreferences.getBoolean("Linear_Accelometr_check", false)
        if (!accesibleSensor){
            linearAccelerationCheckBox.isClickable = false
        }else{
            linearAccelerationlSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
        }
    } //V pořádku

    @SuppressLint("SetTextI18n")
    private fun initializeSensorsTextViews(){
        linearAccelerationTextView = findViewById(R.id.laSensorDataX)
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView = findViewById(R.id.aSensorDataX)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView = findViewById(R.id.gSensorDataX)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
    } //V pořádku

    private fun initializeGPSTextViews(){
        longitudeTextView = findViewById(R.id.GPS_Longitude_Data)
        longitudeTextView.text = "0°"
        latitudeTextView = findViewById(R.id.GPS_Latitude_Data)
        latitudeTextView.text = "0°"
        altitudeTextView = findViewById(R.id.GPS_Altitude_Data)
        altitudeTextView.text = "0 m.n.m."
        speedTextView = findViewById(R.id.GPS_Speed_Data)
        speedTextView.text = "0 km/h"
        gpsTimeTextView = findViewById(R.id.GPS_Time_Data)
        gpsTimeTextView.text = "00:00:00:00:00:00"
        satteliteCountTextView = findViewById(R.id.GPS_Sattelite_Data)
        satteliteCountTextView.text = "0"
    } //V pořádku

    private fun resetValues() {
        Button.text = "Start"
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
        longitudeTextView.text = "0°"
        latitudeTextView.text = "0°"
        altitudeTextView.text= "0 m.n.m."
        speedTextView.text = "0 km/h"
        gpsTimeTextView.text = "00:00:00:00:00:00"
        satteliteCountTextView.text = "0"
        linearAccelerationValueX = 0f
        linearAccelerationValueY = 0f
        linearAccelerationValueZ = 0f
        accelerationValueX = 0f
        accelerationValueY = 0f
        accelerationValueZ = 0f
        gyroscopeValueX = 0f
        gyroscopeValueY = 0f
        gyroscopeValueZ = 0f
        //GPS má svoje typy jako Long, opravit později
        latitudeValue = 0.0
        longitudeValue = 0.0
        altitudeValue = 0.0
        speedValue = 0f
        timeValue = 0L
        satelliteValue = 0
        usedSatelliteValue = 0
        stopwatchTextView.text = stopwatchText
        fileName.setText("")
        sendHardwareFile = false
    } //V pořádku
    //Updaty z venku
    private val sensorDataReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle the received sensor data here
            // Extract sensor data from the intent extras
            val sensorData: SensorData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent?.getParcelableExtra("sensorData", SensorData::class.java)
            } else {
                intent?.getParcelableExtra("sensorData")
            }

            // Update UI or perform any other necessary actions with the sensor data
            if (sensorData != null) {
                updateSensorValues(sensorData)
            }
        }
    }

    private fun updateSensorValues(sensorData: SensorData){
        var formating = 0f
        linearAccelerationValueX = sensorData.sensorData?.get(0) ?: 0f
        linearAccelerationValueY = sensorData.sensorData?.get(1) ?: 0f
        linearAccelerationValueZ = sensorData.sensorData?.get(2) ?: 0f

        accelerationValueX = sensorData.sensorData?.get(3) ?: 0f
        accelerationValueY = sensorData.sensorData?.get(4) ?: 0f
        accelerationValueZ = sensorData.sensorData?.get(5) ?: 0f

        gyroscopeValueX = sensorData.sensorData?.get(6) ?: 0f
        gyroscopeValueY = sensorData.sensorData?.get(7) ?: 0f
        gyroscopeValueZ = sensorData.sensorData?.get(8) ?: 0f
    }

    private val gpsDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle the received GPS data here
            // Extract GPS data from the intent extras
            if (intent != null) {
                latitudeValue = intent.getDoubleExtra("latitude", 0.0)
                longitudeValue = intent.getDoubleExtra("longitude", 0.0)
                altitudeValue = intent.getDoubleExtra("altitude", 0.0)
                speedValue = intent.getFloatExtra("speed", 0f)
                satelliteValue = intent.getIntExtra("satelliteCount",0)
                usedSatelliteValue = intent.getIntExtra("usedSatellites",0)
                gpsTimeTextView.text = formatTime(intent.getLongExtra("time", 0L))
            }
        }
    }

    private fun formatTime(milliseconds: Long): String { //Třebe předělat dle požadavků
       /* val yearDivider = 31_536_000_000L
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
        var secondsText = seconds.toString()*/
        //Funguje, ale hraju si tam s minusy a -1, nevím jak se bude chovat při přechodech
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliseconds
        val years = calendar.get(Calendar.YEAR) - 1970
        val months = calendar.get(Calendar.MONTH) - 0
        val days = calendar.get(Calendar.DAY_OF_MONTH) - 0
        val hours = calendar.get(Calendar.HOUR_OF_DAY) - 0
        val minutes = calendar.get(Calendar.MINUTE) - 0
        val seconds = calendar.get(Calendar.SECOND) - 0


        val yearsText = (years - 30).toString().padStart(2, '0')
        val monthsText = (months + 1).toString().padStart(2, '0')
        val daysText = days.toString().padStart(2, '0')
        val hoursText = hours.toString().padStart(2, '0')
        val minutesText = minutes.toString().padStart(2, '0')
        val secondsText = seconds.toString().padStart(2, '0')
        timeValue = "$yearsText$monthsText$daysText$hoursText$minutesText$secondsText".toLong()
        Log.d("Time Debug", timeValue.toString())
        return yearsText + ":" + monthsText + ":" + daysText + ":" + hoursText + ":" + minutesText + ":" + secondsText
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return (networkCapabilities != null) &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}