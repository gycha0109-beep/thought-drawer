package com.example.brainclean

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtEntity
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import com.example.brainclean.ui.theme.BrainCleanTheme
import com.example.brainclean.widget.BrainCleanHomeWidget
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BrainCleanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickAddScreen(
                        onSave = { content ->
                            saveThought(content)
                        }
                    )
                }
            }
        }
    }

    private fun saveThought(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return

        lifecycleScope.launch {
            val thought = Thought(
                id = System.currentTimeMillis(),
                content = trimmedContent,
                status = ThoughtStatus.INBOX
            )

            BrainCleanDatabase.getDatabase(applicationContext)
                .thoughtDao()
                .insertThought(ThoughtEntity.fromThought(thought))

            BrainCleanHomeWidget.refreshAll(applicationContext)
            finish()
        }
    }
}

@Composable
private fun QuickAddScreen(onSave: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier.fillMaxWidth(),
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
