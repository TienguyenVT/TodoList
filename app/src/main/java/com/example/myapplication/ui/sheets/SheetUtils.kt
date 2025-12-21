package com.example.myapplication.ui.sheets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

fun handleCameraClick(
    context: Context,
    onLaunch: () -> Unit,
    launcher: ManagedActivityResultLauncher<String, Boolean>
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        onLaunch()
    } else {
        launcher.launch(Manifest.permission.CAMERA)
    }
}
