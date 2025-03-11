package com.solomon.shelter.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.bunkergame.data.database.entities.Card
import com.example.bunkergame.data.database.entities.Player
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*

/**
 * Find available port on the device
 */
fun findAvailablePort(startPort: Int = 8080, endPort: Int = 8085): Int {
    for (port in startPort..endPort) {
        try {
            ServerSocket(port).use { return port }
        } catch (e: IOException) {
            // Port is already in use, try next one
        }
    }
    throw IOException("No available ports found in range $startPort-$endPort")
}

/**
 * Get device IP address
 */
fun Context.getIPAddress(): String {
    val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val ipAddress = wifiManager.connectionInfo.ipAddress

    // Convert little-endian to big-endian if needed
    return String.format(
        "%d.%d.%d.%d",
        ipAddress and 0xff,
        ipAddress shr 8 and 0xff,
        ipAddress shr 16 and 0xff,
        ipAddress shr 24 and 0xff
    )
}

/**
 * Vibrate device
 */
fun Context.vibrate(duration: Long = 100) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}

/**
 * Format timestamp to readable date
 */
fun Long.formatDateTime(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * JSON serialization/deserialization helpers
 */
fun Any.toJson(): String {
    return Gson().toJson(this)
}

inline fun <reified T> String.fromJson(): T {
    return Gson().fromJson(this, object : TypeToken<T>() {}.type)
}

/**
 * Get characteristic name by ID
 */
fun Long.getCharacteristicName(): String {
    return when (this) {
        Constants.PROFESSION_ID -> "Профессия"
        Constants.HEALTH_ID -> "Здоровье"
        Constants.PSYCHOLOGY_ID -> "Психика"
        Constants.HOBBY_ID -> "Хобби"
        Constants.ADDITIONAL_INFO_ID -> "Доп. информация"
        Constants.BAGGAGE_ID -> "Багаж"
        Constants.ACTION_CARD_ID -> "Карта действия"
        else -> "Характеристика"
    }
}

/**
 * Group cards by characteristic type
 */
fun List<Card>.groupByCharacteristic(): Map<Long, List<Card>> {
    return this.groupBy { it.characteristicId }
}

/**
 * Sort players by order number
 */
fun List<Player>.sortByOrderNumber(): List<Player> {
    return this.sortedBy { it.orderNumber }
}

/**
 * Compose lifecycle observer
 */
@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}