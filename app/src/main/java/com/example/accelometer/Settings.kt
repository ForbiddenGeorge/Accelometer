package com.example.accelometer

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class Settings : ComponentActivity() {
    private lateinit var saveButton: Button
    private lateinit var latencyEdit: EditText
    private val min = MainActivity.SensorHelper.accelerometerMinDelay.toFloat()
    //FTP Data
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
        FTPHost = findViewById(R.id.FTP_Host)
        FTPName = findViewById(R.id.FTP_Username)
        FTPPassword = findViewById(R.id.FTP_Password)
        FTPDirectory = findViewById(R.id.FTP_Directory)
        FTPPort = findViewById(R.id.FTP_Port)
        loadData()
        saveButton.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val savedData = latencyEdit.text.toString().toInt()
        if (savedData > (min / 1000) && FTPHost.text != null && FTPName.text != null && FTPPassword.text != null && FTPDirectory.text != null) {
            val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.apply {
                putInt("INT_KEY", savedData)
                putString("host", FTPHost.text.toString())
                putString("username", FTPName.text.toString())
                putString("password", FTPPassword.text.toString())
                putString("directory", FTPDirectory.text.toString())
                putString("port", FTPPort.text.toString())
            }.apply()
            Toast.makeText(this, "Preference uloženy", Toast.LENGTH_SHORT).show()
        } else {
            if(savedData < (min / 1000)){
                Toast.makeText(
                    this,
                    "Spoždění musí mít stejnou nebo vyšší hodnotu než ${min / 1000} ms",
                    Toast.LENGTH_LONG
                ).show()
            }else{
                Toast.makeText(
                    this,
                    "Je třeba vyplnit všechna pole",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedData = sharedPreferences.getInt("INT_KEY", 200)

        latencyEdit.setText(savedData.toString())
        FTPHost.setText(sharedPreferences.getString("host", null))
        FTPName.setText(sharedPreferences.getString("username", null))
        FTPPassword.setText(sharedPreferences.getString("password", null))
        FTPDirectory.setText(sharedPreferences.getString("directory", null))
        FTPPort.setText(sharedPreferences.getString("port", null))
    }

}