package com.example.accelometer

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

/**
 * Most important activity of the entire application
 * Handles user interaction with measuring equipment
 * Calls FTP processes
 * Calls foreground service with requested data
 */
class MeasureActivity: ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var accelerationSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
    //UI for sensor data
    private lateinit var linearAccelerationTextView: TextView
    private lateinit var accelerationTextView: TextView
    private lateinit var gyroscopeTextView: TextView
    //Raw sensor data
    private var linearAccelerationValueX = 0f
    private var linearAccelerationValueY = 0f
    private var linearAccelerationValueZ = 0f
    private var accelerationValueX = 0f
    private var accelerationValueY = 0f
    private var accelerationValueZ = 0f
    private var gyroscopeValueX = 0f
    private var gyroscopeValueY = 0f
    private var gyroscopeValueZ = 0f
    //UI for GPS data
    private lateinit var longitudeTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var altitudeTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var gpsTimeTextView: TextView
    private lateinit var satelliteCountTextView: TextView
    //Raw GPS data
    private var longitudeValue: Double = 0.0
    private var latitudeValue: Double = 0.0
    private var altitudeValue: Double = 0.0
    private var speedValue = 0f
    private var timeValue:Long = 0L
    private var satelliteValue: Int = 0
    private var usedSatelliteValue: Int = 0
    //Checkboxes
    private lateinit var linearAccelerationCheckBox: CheckBox
    private lateinit var accelerationCheckBox: CheckBox
    private lateinit var gyroscopeCheckBox: CheckBox
    private lateinit var hardwareFileCheckBox: CheckBox
    private lateinit var gpsCheckBox: CheckBox
    private var accessibleSensor: Boolean = false
    //File variables
    private val csvWriter = Writer(this)
    private lateinit var fileName: EditText
    private lateinit var fileNameWhole: String
    private var sendHardwareFile: Boolean = false
    //Stopwatch variables
    private lateinit var stopwatchTextView: TextView
    private lateinit var stopWatch: Chronometer
    private var startTime: Long = 0
    private var milliseconds:Long = 0
    private var stopwatchText = "00:00:000"
    //Button
    private lateinit var Button: Button
    //Boolean for active measuring
    private var isMeasuringActive: Boolean = false
    //FTP
    private val ftpSender = FTP()
    private var scheduler: ScheduledExecutorService? = Executors.newScheduledThreadPool(2)
    //Latency variable
    private var latency = 0
    //Main thread
    private val uiHandler = Handler(Looper.getMainLooper())
/*
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
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        //Main initializing function, calls the rest of initializing functions
        //Initialize UI
        //Ask for necessary permission
        super.onCreate(savedInstanceState)
        setContentView(R.layout.measureui)
        PermissionUtils.checkAndRequestStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtils.checkAndRequestHighSamplePermission(this)
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        linearAccelerationCheckBox = findViewById(R.id.CK1)
        accelerationCheckBox = findViewById(R.id.CK2)
        gyroscopeCheckBox = findViewById(R.id.CK3)
        gpsCheckBox = findViewById(R.id.CKGPS)
        hardwareFileCheckBox = findViewById(R.id.CK4)
        gpsCheckBox.textSize = 70f
        initializeGPSTextViews()
        initializeSensorsTextViews()
        getAccessibleSensors()
        checkAndRequestNotificationPermission()
        Button = findViewById(R.id.startButton)
        stopwatchTextView = findViewById(R.id.TimeRunData)
        stopwatchTextView.text = stopwatchText
        stopWatch = findViewById(R.id.stopWatch)
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        latency = sharedPreferences.getInt("INT_KEY",0)
        Log.d("latency", latency.toString())
        fileName = findViewById(R.id.nazevSouboru)

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
            //Check if location permissions are granted
            if(gpsCheckBox.isChecked){
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
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
    //--------------------------Initializing functions--------------------------
    private fun getAccessibleSensors(){
        //If sensor is available, checkbox is clickable, otherwise not
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        accessibleSensor = sharedPreferences.getBoolean("Gyroscope_check", false)
        if (!accessibleSensor){
            gyroscopeCheckBox.isClickable = false
        }else{
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        }
        accessibleSensor = sharedPreferences.getBoolean("Accelerometer_check", true)
        if (!accessibleSensor){
            accelerationCheckBox.isClickable = false
        }else{
            accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        }
        accessibleSensor = sharedPreferences.getBoolean("Linear_Accelerometer_check", false)
        if (!accessibleSensor){
            linearAccelerationCheckBox.isClickable = false
        }else{
            linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)!!
        }
    }

    private fun initializeSensorsTextViews(){
        //Initialize UI for sensors
        linearAccelerationTextView = findViewById(R.id.laSensorDataX)
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView = findViewById(R.id.aSensorDataX)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView = findViewById(R.id.gSensorDataX)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
    }

    private fun initializeGPSTextViews(){
        //Initialize UI for GPS
        longitudeTextView = findViewById(R.id.GPS_Longitude_Data)
        longitudeTextView.text = "0°"
        latitudeTextView = findViewById(R.id.GPS_Latitude_Data)
        latitudeTextView.text = "0°"
        altitudeTextView = findViewById(R.id.GPS_Altitude_Data)
        altitudeTextView.text = "0 m.n.m."
        speedTextView = findViewById(R.id.GPS_Speed_Data)
        speedTextView.text = getString(R.string.defaultSpeedUIValue)
        gpsTimeTextView = findViewById(R.id.GPS_Time_Data)
        gpsTimeTextView.text = getString(R.string.gpsDefaultUIValue)
        satelliteCountTextView = findViewById(R.id.GPS_Sattelite_Data)
        satelliteCountTextView.text = "0"
    }

    private fun checkAndRequestNotificationPermission() {
        //Check if notification permission is granted, request otherwise
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    88
                )
            }
        }
    }

    //--------------------------Measurement start functions--------------------------
    private fun startMeasuring() {
       //Main starting function
        //Calls the rest of starting functions
        //Register receivers based on user input
        val selectedSensorTypes = getSelectedSensorTypes()
        if(gpsCheckBox.isChecked && !isLocationEnabled(this)){
            //If user checked the GPS checkbox, but location is turned off, inform user and refuse start
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
                //Sensors
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

    private fun startItAll() {
        //Calls the rest of start functions
        //Create data file
        //Start stopwatch
        //Check internet connectivity
        dataFileSetup()
        startTime = SystemClock.elapsedRealtime()
        milliseconds = System.currentTimeMillis()
        scheduler = Executors.newScheduledThreadPool(1)
        stopWatch()
        startPeriodical()
        Button.text = getString(R.string.stop)
        isMeasuringActive = true
        if(!isConnectedToInternet(this)) {
            CustomDialog.showMessage(this,
                "Internet",
                "Vaše zařízení není připojené k internetu." +
                        " Pokud chcete po ukončení měření odeslat soubory, " +
                        "připojte se během měření k internetu.")
        }
    }

    private fun dataFileSetup(){
        //Create new data file
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        fileNameWhole = "F$currentTime" + "_" + fileName.text.toString().replace(" ", "_") + ".csv"
        Log.d("Whole file name", fileNameWhole)
        csvWriter.createFile(fileNameWhole)
        val nameOfDevice = arrayOf(Build.MODEL)
        csvWriter.writeData(nameOfDevice)
    }

    private fun startPeriodical(){
        //Start periodical tasks
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
            Log.e("MeasuringActivity", "Invalid initialDelay or period for scheduling: $initialDelay, $period")
            return
        }

        try {
            scheduler?.scheduleAtFixedRate({
                //Periodical tasks
                writeDataToFile()
                updateUI()
            }, initialDelay, period, TimeUnit.MILLISECONDS)
        } catch (e: IllegalArgumentException) {
            Log.e("MeasuringActivity", "Error scheduling periodic task", e)
        }
    }

    private fun stopWatch(){
        //Start and stop stopwatch
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
    }

    private fun getSelectedSensorTypes(): IntArray {
        //Return list of selected sensors for foreground service
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

    private fun isLocationEnabled(context: Context): Boolean {
        //Check that device has enabled location
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    //--------------------------Stop measurement functions--------------------------
    private fun stopMeasuring(){
        //Main stopping function, starts all other stopping functions
        //Unregister all listeners, stop foreground service
        stopWatch()
        isMeasuringActive = false
        ForeGroundService.stopService(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsDataReceiver)
        enableDisableCheckBoxes(true)
        stopItAll()
    }

    private fun stopItAll() {
        //var message = FTPResult(false,"",0)
        //Close file, stop stopwatch, reset all variables, call FTP
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
                val message = runFTP()
                withContext(Dispatchers.Main) {
                    if (message.status) {
                        Toast.makeText(this@MeasureActivity, "Soubor úspěšně odeslán", Toast.LENGTH_SHORT).show()
                    } else {
                        CustomDialog.showMessage(this@MeasureActivity, "Chyba " + message.kod, message.chyba)
                    }
                }
            }
        }
        resetValues()
    }

    private fun runFTP(): FTPResult {
        //Call FTP class with name of the file etc.
            ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), fileNameWhole, hardwareFileCheckBox.isChecked)
            val outcome = runBlocking {
                ftpSender.queueFTP(true)
            }
            return outcome
    }

    private fun resetValues() {
        //Reset all UI and data variables to default values
        Button.text = getString(R.string.start)
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
        longitudeTextView.text = "0°"
        latitudeTextView.text = "0°"
        altitudeTextView.text= "0 m.n.m."
        speedTextView.text = getString(R.string.defaultSpeedUIValue)
        gpsTimeTextView.text = getString(R.string.gpsDefaultUIValue)
        satelliteCountTextView.text = "0"
        linearAccelerationValueX = 0f
        linearAccelerationValueY = 0f
        linearAccelerationValueZ = 0f
        accelerationValueX = 0f
        accelerationValueY = 0f
        accelerationValueZ = 0f
        gyroscopeValueX = 0f
        gyroscopeValueY = 0f
        gyroscopeValueZ = 0f
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
    }

   //--------------------------Periodically called functions--------------------------

    private val updateStopwatchRunnable: Runnable = object : Runnable {
        override fun run() {
            //Update stopwatch until measurment stops
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
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        //Update UI with possible new values and add units when possible
        if(gpsCheckBox.isChecked){
            latitudeTextView.text = latitudeValue.toString() + "°"
            longitudeTextView.text = longitudeValue.toString() + "°"
            altitudeTextView.text = altitudeValue.toString() + " m.n.m."
            speedTextView.text = (speedValue).toString() + " km/h"
            satelliteCountTextView.text = usedSatelliteValue.toString() + "/" + satelliteValue.toString()
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

    private fun writeDataToFile(){
        //Write data to file
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedMillis = currentTimeMillis - milliseconds
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
    }

    //--------------------------Additive functions--------------------------
    private fun enableDisableCheckBoxes(DisableOrEnable: Boolean) {
        //Make checkboxes either clickable or non-clickable
        linearAccelerationCheckBox.isClickable = DisableOrEnable
        accelerationCheckBox.isClickable = DisableOrEnable
        gyroscopeCheckBox.isClickable = DisableOrEnable
        hardwareFileCheckBox.isClickable = DisableOrEnable
        gpsCheckBox.isClickable = DisableOrEnable
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        //Check that device is connected to the internet
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return (networkCapabilities != null) &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    //--------------------------Sensor and GPS data receivers--------------------------
    private val sensorDataReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle the received sensor data
            // Extract sensor data from the intent extras
            val sensorData: SensorData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent?.getParcelableExtra("sensorData", SensorData::class.java)
            } else {
                intent?.getParcelableExtra("sensorData")
            }

            //Update sensor variables
            if (sensorData != null) {
                updateSensorValues(sensorData)
            }
        }
    }

    private fun updateSensorValues(sensorData: SensorData){
        //Update sensor variables values
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
            //Handle the received GPS
            //Extract GPS data from the intent extras and update local variables
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

    private fun formatTime(milliseconds: Long): String {
        //Format UNIX epoch time to proper format
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliseconds
        val years = calendar.get(Calendar.YEAR) - 1970
        val months = calendar.get(Calendar.MONTH) - 0
        val days = calendar.get(Calendar.DAY_OF_MONTH) - 0
        val hours = calendar.get(Calendar.HOUR_OF_DAY) - 0
        val minutes = calendar.get(Calendar.MINUTE) - 0
        val seconds = calendar.get(Calendar.SECOND) - 0
        //Add 0 for consistency
        val yearsText = (years - 30).toString().padStart(2, '0')
        val monthsText = (months + 1).toString().padStart(2, '0')
        val daysText = days.toString().padStart(2, '0')
        val hoursText = hours.toString().padStart(2, '0')
        val minutesText = minutes.toString().padStart(2, '0')
        val secondsText = seconds.toString().padStart(2, '0')
        timeValue = "$yearsText$monthsText$daysText$hoursText$minutesText$secondsText".toLong()
        return "$yearsText:$monthsText:$daysText:$hoursText:$minutesText:$secondsText"
    }
}