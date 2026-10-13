package com.example.sensormonitor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** The most recent reading from one sensor. */
class SensorSample(val values: FloatArray)

/**
 * Listens to every hardware sensor on the device and keeps the latest reading for each.
 * Callbacks arrive on the thread that called [start] (the main thread), so no locking is needed.
 */
class SensorMonitor(private val sensorManager: SensorManager) : SensorEventListener {

    val sensors: List<Sensor> =
        sensorManager.getSensorList(Sensor.TYPE_ALL).sortedWith(compareBy({ it.type }, { it.name }))

    private val latest = HashMap<Sensor, SensorSample>()

    fun start() {
        for (sensor in sensors) {
            // One-shot sensors fire a single event and need requestTriggerSensor(), so skip them.
            if (sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT) continue
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** A copy of the latest readings, safe to hold after the next event arrives. */
    fun snapshot(): Map<Sensor, SensorSample> = HashMap(latest)

    override fun onSensorChanged(event: SensorEvent) {
        // The framework reuses event.values, so copy it.
        latest[event.sensor] = SensorSample(event.values.clone())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
