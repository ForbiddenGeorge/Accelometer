package com.example.accelometer

import CustomDialog
import FTPSender
import Writer
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MereniTest: AppCompatActivity() {
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
    //Raw data
    private var longitudeValue: Double = 0.0
    private var latitudeValue: Double = 0.0
    private var altitudeValue: Double = 0.0
    private var speedValue = 0f
    private var timeValue:Long = 0L
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
    private val ftpSender = FTPSender()
    private var scheduler: ScheduledExecutorService? = Executors.newScheduledThreadPool(1)
    //Spoždění
    private var latency = 0
    //Vlákno
    private val uiHandler = Handler(Looper.getMainLooper())
    //Zaokrouhlovač
    private val decimalFormat = "%.6f"




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mereni_test)
        PermissionUtils.checkAndRequestStoragePermission(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtils.checkAndRequestHighSamplePermission(this)
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1
        )
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
        //Okno pro název souboru
        fileName = findViewById(R.id.nazevSouboru)
        //Logika tlačítka
        Button.setOnClickListener {
            if(!isMeasuringActive){
                if(!ftpSender.isConnectedToInternet(this)){
                    CustomDialog.showMessage(this,"Chyba připojení",
                        "CZ:Zařízení není připojeno k internetu, pro FTP přenos, připojte se během měření k internetu")
                }
                startMeasuring()
            }else{
                stopMeasuring()
            }
        }
        resetValues()
    }

    private fun stopMeasuring(){
        ForeGroundServiceTest.stopService(this)
        stopItAll()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsDataReceiver)
        enableDisableCheckBoxes(true)
    }

    private fun stopItAll() {
        var zprava = Vysledek(false,"",0)
        stopWatch()
        isMeasuringActive = false
        csvWriter.closeFile()
        scheduler?.shutdown()
        Toast.makeText(this, "Soubor $fileNameWhole uložen!", Toast.LENGTH_SHORT).show()
        zprava = runFTP()
        if (zprava.status)
        {
            Toast.makeText(this,"Soubor úspěšně odeslán",Toast.LENGTH_SHORT).show()
        }else{
            CustomDialog.showMessage(this,"Chyba " + zprava.kod, zprava.chyba)
        }
        resetValues()
    }

    private fun startMeasuring() {
       //doRegisterSensorsChecked()
        val selectedSensorTypes = getSelectedSensorTypes() // Správné číselné hodnoty
        if(selectedSensorTypes.isNotEmpty() || gpsCheckBox.isChecked){

            ForeGroundServiceTest.startService(
                this,
                selectedSensorTypes,
                gpsCheckBox.isChecked,
                latency
            )
            startItAll()
            //Senzory
            LocalBroadcastManager.getInstance(this).registerReceiver(
                sensorDataReceiver,
                IntentFilter("SENSOR_DATA_ACTION")
            )
            //GPS
            LocalBroadcastManager.getInstance(this).registerReceiver(
                gpsDataReceiver,
                IntentFilter("GPS_DATA_ACTION")
            )
            enableDisableCheckBoxes(false)
        }else{
            Toast.makeText(this,"Nebyl zvolen žádný senzor", Toast.LENGTH_SHORT).show()
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

    private fun runFTP(): Vysledek {
        ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), fileNameWhole, hardwareFileCheckBox.isChecked)
        val outcome = runBlocking {
            // Waiting for the FTP operation to finish and capturing its result
            ftpSender.uploadFileToFTPAsync()
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
        scheduler?.scheduleAtFixedRate(
            {
                writeDataToFile()
                updateUI()
            },
            0,
            latency.toLong(),
            TimeUnit.MILLISECONDS
        )

    } //V pořádku

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        if(gpsCheckBox.isChecked){
            latitudeTextView.text = latitudeValue.toString()
            longitudeTextView.text = longitudeValue.toString()
            altitudeTextView.text = altitudeValue.toString()
            speedTextView.text = speedValue.toString()
            gpsTimeTextView.text = timeValue.toString()
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
        longitudeTextView.text = "0"
        latitudeTextView = findViewById(R.id.GPS_Latitude_Data)
        latitudeTextView.text = "0"
        altitudeTextView = findViewById(R.id.GPS_Altitude_Data)
        altitudeTextView.text = "0"
        speedTextView = findViewById(R.id.GPS_Speed_Data)
        speedTextView.text = "0"
        gpsTimeTextView = findViewById(R.id.GPS_Time_Data)
        gpsTimeTextView.text = "00:00:00:00:00:00"
    } //V pořádku

    private fun resetValues() {
        Button.text = "Start"
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
        longitudeTextView.text = "0"
        latitudeTextView.text = "0"
        altitudeTextView.text= "0"
        speedTextView.text = "0"
        gpsTimeTextView.text = stopwatchText
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
        stopwatchTextView.text = stopwatchText
        fileName.setText("")
        hardwareFileCheckBox.isChecked = false
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
        val accelerometerData = sensorData.accelerometerData
        val gyroscopeData = sensorData.gyroscopeData
        val linearAccelerometerData = sensorData.otherSensorData
        linearAccelerationValueX = linearAccelerometerData?.get(0) ?: 0f
        linearAccelerationValueY = linearAccelerometerData?.get(1) ?: 0f
        linearAccelerationValueZ = linearAccelerometerData?.get(2) ?: 0f

        accelerationValueX = accelerometerData?.get(0) ?: 0f
        accelerationValueY = accelerometerData?.get(1) ?: 0f
        accelerationValueZ = accelerometerData?.get(2) ?: 0f

        gyroscopeValueX = gyroscopeData?.get(0) ?: 0f
        gyroscopeValueY = gyroscopeData?.get(1) ?: 0f
        gyroscopeValueZ = gyroscopeData?.get(2) ?: 0f
    }

    private val gpsDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle the received GPS data here
            // Extract GPS data from the intent extras
            val latitude = intent?.getDoubleExtra("latitude", 0.0)
            val longitude = intent?.getDoubleExtra("longitude", 0.0)
            val altitude = intent?.getDoubleExtra("altitude", 0.0)
            val speed = intent?.getFloatExtra("speed", 0f)
            val time = intent?.getLongExtra("time", 0L)

            // Update UI or perform any other necessary actions with the GPS data
        }
    }
}