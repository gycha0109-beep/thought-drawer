package com.example.brainclean.capture.assistant

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.brainclean.R
import com.example.brainclean.capture.VoiceCaptureController
import com.example.brainclean.capture.VoiceCaptureState
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.CaptureResult
import com.example.brainclean.domain.ThoughtCommandService
import com.example.brainclean.model.CaptureSource
import com.example.brainclean.ui.theme.BrainCleanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AssistantVoiceCaptureActivity : ComponentActivity() {
    private val voiceCaptureController by lazy { VoiceCaptureController(this) }
    private val commandService by lazy {
        ThoughtCommandService(
            context = applicationContext,
            repository = ThoughtRepository(
                BrainCleanDatabase.getDatabase(applicationContext).thoughtDao()
            )
        )
    }

    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureForLockScreen()

        setContent {
            BrainCleanTheme {
                val voiceState by voiceCaptureController.state.collectAsState()

                LaunchedEffect(Unit) {
                    voiceCaptureController.startListening()
                }

                LaunchedEffect(voiceState) {
                    if (voiceState is VoiceCaptureState.Result) {
                        saveTranscript((voiceState as VoiceCaptureState.Result).text)
                    }
                }

                AssistantVoiceCaptureScreen(
                    voiceState = voiceState,
                    isSaving = isSaving,
                    onRetry = voiceCaptureController::startListening,
                    onCancel = ::finish
                )
            }
        }
    }

    override fun onDestroy() {
        voiceCaptureController.destroy()
        super.onDestroy()
    }

    private fun configureForLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun saveTranscript(transcript: String) {
        if (isSaving) return
        isSaving = true

        lifecycleScope.launch {
            when (
                commandService.captureThought(
                    content = transcript,
                    source = CaptureSource.ASSISTANT
                )
            ) {
                CaptureResult.Blank -> isSaving = false
                is CaptureResult.Success -> {
                    AssistantCaptureFeedback.saved(this@AssistantVoiceCaptureActivity)
                    delay(250)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun AssistantVoiceCaptureScreen(
    voiceState: VoiceCaptureState,
    isSaving: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    isSaving -> stringResource(R.string.assistant_capture_saving)
                    voiceState is VoiceCaptureState.Listening ->
                        stringResource(R.string.assistant_capture_listening)
                    voiceState is VoiceCaptureState.Result ->
                        stringResource(R.string.assistant_capture_saving)
                    voiceState is VoiceCaptureState.Error -> voiceState.message
                    voiceState is VoiceCaptureState.Unavailable ->
                        stringResource(R.string.voice_capture_unavailable_detail)
                    else -> stringResource(R.string.assistant_capture_listening)
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.assistant_capture_auto_save_hint),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (
                !isSaving &&
                (voiceState is VoiceCaptureState.Error ||
                    voiceState is VoiceCaptureState.Unavailable)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Text(stringResource(R.string.assistant_capture_retry))
                }
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(stringResource(R.string.assistant_capture_cancel))
            }
        }
    }
}
