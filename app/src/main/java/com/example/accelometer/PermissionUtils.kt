package com.example.accelometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object PermissionUtils{
    private const val PREFS_NAME = "sharedPrefs"
    private const val STORAGE_PERMISSION_GRANTED_KEY = "storage_permission_granted"
    private const val HIGH_SAMPLING_PERMISSION_GRANTED_KEY = "high_sampling_permission_granted"

    fun checkAndRequestStoragePermission(activity: AppCompatActivity) {
        val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPermissionGranted = sharedPreferences.getBoolean(STORAGE_PERMISSION_GRANTED_KEY, false)

        if (!isPermissionGranted || !isStoragePermissionGranted(activity)) {
            requestStoragePermission(activity)
        } else {
            // Permission was previously granted
           Log.d("Storage", "Access already granted")
        }
    }

    private fun requestStoragePermission(activity: AppCompatActivity) {
        val requestPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted
                    savePermissionStatus(activity, true)
                } else {
                    // Permission denied
                    savePermissionStatus(activity, false)
                    Log.e("Storage", "Permission denied")
                }
            }

        // Request the permission
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun savePermissionStatus(activity: AppCompatActivity, isGranted: Boolean) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(STORAGE_PERMISSION_GRANTED_KEY, isGranted)
            apply()
        }
    }

    private fun isStoragePermissionGranted(activity: AppCompatActivity): Boolean {
        // Check the current status of the storage permission
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    fun checkAndRequestHighSamplePermission(activity: AppCompatActivity) {
        val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPermissionGranted = sharedPreferences.getBoolean(HIGH_SAMPLING_PERMISSION_GRANTED_KEY, false)

        if (!isPermissionGranted || !isHighSamplePermissionGranted(activity)) {
            requestSamplingPermission(activity)
        } else {
            // Permission was previously granted
            Log.d("High sampling", "Access already granted")
        }
    }

    @SuppressLint("InlinedApi")
    private fun isHighSamplePermissionGranted(activity: AppCompatActivity): Boolean {
        // Check the current status of the storage permission
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("InlinedApi")
    private fun requestSamplingPermission(activity: AppCompatActivity){
        val requestPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted
                    saveHighSamplingPermissionStatus(activity, true)
                } else {
                    // Permission denied
                    saveHighSamplingPermissionStatus(activity, false)
                    Log.e("High sampling", "Permission denied")
                }
            }

        // Request the permission
        requestPermissionLauncher.launch(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
    }

    private fun saveHighSamplingPermissionStatus(activity: ComponentActivity, isGranted: Boolean) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(HIGH_SAMPLING_PERMISSION_GRANTED_KEY, isGranted)
            apply()
        }
    }
}