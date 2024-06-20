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
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gravity3.ui.theme.PressureReader
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {


    private var startingPressure: Float = 0.0f
    private var endPressure: Float = 0.0f

    private lateinit var pressureTextView: TextView
    private lateinit var timeTextView: TextView
    private var mediaRecorder: MediaRecorder? = null
    private var isListeningForSound: Boolean = false

    private var startTime: Long = 0
    private var endTime: Long = 0
    var pressureReader: PressureReader? = null
    private var deltaTime = 0.0f;

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val WRITE_FILES_CODE = 111

    private var pressureCallback: ((Float) -> Unit)? = null

    private var estFallHeight: Float = 0.0f
    private var calibrationStart: Long = 0
    private var calibrationEnd: Long = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pressureTextView = findViewById(R.id.pressureTextView)
        timeTextView = findViewById(R.id.timeView)
        val button: Button = findViewById(R.id.button)
        val calculateButton: Button = findViewById(R.id.calcbutton)

        pressureReader = PressureReader(this)

        Log.d("android version: ", Build.VERSION.RELEASE)

        calculateButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Action when the button is touched
                    val h = (endPressure - startingPressure) / 0.107f
                    val t = ((endTime-startTime-20).toFloat())/1000
                    pressureTextView.text =
                        "Starting Pressure: $startingPressure hPa \n End Pressure: $endPressure hPa \n" +
                                "Szacowana wysokość upadku to $h m"
                    timeTextView.text = "Czas spadania wyniódł $t s. Chomiki wyliczyły, że przyspieszenie ziemskie wynosi ${(2*h)/(t*t)}"

                    true
                }

                else -> false
            }
        }
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (checkMicrophonePermission() and !isListeningForSound and checkStoragePermission()) {
                        // Assign starting pressure and register the sensor listener
                        lifecycleScope.launch {
                            startTime = System.currentTimeMillis()
                            startingPressure = pressureReader!!.getAveragedPressure()
                            endPressure = listenForLoudSound(true)

                            // pressureTextView.text = ""
                            // Thread.sleep(1000)
                        }
                        pressureTextView.text =
                            "Starting Pressure: $startingPressure hPa \n End Pressure: $endPressure hPa"

                        // timeTextView.text = "Delta time: ${endTime - startTime} ms"
                    } else {
                        if (checkMicrophonePermission()) {
                            requestMicrophonePermission()
                        }
                        if (checkStoragePermission()) {
                            requestStoragePermission()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_DOWN ->{
                    // calibrationStart = System.currentTimeMillis()
                    // listenForLoudSound(false)
                    pressureReader!!.startRecording()
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

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                || Build.VERSION.RELEASE == "14"
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_REQUEST_CODE
        )
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_FILES_CODE
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
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required to detect sound.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun listenForLoudSound(wantPressure: Boolean): Float {
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
                        if (maxAmplitude > 5000) {  // Arbitrary threshold for a loud sound
                            if (wantPressure){
                                endTime = System.currentTimeMillis()


                                Thread.sleep(3000)


                                // endPressure = pressureReader?.getAveragedPressure() ?: 0f
                            }
                            else{
                                // calibrationEnd = System.currentTimeMillis()
                            }
                            isListeningForSound = false



//                                    pressureTextView.text =
//                                        "Starting Pressure: $startingPressure hPa \n End Pressure: $endPressure hPa\n"


                            // pressureTextView.text = "End Pressure: $endPressure hPa"
                            /*Toast.makeText(
                                this@MainActivity,
                                "Loud sound detected!",
                                Toast.LENGTH_SHORT
                            ).show()*/
                            endPressure = pressureReader?.getAveragedPressure() ?: 0f
                            pressureReader?.endRecording(this, startingPressure, endPressure, ((endTime-startTime-20).toFloat())/1000)
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
        // timeTextView.text = "Upadek trwał ${endTime - startTime} ms"
        return p
    }


    override fun onPause() {
        super.onPause()
        if (isListeningForSound) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }
}
