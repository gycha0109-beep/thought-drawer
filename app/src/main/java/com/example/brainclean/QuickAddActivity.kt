package com.example.brainclean

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.brainclean.capture.VoiceCaptureController
import com.example.brainclean.capture.VoiceCaptureState
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.CaptureResult
import com.example.brainclean.domain.ThoughtCommandService
import com.example.brainclean.model.CaptureSource
import com.example.brainclean.ui.theme.BrainCleanTheme
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {
    private val commandService by lazy {
        ThoughtCommandService(
            context = applicationContext,
            repository = ThoughtRepository(
                BrainCleanDatabase.getDatabase(applicationContext).thoughtDao()
            )
        )
    }

    private val voiceCaptureController by lazy {
        VoiceCaptureController(this)
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceCaptureController.startListening()
        }
    }

    private val captureSource: CaptureSource by lazy {
        intent.getStringExtra(EXTRA_CAPTURE_SOURCE)
            ?.let { sourceName ->
                CaptureSource.entries.firstOrNull { it.name == sourceName }
            }
            ?: CaptureSource.WIDGET
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        setContent {
            BrainCleanTheme {
                val voiceState by voiceCaptureController.state.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickAddScreen(
                        voiceState = voiceState,
                        onVoiceClick = ::requestVoiceCapture,
                        onSave = { content, usedVoiceInput ->
                            saveThought(
                                content = content,
                                source = if (usedVoiceInput) CaptureSource.VOICE else captureSource
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        voiceCaptureController.destroy()
        super.onDestroy()
    }

    private fun requestVoiceCapture() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            voiceCaptureController.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun saveThought(content: String, source: CaptureSource) {
        lifecycleScope.launch {
            when (
                commandService.captureThought(
                    content = content,
                    source = source
                )
            ) {
                CaptureResult.Blank -> Unit
                is CaptureResult.Success -> finish()
            }
        }
    }

    companion object {
        const val EXTRA_CAPTURE_SOURCE = "extra_capture_source"
    }
}

@Composable
private fun QuickAddScreen(
    voiceState: VoiceCaptureState,
    onVoiceClick: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var usedVoiceInput by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceCaptureState.Result) {
            inputText = voiceState.text
            usedVoiceInput = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.quick_add_hint)) }
        )

        OutlinedButton(
            onClick = {
                keyboardController?.hide()
                onVoiceClick()
            },
            enabled = voiceState !is VoiceCaptureState.Listening,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (voiceState) {
                    VoiceCaptureState.Idle,
                    is VoiceCaptureState.Result -> stringResource(R.string.voice_capture_action)
                    VoiceCaptureState.Listening -> stringResource(R.string.voice_capture_listening)
                    VoiceCaptureState.Unavailable -> stringResource(R.string.voice_capture_unavailable)
                    is VoiceCaptureState.Error -> stringResource(R.string.voice_capture_retry)
                }
            )
        }

        when (voiceState) {
            is VoiceCaptureState.Error -> Text(voiceState.message)
            VoiceCaptureState.Unavailable -> Text(stringResource(R.string.voice_capture_unavailable_detail))
            else -> Unit
        }

        Button(
            onClick = { onSave(inputText, usedVoiceInput) },
            enabled = inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quick_add_save))
        }
    }
}
