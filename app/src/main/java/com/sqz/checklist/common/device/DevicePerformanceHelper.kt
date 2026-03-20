@file:Suppress("unused")

package com.sqz.checklist.common.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Determine if the current device status allows for high-power operation.
 *
 * @param context The application context.
 * @return `true` if the device is ready for high-performance operations, `false` otherwise.
 */
fun isResourceReadyForHighPerformance(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // 1. Assessing sufficiency of equipment resources
    // (based on thermal condition as the core indicator).
    // If the thermal state reaches THRESHOLD_MODERATE (medium),
    // resources are considered no longer "sufficient".
    val isResourceSufficient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        powerManager.currentThermalStatus < PowerManager.THERMAL_STATUS_MODERATE
    } else {
        true
    }

    // 2. Get battery level and charging status
    val batteryStatus: Intent? = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = (level / scale.toFloat()) * 100

    // 3. Power saving mode status
    val isPowerSaveMode = powerManager.isPowerSaveMode

    // Core logic judgment
    return isResourceSufficient && ((!isPowerSaveMode || batteryPct > 50f) || isCharging)
}

/**
 * Flow for monitoring changes in device resource status
 * for high-performance operations.
 *
 * Sample:
 * ```
 *  val isHighPerformanceMode by remember(context) {
 *      observeResourceStatus(context)
 *  }.collectAsState(initial = isResourceReadyForHighPerformance(context))
 * ```
 *
 * @param context The application context.
 * @return A [Flow] emitting `true` if the device is ready for high-performance operations,
 *   `false` otherwise.
 * @see isResourceReadyForHighPerformance
 */
fun observeResourceStatus(context: Context): Flow<Boolean> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(isResourceReadyForHighPerformance(context))
        }
    }

    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    }
    context.registerReceiver(receiver, filter)

    trySend(isResourceReadyForHighPerformance(context))

    awaitClose {
        context.unregisterReceiver(receiver)
    }
}
