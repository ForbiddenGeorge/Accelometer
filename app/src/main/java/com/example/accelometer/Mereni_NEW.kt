package com.example.accelometer

import CustomDialog
import FTPSender
import Writer
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
//Není vůbec zapojen GPS a Foreground

class Mereni_NEW: AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    //Senzory
    private lateinit var linearAccelerationlSensor: Sensor
    private lateinit var accelerationSensor: Sensor
    private lateinit var gyroscopeSensor: Sensor
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
    //Pro GPS, ještě uvidím jak udělám
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
    private var miliseconds = 0
    private var stopwatchText = "00:00:000"
    //Spoždění
    private var latency = 0
    //Tlačitko
    private lateinit var Button: Button
    //Decimální formát
    private val decimalFormat = "%.6f"
    //Stav měření
    private var isMeasuringActive: Boolean = false
    //Čas počátku měření
    private var startTimeInMilliseconds: Long = 0L
    //FTP
    private val ftpSender = FTPSender()
    //Spouštěč žádostí o povolení
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.v("PERMISSION", "ACCESS Granted")
            } else {
                // Permission denied, handle it (e.g., show a message to the user)
                Log.v("PERMISSION", "ACCESS Denied")
            }
        }
    //Vlákna
    private val uiHandler = Handler(Looper.getMainLooper())
    private val sensorExecutor = Executors.newSingleThreadExecutor()
    private val locationExecutor = Executors.newSingleThreadExecutor()
    private lateinit var sensorHandlerThread: HandlerThread
    private lateinit var sensorHandler: Handler
    private lateinit var locationHandlerThread: HandlerThread
    private lateinit var locationHandler: Handler
    //Old
    private var scheduler: ScheduledExecutorService? = Executors.newScheduledThreadPool(2)

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
        //Senzory
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        linearAccelerationCheckBox = findViewById(R.id.CK1)
        accelerationCheckBox = findViewById(R.id.CK2)
        gyroscopeCheckBox = findViewById(R.id.CK3)
        gpsCheckBox = findViewById(R.id.CKGPS)
        hardwareFileCheckBox = findViewById(R.id.CK4)
        //GPS
        initializeGPSTextViews()
        //Senzory
        initializeSensorsTextViews()
        //Vypnutí/Zapnutí checkboxů dle přístupnosti senzorů
        getAccessibleSensors()
        //Tlačítko
        Button = findViewById(R.id.startButton)
        //Stopky
        stopwatchTextView = findViewById(R.id.TimeRunData)
        stopwatchTextView.text = stopwatchText
        stopWatch = findViewById(R.id.stopWatch)
        //Latence
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        latency = sharedPreferences.getInt("INT_KEY",0) * 1000
        //Okno pro název souboru
        fileName = findViewById(R.id.nazevSouboru)
        //Logika tlačítka
        Button.setOnClickListener {
            if(!isMeasuringActive){
                if(!ftpSender.isConnectedToInternet(this)){
                    CustomDialog.showMessage(this,"Chyba připojení",
                        "CZ:Zařízení není připojeno k internetu, pro FTP přenos, připojte se během měření k internetu")
                }
                startMeasuring(doRegisterSensorsChecked(),gpsCheckBox.isChecked)
            }else{
                stopMeasuring(unRegisterSensorsChecked(),gpsCheckBox.isChecked)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {
        sensorHandler.post{
            if (event!!.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION){
                linearAccelerationValueX = String.format(decimalFormat, event.values[0]).toFloat()
                linearAccelerationValueY = String.format(decimalFormat, event.values[1]).toFloat()
                linearAccelerationValueZ = String.format(decimalFormat, event.values[2]).toFloat()
                linearAccelerationTextView.text = "X = $linearAccelerationValueX \n\n" +
                        "Y= $linearAccelerationValueY \n\n" +
                        "Z= $linearAccelerationValueZ "
            }
            if(event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                accelerationValueX = String.format(decimalFormat, event.values[0]).toFloat()
                accelerationValueY = String.format(decimalFormat, event.values[1]).toFloat()
                accelerationValueZ = String.format(decimalFormat, event.values[2]).toFloat()
                accelerationTextView.text = "X = $accelerationValueX \n\n" +
                        "Y= $accelerationValueY \n\n" +
                        "Z= $accelerationValueZ "
            }
            if(event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                gyroscopeValueX = String.format(decimalFormat, event.values[0]).toFloat()
                gyroscopeValueY = String.format(decimalFormat, event.values[1]).toFloat()
                gyroscopeValueZ = String.format(decimalFormat, event.values[2]).toFloat()
                gyroscopeTextView.text = "X = $gyroscopeValueX \n\n" +
                        "Y= $gyroscopeValueY \n\n" +
                        "Z= $gyroscopeValueZ "
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("Sensors", "Accuracy changed")
    }

    private fun stopMeasuring(sensorsWanted: Boolean, gpsWanted: Boolean) {
        var zprava = Vysledek(false,"",0)
        isMeasuringActive = false
        if(sensorsWanted){
            sensorHandler.removeCallbacks(updateSensorDataRunnable)
            sensorHandlerThread.quitSafely()
        }
        if(gpsWanted){
            locationHandler.removeCallbacks(TODO("updateLocationRunnable"))
            locationHandlerThread.quitSafely()
        }
        stopWatch()
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
        //Chybí tady hodnoty pro ukládání GPS dat

        stopwatchTextView.text = stopwatchText
        fileName.setText("")
        hardwareFileCheckBox.isChecked = false
        sendHardwareFile = false
    }

    private fun runFTP(): Vysledek {
        ftpSender.init(this, csvWriter.getAppSubdirectory().toString(), fileNameWhole, hardwareFileCheckBox.isChecked)
        val outcome = runBlocking {
            // Waiting for the FTP operation to finish and capturing its result
            ftpSender.queueFTP(true)
        }
        return outcome
    }

    private fun stopGpsUpdates() {
        TODO("Zastavení GPS")
    }

    private fun unRegisterSensorsChecked(): Boolean {
        if(linearAccelerationCheckBox.isChecked || accelerationCheckBox.isChecked || gyroscopeCheckBox.isChecked){
            if(linearAccelerationCheckBox.isChecked){
                sensorManager.unregisterListener(this, linearAccelerationlSensor)
            }
            if(accelerationCheckBox.isChecked){
                sensorManager.unregisterListener(this,accelerationSensor)
            }
            if(gyroscopeCheckBox.isChecked){
                sensorManager.unregisterListener(this, gyroscopeSensor)
            }
            enableDisableCheckBoxes(true)
            return true
        }else{
            return false
        }
    }

    private fun startMeasuring(sensorsWanted: Boolean, gpsWanted: Boolean){
        //Druhý backThread
        if(gpsWanted){
            locationHandlerThread.start()
            getGpsUpdates()
        }
        //Backthread
        if(sensorsWanted) {
            sensorHandlerThread.start()
            sensorHandler = Handler(sensorHandlerThread.looper)
            sensorHandler.post(updateSensorDataRunnable)
        }
        if(gpsWanted || sensorsWanted){
            isMeasuringActive = true
            dataFileSetup()
            //startTimeInMilliseconds = System.currentTimeMillis()
            startTime = SystemClock.elapsedRealtime()
            scheduler = Executors.newScheduledThreadPool(1)
            stopWatch()
            startPeriodical()
            Button.text = "Stop"
        }else{
            Toast.makeText(this,"Nebyl zvolen žádný senzor", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getGpsUpdates(){
        scheduler?.scheduleAtFixedRate(
            {
                TODO("Tady bude volána třída/funkce z venku/jiného souboru")
            },
            1000L,
            (latency*2).toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    private val updateSensorDataRunnable: Runnable = object : Runnable {
        override fun run() {
            // Handle any periodic sensor updates if needed
            sensorHandler.postDelayed(this, 0) // Update sensor data every 20 milliseconds
        }
    }

    private fun startPeriodical(){
        scheduler?.scheduleAtFixedRate(
            {
                writeDataToFile()
            },
        0,
        latency.toLong(),
        TimeUnit.MILLISECONDS
        )

    }

    private fun writeDataToFile(){
        val dataToFile = arrayOf(
            miliseconds.toString(),
            linearAccelerationValueX.toString(),
            linearAccelerationValueY.toString(),
            linearAccelerationValueZ.toString(),
            accelerationValueX.toString(),
            accelerationValueY.toString(),
            accelerationValueZ.toString(),
            gyroscopeValueX.toString(),
            gyroscopeValueY.toString(),
            gyroscopeValueZ.toString(),
            //latitude
            //longitude
            //altitude
            //speed
            //satellites?
            //gps
        )
        csvWriter.writeData(dataToFile)
    }

    private fun dataFileSetup(){
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        fileNameWhole = "F$currentTime" + "_" + fileName.text.toString().replace(" ", "_") + ".csv"
        Log.d("Whole file name", fileNameWhole)
        csvWriter.createFile(fileNameWhole)
        val nameOfDevice = arrayOf(Build.MODEL)
        csvWriter.writeData(nameOfDevice)
    }

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
       /* val currentTime = SystemClock.elapsedRealtime()
        val elapsedTime = currentTime - startTime
        miliseconds = elapsedTime.toInt()
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        stopwatchTextView.text = String.format("%02d:%02d:%03d", minutes % 60, seconds % 60, (miliseconds % 1000))
        if(isMeasuringActive){
            stopWatch()
            //Třeba bude fungovat
        }
        else{
            stopwatchTextView.text = stopwatchText
        }*/
    }

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
    }
    private fun doRegisterSensorsChecked(): Boolean{
        if(linearAccelerationCheckBox.isChecked || accelerationCheckBox.isChecked || gyroscopeCheckBox.isChecked){
            if(linearAccelerationCheckBox.isChecked){
                sensorManager.registerListener(this, linearAccelerationlSensor, latency)
            }
            if(accelerationCheckBox.isChecked){
                sensorManager.registerListener(this, accelerationSensor, latency)
            }
            if(gyroscopeCheckBox.isChecked){
                sensorManager.registerListener(this, gyroscopeSensor, latency)
            }
            if(hardwareFileCheckBox.isChecked){
                sendHardwareFile = true
            }
            enableDisableCheckBoxes(false)
            return true
        }else{
            return false
        }
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
    }

    @SuppressLint("SetTextI18n")
    private fun initializeSensorsTextViews(){
        linearAccelerationTextView = findViewById(R.id.laSensorDataX)
        linearAccelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        accelerationTextView = findViewById(R.id.aSensorDataX)
        accelerationTextView.text = getString(R.string.SensorTextViewEmpty)
        gyroscopeTextView = findViewById(R.id.gSensorDataX)
        gyroscopeTextView.text = getString(R.string.SensorTextViewEmpty)
    }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorHandlerThread.quitSafely()
        locationHandlerThread.quitSafely()
        scheduler?.shutdown()
        sensorManager.unregisterListener(this)
    }
}