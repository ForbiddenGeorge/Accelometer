package com.example.accelometer

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class Settings : ComponentActivity() {
    private lateinit var saveButton: Button
    private lateinit var latencyEdit: EditText
    private lateinit var checkFTP: CheckBox
    private val min = MainActivity.SensorHelper.accelerometerMinDelay.toFloat()
    //FTP Data
    private lateinit var FTPHeadline: TextView
    private lateinit var FTPHost: EditText
    private lateinit var FTPName: EditText
    private lateinit var FTPPassword: EditText
    private lateinit var FTPDirectory: EditText
    private lateinit var FTPPort: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_test)

        latencyEdit = findViewById(R.id.LatenceEditText)
        saveButton = findViewById(R.id.SaveButton)
        checkFTP = findViewById(R.id.CheckFTP)
        FTPHeadline = findViewById(R.id.FTPHeadline)
        FTPHost = findViewById(R.id.FTP_Host)
        FTPName = findViewById(R.id.FTP_Username)
        FTPPassword = findViewById(R.id.FTP_Password)
        FTPDirectory = findViewById(R.id.FTP_Directory)
        FTPPort = findViewById(R.id.FTP_Port)
        loadData()
        saveButton.setOnClickListener {
            saveData()
        }
        FTPHost.visibility = View.INVISIBLE
        FTPName.visibility = View.INVISIBLE
        FTPPassword.visibility = View.INVISIBLE
        FTPDirectory.visibility = View.INVISIBLE
        FTPPort.visibility = View.INVISIBLE
        FTPHeadline.visibility = View.INVISIBLE
        checkFTP.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                FTPShow()
            }
        }
    }

    private fun saveData() {
        val savedFrequency = latencyEdit.text.toString().toInt()
        if (savedFrequency >= (min / 1000)){
            val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.apply {
                putInt("INT_KEY", savedFrequency)
                putBoolean("FTP_CHECK", checkFTP.isChecked)
            }.apply()
            Toast.makeText(this, "Preference uloženy", Toast.LENGTH_SHORT).show()
        } else {
                Toast.makeText(
                    this,
                    "Spoždění musí mít stejnou nebo vyšší hodnotu než ${min / 1000} ms",
                    Toast.LENGTH_LONG
                ).show()
            }
        if (checkFTP.isChecked){
            val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.apply {
                putString("host", FTPHost.text.toString())
                putString("username", FTPName.text.toString())
                putString("password", FTPPassword.text.toString())
                putString("directory", FTPDirectory.text.toString())
                putString("port", FTPPort.text.toString())
                putBoolean("FTP_CHECK", checkFTP.isChecked)
            }.apply()
        }
        }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedData = sharedPreferences.getInt("INT_KEY", 200)

        latencyEdit.setText(savedData.toString())
        checkFTP.isChecked = sharedPreferences.getBoolean("FTP_CHECK", true)
        if (checkFTP.isChecked) {
            FTPShow()
        }
        FTPHost.setText(sharedPreferences.getString("host", null))
        FTPName.setText(sharedPreferences.getString("username", null))
        FTPPassword.setText(sharedPreferences.getString("password", null))
        FTPDirectory.setText(sharedPreferences.getString("directory", null))
        FTPPort.setText(sharedPreferences.getString("port", null))
    }

    private fun FTPShow(){
            FTPHost.visibility = View.VISIBLE
            FTPName.visibility = View.VISIBLE
            FTPPassword.visibility = View.VISIBLE
            FTPDirectory.visibility = View.VISIBLE
            FTPPort.visibility = View.VISIBLE
            FTPHeadline.visibility = View.VISIBLE
    }



}