package com.example.brainclean.capture.assistant

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.brainclean.R
import com.example.brainclean.capture.VoiceCaptureController
import com.example.brainclean.capture.VoiceCaptureState
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.CaptureResult
import com.example.brainclean.domain.ThoughtCommandService
import com.example.brainclean.model.CaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ThoughtVoiceSession(
    context: Context
) : VoiceInteractionSession(context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val voiceCaptureController = VoiceCaptureController(context)
    private val commandService = ThoughtCommandService(
        context = context.applicationContext,
        repository = ThoughtRepository(
            BrainCleanDatabase.getDatabase(context.applicationContext).thoughtDao()
        )
    )

    private var statusText: TextView? = null
    private var retryButton: Button? = null
    private var startedForCurrentShow = false
    private var saving = false

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            voiceCaptureController.state.collectLatest(::handleVoiceState)
        }
    }

    override fun onCreateContentView(): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = GradientDrawable().apply {
                color = Color.WHITE
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        statusText = TextView(context).apply {
            text = context.getString(R.string.assistant_capture_listening)
            textSize = 20f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        panel.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val hint = TextView(context).apply {
            text = context.getString(R.string.assistant_capture_auto_save_hint)
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }
        panel.addView(
            hint,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        retryButton = Button(context).apply {
            text = context.getString(R.string.assistant_capture_retry)
            visibility = View.GONE
            setOnClickListener { startCapture() }
        }
        panel.addView(
            retryButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        val cancelButton = Button(context).apply {
            text = context.getString(R.string.assistant_capture_cancel)
            setOnClickListener { finish() }
        }
        panel.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        return panel
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (!startedForCurrentShow) {
            startedForCurrentShow = true
            startCapture()
        }
    }

    override fun onHide() {
        if (!saving) {
            voiceCaptureController.destroy()
        }
        startedForCurrentShow = false
        super.onHide()
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        voiceCaptureController.destroy()
        scope.cancel()
        super.onDestroy()
    }

    private fun startCapture() {
        if (saving) return
        retryButton?.visibility = View.GONE
        statusText?.text = context.getString(R.string.assistant_capture_listening)
        voiceCaptureController.startListening()
    }

    private suspend fun handleVoiceState(state: VoiceCaptureState) {
        when (state) {
            VoiceCaptureState.Idle -> Unit
            VoiceCaptureState.Listening -> {
                retryButton?.visibility = View.GONE
                statusText?.text = context.getString(R.string.assistant_capture_listening)
            }

            VoiceCaptureState.Unavailable -> {
                statusText?.text = context.getString(R.string.voice_capture_unavailable_detail)
                retryButton?.visibility = View.VISIBLE
            }

            is VoiceCaptureState.Error -> {
                statusText?.text = state.message
                retryButton?.visibility = View.VISIBLE
            }

            is VoiceCaptureState.Result -> saveTranscript(state.text)
        }
    }

    private suspend fun saveTranscript(transcript: String) {
        if (saving) return
        saving = true
        statusText?.text = context.getString(R.string.assistant_capture_saving)
        retryButton?.visibility = View.GONE

        when (
            commandService.captureThought(
                content = transcript,
                source = CaptureSource.ASSISTANT
            )
        ) {
            CaptureResult.Blank -> {
                saving = false
                statusText?.text = context.getString(R.string.assistant_capture_no_result)
                retryButton?.visibility = View.VISIBLE
            }

            is CaptureResult.Success -> {
                statusText?.text = context.getString(R.string.assistant_capture_saved)
                AssistantCaptureFeedback.saved(context)
                delay(250)
                finish()
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
