package com.example.brainclean

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.lifecycleScope
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickAddScreen(onSave = ::saveThought)
                }
            }
        }
    }

    private fun saveThought(content: String) {
        lifecycleScope.launch {
            when (
                commandService.captureThought(
                    content = content,
                    source = captureSource
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
private fun QuickAddScreen(onSave: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.quick_add_hint)) }
        )

        Button(
            onClick = { onSave(inputText) },
            enabled = inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quick_add_save))
        }
    }
}
