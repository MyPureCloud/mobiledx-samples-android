package com.genesys.cloud.messenger.sample.data

import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionHandler(
    activity: AppCompatActivity
) {

    companion object {
        private const val TAG = "PermissionHandler"

        // android.Manifest.permission.POST_NOTIFICATIONS needs API 33
        const val PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
    }

    private var permissionGranted: (() -> Unit)? = null
    private var permissionDenied: (() -> Unit)? = null
    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                permissionGranted?.invoke()
            } else {
                permissionDenied?.invoke()
            }
        }

    fun requestPermission(
        permission: String,
        permissionGranted: (() -> Unit)?,
        permissionDenied: (() -> Unit)?
    ) {
        Log.d(TAG, "requestPermission($permission)")
        this.permissionGranted = permissionGranted
        this.permissionDenied = permissionDenied
        requestPermissionLauncher.launch(permission)
    }
}