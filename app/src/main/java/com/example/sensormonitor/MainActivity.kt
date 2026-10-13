package com.example.sensormonitor

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val monitor = SensorMonitor(sensorManager)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SensorScreen(monitor)
                }
            }
        }
    }
}

@Composable
fun SensorScreen(monitor: SensorMonitor) {
    val context = LocalContext.current
    var samples by remember { mutableStateOf<Map<Sensor, SensorSample>>(emptyMap()) }
    var lastRefresh by remember { mutableStateOf("--:--:--") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permission-gated sensors only stream once granted, so re-register all listeners.
        monitor.stop()
        monitor.start()
    }

    // Start listening when the screen appears and stop when it leaves, so the phone is not
    // draining power in the background.
    DisposableEffect(monitor) {
        val needed = buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())

        monitor.start()
        onDispose { monitor.stop() }
    }

    // Copy the latest readings to the UI once per second.
    LaunchedEffect(monitor) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            samples = monitor.snapshot()
            lastRefresh = formatter.format(Date())
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hardware sensors", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${monitor.sensors.size} found · refreshed $lastRefresh",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))

        if (monitor.sensors.isEmpty()) {
            Text("This device reports no sensors.")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(monitor.sensors) { sensor ->
                    SensorRow(sensor, samples[sensor])
                }
            }
        }
    }
}

@Composable
private fun SensorRow(sensor: Sensor, sample: SensorSample?) {
    val value = when {
        sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT -> "One-shot sensor, not streamed"
        sample == null -> "Waiting for data…"
        else -> sample.values.joinToString(separator = "   ") { "%.3f".format(it) }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(sensor.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "${sensor.stringType} · ${sensor.vendor} · v${sensor.version}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
