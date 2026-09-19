package com.example.nativeaccent.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.nativeaccent.R
import com.example.nativeaccent.ui.theme.InkElevated
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary

/**
 * Gate for RECORD_AUDIO.
 *
 * [MicPermissionState.request] either runs [onGranted] straight away, shows the
 * system prompt, or — once the user has said no — shows a rationale dialog that
 * can re-ask or send them to app settings.
 */
class MicPermissionState internal constructor(
    private val requestPermission: () -> Unit,
    private val isGranted: () -> Boolean,
    private val runAction: () -> Unit,
) {
    /** Runs the guarded action, asking for the microphone first if needed. */
    fun request() {
        if (isGranted()) runAction() else requestPermission()
    }

    val granted: Boolean get() = isGranted()
}

@Composable
fun rememberMicPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {},
): MicPermissionState {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)

    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            currentOnGranted()
        } else {
            currentOnDenied()
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            containerColor = InkElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text(stringResource(R.string.permission_grant)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text(stringResource(R.string.permission_settings)) }
            },
        )
    }

    return remember(launcher, context) {
        MicPermissionState(
            requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            isGranted = { hasMicPermission(context) },
            runAction = { currentOnGranted() },
        )
    }
}

private fun hasMicPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
