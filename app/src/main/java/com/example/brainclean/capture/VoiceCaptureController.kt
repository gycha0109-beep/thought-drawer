package com.example.brainclean.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceCaptureState {
    data object Idle : VoiceCaptureState
    data object Listening : VoiceCaptureState
    data class Result(val text: String) : VoiceCaptureState
    data class Error(val message: String) : VoiceCaptureState
    data object Unavailable : VoiceCaptureState
}

class VoiceCaptureController(
    context: Context
) : RecognitionListener {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<VoiceCaptureState>(VoiceCaptureState.Idle)
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = VoiceCaptureState.Error("마이크 권한을 확인해 주세요")
            return
        }

        val recognizer = getOrCreateRecognizer() ?: run {
            _state.value = VoiceCaptureState.Unavailable
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            _state.value = VoiceCaptureState.Listening
            recognizer.startListening(intent)
        } catch (_: SecurityException) {
            _state.value = VoiceCaptureState.Error("마이크 권한을 확인해 주세요")
        } catch (_: RuntimeException) {
            _state.value = VoiceCaptureState.Error("음성 인식을 시작하지 못했습니다")
        }
    }

    fun destroy() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceCaptureState.Idle
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        speechRecognizer?.let { return it }

        val recognizer = tryCreateOnDeviceRecognizer()
            ?: tryCreateDefaultRecognizer()

        recognizer?.setRecognitionListener(this)
        speechRecognizer = recognizer
        return recognizer
    }

    private fun tryCreateOnDeviceRecognizer(): SpeechRecognizer? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)) return null

        return try {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } catch (_: UnsupportedOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun tryCreateDefaultRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return null

        return try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = VoiceCaptureState.Listening
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성이 들리지 않았습니다"
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "음성 인식 네트워크를 확인해 주세요"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한을 확인해 주세요"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식이 이미 실행 중입니다"
            else -> "음성 인식에 실패했습니다"
        }
        _state.value = VoiceCaptureState.Error(message)
    }

    override fun onResults(results: Bundle?) {
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        _state.value = if (transcript.isBlank()) {
            VoiceCaptureState.Error("음성을 인식하지 못했습니다")
        } else {
            VoiceCaptureState.Result(transcript)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
