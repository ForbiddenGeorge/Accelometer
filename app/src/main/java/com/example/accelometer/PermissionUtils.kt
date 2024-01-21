package com.example.accelometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionUtils{
    private const val PREFS_NAME = "sharedPrefs"
    private const val STORAGE_PERMISSION_GRANTED_KEY = "storage_permission_granted"

    fun checkAndRequestStoragePermission(activity: ComponentActivity) {
        val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPermissionGranted = sharedPreferences.getBoolean(STORAGE_PERMISSION_GRANTED_KEY, false)

        if (!isPermissionGranted || !isStoragePermissionGranted(activity)) {
            requestStoragePermission(activity)
        } else {
            // Permission was previously granted
           Log.d("Storage", "Access already granted")
        }
    }

    private fun requestStoragePermission(activity: ComponentActivity) {
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

    private fun savePermissionStatus(activity: ComponentActivity, isGranted: Boolean) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(STORAGE_PERMISSION_GRANTED_KEY, isGranted)
            apply()
        }
    }

    private fun isStoragePermissionGranted(activity: ComponentActivity): Boolean {
        // Check the current status of the storage permission
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}