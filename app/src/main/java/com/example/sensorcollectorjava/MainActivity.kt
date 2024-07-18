package com.example.sensorcollectorjava

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorcollectorjava.ui.theme.SensorCollectorJavaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SensorCollectorJavaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(onClick = { startSensorService() }) {
                            Text("Start Sensor Service")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { stopSensorService() }) {
                            Text("Stop Sensor Service")
                        }
                    }
                }
            }
        }
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        startService(serviceIntent)
    }

    private fun stopSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        stopService(serviceIntent)
    }
}