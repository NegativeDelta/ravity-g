package com.example.gravity3

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.coroutines.resume

class MainActivityoLD : AppCompatActivity(), SensorEventListener {

    private var pressureReading: Float = 0.0f

    private var startingPressure: Float = 0.0f
    private var endPressure: Float = 0.0f

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private lateinit var pressureTextView: TextView
    private lateinit var timeTextView: TextView
    private var mediaRecorder: MediaRecorder? = null
    private var isListeningForSound: Boolean = false

    private var startTime: Long = 0
    private var endTime: Long = 0

    private var deltaTime = 0.0f;

    private val RECORD_AUDIO_REQUEST_CODE = 101

    private var pressureCallback: ((Float) -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pressureTextView = findViewById(R.id.pressureTextView)
        timeTextView = findViewById(R.id.timeView)
        val button: Button = findViewById(R.id.button)
        val calculateButton: Button = findViewById(R.id.calcbutton)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            Toast.makeText(this, "No Pressure Sensor Found!", Toast.LENGTH_SHORT).show()
            return
        }
        calculateButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Action when the button is touched
                    pressureTextView.text = "jajco"
                    true
                }
                else -> false
            }
        }
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (checkMicrophonePermission() and !isListeningForSound) {
                        // Assign starting pressure and register the sensor listener
                        lifecycleScope.launch {
                            startTime = System.currentTimeMillis()
                            startingPressure = getPressureReading()
                            endPressure = listenForLoudSound()

                            // pressureTextView.text = ""
                            // Thread.sleep(1000)
                        }
                        pressureTextView.text =
                            "Starting Pressure: $startingPressure hPa \n End Pressure: $endPressure hPa"

                        timeTextView.text = "Delta time: ${endTime - startTime} ms"
                    } else {
                        requestMicrophonePermission()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted, proceed with sensor registration
                sensorManager.registerListener(
                    this,
                    pressureSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required to detect sound.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            pressureReading = event.values[0]

            // Unregister the sensor listener to save battery
            sensorManager.unregisterListener(this)

            // Call the callback with the pressure reading
            pressureCallback?.invoke(pressureReading)
        }
    }

    suspend fun getPressureReading(): Float = suspendCancellableCoroutine { cont ->
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    if (cont.isActive) {
                        cont.resume(event.values[0])
                        sensorManager.unregisterListener(this)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        cont.invokeOnCancellation {
            sensorManager.unregisterListener(listener)
        }

        sensorManager.registerListener(
            listener,
            pressureSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )

    }

    private fun listenForLoudSound(): Float {
        var p: Float = 0.0f
        try {

            val outputFile = File(filesDir, "temp_audio.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            isListeningForSound = true

            thread {
                while (isListeningForSound) {

                    mediaRecorder?.maxAmplitude?.let { maxAmplitude ->
                        if (maxAmplitude > 10000) {  // Arbitrary threshold for a loud sound

                            isListeningForSound = false
                            endTime = System.currentTimeMillis()

                            runOnUiThread {
                                lifecycleScope.launch {
                                    endPressure = getPressureReading()
                                }
                                // pressureTextView.text = "End Pressure: $endPressure hPa"
                                Toast.makeText(
                                    this@MainActivityoLD,
                                    "Loud sound detected!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null

                        }
                    }
                    Thread.sleep(5)
                }

            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Error initializing MediaRecorder: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "MediaRecorder start failed: ${e.message}",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        // endTime = System.currentTimeMillis()
        return p
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something if sensor accuracy changes
    }

    override fun onResume() {
        super.onResume()
        // You may re-register the sensor listener here if needed
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        if (isListeningForSound) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }
}
