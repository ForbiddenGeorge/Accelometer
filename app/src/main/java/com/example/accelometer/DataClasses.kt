package com.example.accelometer

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

/**
* Data classes for various needs
* FTPResult class - to get results from FTP process for proper user awareness
* FTPQueue class - for storing files that are scheduled to be sent via FTP
* SensorData class - custom class for handling sensor data array
* */

data class FTPResult(var status: Boolean, var chyba: String, var kod: Int)
data class FTPQueue(val context: Context, val localFilePath: String, val remoteFileName: String, val hardwareFile: Boolean)

data class SensorData(
    val sensorData: FloatArray?,
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.createFloatArray())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloatArray(sensorData)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorData
        return sensorData?.contentEquals(other.sensorData) ?: (other.sensorData == null)
    }

    override fun hashCode(): Int {
        return sensorData?.contentHashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<SensorData> {
        override fun createFromParcel(parcel: Parcel): SensorData {
            return SensorData(parcel)
        }

        override fun newArray(size: Int): Array<SensorData?> {
            return arrayOfNulls(size)
        }
    }
}