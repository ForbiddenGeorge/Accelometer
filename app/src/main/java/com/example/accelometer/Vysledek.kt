package com.example.accelometer

import android.os.Parcel
import android.os.Parcelable

data class Vysledek(var status: Boolean, var chyba: String, var kod: Int)
/*
data class SensorValue(val axis: Int, val value: Float)

data class SensorData(val sensorType: Int, val sensorValues: List<SensorValue>)
*/


data class SensorData(
    val accelerometerData: FloatArray?,
    val gyroscopeData: FloatArray?,
    val otherSensorData: FloatArray?,
    // Add more sensor data arrays as needed
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createFloatArray(),
        parcel.createFloatArray(),
        parcel.createFloatArray(),
        // Initialize more arrays as needed
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloatArray(accelerometerData)
        parcel.writeFloatArray(gyroscopeData)
        parcel.writeFloatArray(otherSensorData)
        // Write more arrays as needed
    }

    override fun describeContents(): Int {
        return 0
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