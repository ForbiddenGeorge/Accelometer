package com.example.accelometer

import android.os.Parcel
import android.os.Parcelable

data class Vysledek(var status: Boolean, var chyba: String, var kod: Int)
/*
data class SensorValue(val axis: Int, val value: Float)

data class SensorData(val sensorType: Int, val sensorValues: List<SensorValue>)
*/
/*
data class SensorData(
    val linearAccelerationData: FloatArray?,
    val accelerometerData: FloatArray?,
    val gyroscopeData: FloatArray?,
    // Add more sensor data arrays as needed
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createFloatArray(),
        parcel.createFloatArray(),
        parcel.createFloatArray(),
        // Initialize more arrays as needed
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloatArray(linearAccelerationData)
        parcel.writeFloatArray(accelerometerData)
        parcel.writeFloatArray(gyroscopeData)
        // Write more arrays as needed
    }

    override fun describeContents(): Int {
        return 0
    }
    //Okay nevím co tady to je, uvidíme
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (linearAccelerationData != null) {
            if (other.linearAccelerationData == null) return false
            if (!linearAccelerationData.contentEquals(other.linearAccelerationData)) return false
        } else if (other.linearAccelerationData != null) return false
        if (accelerometerData != null) {
            if (other.accelerometerData == null) return false
            if (!accelerometerData.contentEquals(other.accelerometerData)) return false
        } else if (other.accelerometerData != null) return false
        if (gyroscopeData != null) {
            if (other.gyroscopeData == null) return false
            if (!gyroscopeData.contentEquals(other.gyroscopeData)) return false
        } else if (other.gyroscopeData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = linearAccelerationData?.contentHashCode() ?: 0
        result = 31 * result + (accelerometerData?.contentHashCode() ?: 0)
        result = 31 * result + (gyroscopeData?.contentHashCode() ?: 0)
        return result
    }
    //HMMMM

    companion object CREATOR : Parcelable.Creator<SensorData> {
        override fun createFromParcel(parcel: Parcel): SensorData {
            return SensorData(parcel)
        }

        override fun newArray(size: Int): Array<SensorData?> {
            return arrayOfNulls(size)
        }
    }
}*/

data class SensorData(
    val sensorData: FloatArray?,
    // Add more sensor data arrays as needed
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