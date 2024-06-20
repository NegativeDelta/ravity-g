package com.example.gravity3.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.util.JsonReader
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getExternalFilesDirs
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.io.File
import java.util.LinkedList
import java.util.Queue
import kotlin.concurrent.thread

class PressureReader(context: Context) : SensorEventListener {
    private val previousReads: Queue<Float> = LinkedList()
    private val avgBuffer: Queue<Float> = LinkedList()

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private val maxQueueLen: Int = 20
    private val maxBufferLen: Int = 10
    private var rawReadLog: ArrayList<Float> = ArrayList<Float>()
    private var smoothedReadLog: ArrayList<Float> = ArrayList<Float>()
    private var saveReadings: Boolean = false
    private fun calcAvg(numArr: Queue<Float>): Float {
        var suma: Float = 0f
        for (num in numArr) {
            suma += num
        }
        return suma / numArr.size
    }

    fun getAveragedPressure(): Float {
        return if (previousReads.isNotEmpty()) {
            calcAvg(previousReads)
        } else {
            0f
        }
    }

    public fun startRecording(){
        rawReadLog = ArrayList<Float>()
        smoothedReadLog = ArrayList<Float>()
        saveReadings = true
    }

    public fun endRecording(context: Context, sp: Float = 0.0f, ep: Float = 0.0f, t: Float = 0f){
        saveReadings = false
        rawReadLog.add(t)
        rawReadLog.add(sp)
        rawReadLog.add(ep)
        val csvContent = rawReadLog.joinToString(separator = "\n")
        val csvContentSmooth = smoothedReadLog.joinToString(separator = "\n")
//        val file = File(context.filesDir, System.currentTimeMillis().toString() + "raw.csv")
//        val fileSmooth = File(context.filesDir, System.currentTimeMillis().toString() + "smooth.csv")
//        Log.d("saved file: ", file.absolutePath)
//        file.writeText(csvContent)
//        fileSmooth.writeText(csvContentSmooth)
        var okHttpClient: OkHttpClient = OkHttpClient()
        var request: Request = Request.Builder().url("http://192.168.0.70/").post(csvContent.toRequestBody()).build()
        okHttpClient.newCall(request).execute().use{ response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            println(response.body!!.string())
        }
    }

    init{
        Log.d("PressureReader", "OnCreate() is being executed")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            Log.e("PressureReader", "Pressure sensor not available")
        } else {
            Log.d("PressureReader", "Pressure sensor available")
            sensorManager.registerListener(
                this,
                pressureSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            //Log.d("PressureReader", "New pressure reading: ${it.values[0]}")
            thread{
                avgBuffer.add(it.values[0])
                if(avgBuffer.size > maxBufferLen){
                    previousReads.add(avgBuffer.remove())
                }
                if (previousReads.size > maxQueueLen) {
                    previousReads.remove()
                }
//                previousReads.add(it.values[0])
//                if (previousReads.size > maxQueueLen) {
//                    previousReads.remove()
//                }
            }
            //Log.d("PressureReader", "Queue size: ${previousReads.size}, Queue contents: $previousReads")
            if(saveReadings){
                rawReadLog.add(it.values[0])
                //smoothedReadLog.add(getAveragedPressure())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implement this if needed
    }

}
