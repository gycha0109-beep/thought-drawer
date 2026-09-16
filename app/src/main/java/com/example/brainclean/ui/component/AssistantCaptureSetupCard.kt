package com.example.brainclean.ui.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.brainclean.R
import com.example.brainclean.capture.assistant.AssistantCaptureManager

@Composable
fun AssistantCaptureSetupCard() {
    val context = LocalContext.current
    val manager = remember(context) { AssistantCaptureManager(context) }

    if (!manager.isSupported()) return

    var isEnabled by remember { mutableStateOf(manager.isEnabled()) }
    var showConfirmation by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isEnabled = manager.isEnabled()
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            manager.createRoleRequestIntent()?.let(roleLauncher::launch)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.assistant_capture_setup_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isEnabled) {
                    stringResource(R.string.assistant_capture_setup_enabled_detail)
                } else {
                    stringResource(R.string.assistant_capture_setup_detail)
                },
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { showConfirmation = true },
                enabled = !isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    if (isEnabled) {
                        stringResource(R.string.assistant_capture_setup_enabled)
                    } else {
                        stringResource(R.string.assistant_capture_setup_action)
                    }
                )
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.assistant_capture_confirm_title)) },
            text = { Text(stringResource(R.string.assistant_capture_confirm_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            manager.createRoleRequestIntent()?.let(roleLauncher::launch)
                        } else {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text(stringResource(R.string.assistant_capture_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text(stringResource(R.string.assistant_capture_cancel))
                }
            }
        )
    }
}
