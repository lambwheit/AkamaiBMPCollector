package com.example.sensorcollectorjava

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class SensorService : Service() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var webSocketClient: WebSocketClient
    private val scope = CoroutineScope(Dispatchers.Default)
    private var filteredAccelerometerData = "0,0,0"
    private var accelerometerData = "0,0,0"
    private var gyroscopeData = "0,0,0"
    private val lowPassFilterValues = floatArrayOf(0f, 0f, 0f)
    private var sampleCount = 0
    private var lastTimestamp = 0L
    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        initWebSocket()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSensorListening()
        startSendingData()
        return START_STICKY
    }

    private fun initWebSocket() {
        val serverUri = URI("ws://192.168.50.254:1029") // Replace with your server's IP
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connection opened")
            }

            override fun onMessage(message: String?) {
                println("Received message: $message")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket connection closed")
            }

            override fun onError(ex: Exception?) {
                println("WebSocket error: ${ex?.message}")
            }
        }
        webSocketClient.connect()
    }

    private fun startSensorListening() {
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            sensorEventListener,
            gyroscope,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
    private fun applyFilters(values: FloatArray): FloatArray {
        val currentTime = System.nanoTime()
        sampleCount++

        // Calculate frequency
        val frequency = if (lastTimestamp != 0L) {
            1.0f / (sampleCount / ((currentTime - lastTimestamp) / 1.0E9f))
        } else {
            0f
        }
        lastTimestamp = currentTime

        // Low-pass filter
        val alpha = 0.18f / (frequency + 0.18f)
        for (i in 0..2) {
            lowPassFilterValues[i] = lowPassFilterValues[i] * alpha + values[i] * (1 - alpha)
        }

        // High-pass filter
        val highPassValues = FloatArray(3)
        for (i in 0..2) {
            highPassValues[i] = values[i] - lowPassFilterValues[i]
            if (highPassValues[i].isNaN() || highPassValues[i].isInfinite()) {
                highPassValues[i] = 0f
            }
        }

        // Invert values
        for (i in 0..2) {
            highPassValues[i] *= -1f
        }

        return highPassValues
    }
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val filteredValues = applyFilters(event.values)
                accelerometerData = "${event.values[0]},${event.values[1]},${event.values[2]}"
                filteredAccelerometerData = "${filteredValues[0]},${filteredValues[1]},${filteredValues[2]}"
            } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                gyroscopeData = "${event.values[0]},${event.values[1]},${event.values[2]}"
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun createJsonData(): String {
        val accelerometerValues = accelerometerData.split(",")
        val filteredAccelerometerValues = filteredAccelerometerData.split(",")
        val gyroscopeValues = gyroscopeData.split(",")

        return """
        {"timestamp": ${System.currentTimeMillis()},"accelerometer": {"x": ${accelerometerValues[0]},"y": ${accelerometerValues[1]},"z": ${accelerometerValues[2]}},"filteredAccelerometer": {"x": ${filteredAccelerometerValues[0]},"y": ${filteredAccelerometerValues[1]},"z": ${filteredAccelerometerValues[2]}},"gyroscope": {"x": ${gyroscopeValues[0]},"y": ${gyroscopeValues[1]},"z": ${gyroscopeValues[2]}}}
        """.trimIndent()
    }
    private fun startSendingData() {
        scope.launch {
            while (isActive) {
                val jsonData = createJsonData()
                webSocketClient.send(jsonData)
                delay(100) // Send data every 100ms
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
        webSocketClient.close()
        scope.cancel()
    }

    private fun createNotification(): Notification {
        val channelId = "SensorServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sensor Collector")
            .setContentText("Collecting sensor data")
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}