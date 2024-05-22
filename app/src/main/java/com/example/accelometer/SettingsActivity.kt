package com.example.accelometer

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : ComponentActivity() {
    private lateinit var saveButton: Button
    //private lateinit var latencyEdit: TextInputEditText
    private lateinit var latencyEdit: EditText
    private val min = MainActivity.SensorHelper.accelerometerMinDelay.toFloat()
    //FTP Data
    private lateinit var FTPHeadline: TextView

    private lateinit var FTPHost: TextInputLayout
    private lateinit var FTPHostEdit: TextInputEditText
    private lateinit var FTPName: TextInputLayout
    private lateinit var FTPNameEdit: TextInputEditText
    private lateinit var FTPPassword: TextInputLayout
    private lateinit var FTPPasswordEdit: TextInputEditText
    private lateinit var FTPDirectory: TextInputLayout
    private lateinit var FTPDirectoryEdit: TextInputEditText
    private lateinit var FTPPort: TextInputLayout
    private lateinit var FTPPortEdit: TextInputEditText
    private lateinit var passwordToggle: ImageButton
    //private lateinit var FTPHost: EditText
    /*private lateinit var FTPName: EditText
    private lateinit var FTPPassword: TextInputLayout
    private lateinit var FTPPasswordEdit: TextInputEditText
    private lateinit var FTPDirectory: EditText
    private lateinit var FTPPort: EditText*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settingsui)

        latencyEdit = findViewById(R.id.latencyEditText)
        saveButton = findViewById(R.id.SaveButton)
       // checkFTP = findViewById(R.id.CheckFTP)
        FTPHeadline = findViewById(R.id.FTPHeadline)
        FTPHost = findViewById(R.id.FTP_Host)
        FTPHostEdit = FTPHost.findViewById(R.id.hostEditText)

        FTPName = findViewById(R.id.FTP_Username)
        FTPNameEdit = FTPName.findViewById(R.id.usernameEditText)

        FTPPassword = findViewById(R.id.passwordInputLayout)
        FTPPasswordEdit = FTPPassword.findViewById(R.id.passwordEditText)

        FTPDirectory = findViewById(R.id.directoryInputLayout)
        FTPDirectoryEdit = FTPDirectory.findViewById(R.id.directoryEditText)

        FTPPort = findViewById(R.id.portInputLayout)
        FTPPortEdit = FTPPort.findViewById(R.id.portEditText)
        passwordToggle = findViewById<ImageButton>(R.id.passwordToggle)

        passwordToggle.setOnClickListener {
            // Toggle password visibility
            val currentInputType = FTPPasswordEdit.inputType
            FTPPasswordEdit.inputType = if (currentInputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            // Move cursor to the end of the text
            FTPPasswordEdit.setSelection(FTPPasswordEdit.text!!.length)
        }

        /*FTPHost.visibility = View.INVISIBLE
        FTPName.visibility = View.INVISIBLE
        FTPPassword.visibility = View.INVISIBLE
        FTPDirectory.visibility = View.INVISIBLE
        FTPPort.visibility = View.INVISIBLE
        FTPHeadline.visibility = View.INVISIBLE*/
        loadData()
        saveButton.setOnClickListener {
            saveData()
        }
        /*checkFTP.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                FTPShow()
            }else{
                FTPHost.visibility = View.INVISIBLE
                FTPName.visibility = View.INVISIBLE
                FTPPassword.visibility = View.INVISIBLE
                FTPDirectory.visibility = View.INVISIBLE
                FTPPort.visibility = View.INVISIBLE
                FTPHeadline.visibility = View.INVISIBLE
            }
        }*/
    }

    private fun saveData() {
        if (latencyEdit.toString() != ""){
            val savedFrequency = latencyEdit.text.toString().toInt()
            println(savedFrequency)
            if (savedFrequency >= ((min*3) / 1000)){
                val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.apply {
                    putInt("INT_KEY", savedFrequency)
                    putString("host", FTPHostEdit.text.toString())
                    putString("username", FTPNameEdit.text.toString())
                    putString("password", FTPPasswordEdit.text.toString())
                    putString("directory", FTPDirectoryEdit.text.toString())
                    putString("port", FTPPortEdit.text.toString())
                    //putBoolean("FTP_CHECK", checkFTP.isChecked)
                    Log.d("Ukládání",FTPHostEdit.text.toString() + FTPNameEdit.text.toString() + FTPPasswordEdit.text.toString() + FTPDirectoryEdit.text.toString())
                }.apply()
                Toast.makeText(this, "Preference uloženy", Toast.LENGTH_SHORT).show()
        }else {
                Toast.makeText(
                    this,
                    "Spoždění musí mít stejnou nebo vyšší hodnotu než ${(min*3) / 1000} ms",
                    Toast.LENGTH_LONG
                ).show()
            }
       /* if (checkFTP.isChecked){
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
        }*/
        }else{
            Toast.makeText(
                this,
                "Spoždění musí mít stejnou nebo vyšší hodnotu než ${(min*3) / 1000} ms",
                Toast.LENGTH_LONG
            ).show()

        }        }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedData = sharedPreferences.getInt("INT_KEY", 100)

        latencyEdit.setText(savedData.toString())
        //checkFTP.isChecked = sharedPreferences.getBoolean("FTP_CHECK", true)
       /* if (checkFTP.isChecked) {
            FTPShow()
        }*/
        FTPHostEdit.setText(sharedPreferences.getString("host", null))
        FTPNameEdit.setText(sharedPreferences.getString("username", null))
        FTPPasswordEdit.setText(sharedPreferences.getString("password", null))
        FTPDirectoryEdit.setText(sharedPreferences.getString("directory", null))
        FTPPortEdit.setText(sharedPreferences.getString("port", null))
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
