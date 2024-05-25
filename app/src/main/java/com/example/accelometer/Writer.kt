package com.example.accelometer

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter

/**
 * Class for handling any file work
 */

class Writer(private val context: Context) {
    private var writer: FileWriter? = null

    //Get folder path
    private fun getFolder(): File {
        return context.getExternalFilesDir(null)!!
    }

    //Get subfolder where all the files will be saved
    fun getAppSubdirectory(): File {
        val folder = getFolder()
        return File(folder, "Mereni").apply { mkdirs() }
    }

    //Create a file
    fun createFile(fileName: String) {
        Log.d("File destination:", getAppSubdirectory().toString())
        val file = File(getAppSubdirectory(), fileName)
        //Check if not that file doesn't already exists
        if (file.exists()) {
            Log.d("File already exists", "File with name $fileName already exists.")
            writer?.close()
            return
        }
        writer = FileWriter(file)
    }

    fun writeData(data: Array<String>) {
        writer?.write(data.joinToString(","))
        writer?.write("\n")
    }

    fun writeHardwareData(data: Array<String>) {
        writer?.write(data.joinToString(" ") )
        writer?.write("\n")
    }

    fun closeFile() {
        writer?.close()
        Log.d("File closed", "Soubor closed and saved")
    }
}
