package com.example.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object AudioPermissionService {

    /**
     * Determines the appropriate audio permission depending on Android version.
     * Uses READ_MEDIA_AUDIO for Android 13+ (Tiramisu, API 33+) and
     * READ_EXTERNAL_STORAGE for older versions.
     */
    val requiredAudioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * Checks if the required audio permission is already granted.
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredAudioPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the system recommends displaying a rationale for the permission.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            requiredAudioPermission
        )
    }

    /**
     * Opens the device Application Details Settings screen so the user
     * can manually grant the permission if it was permanently declined.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
