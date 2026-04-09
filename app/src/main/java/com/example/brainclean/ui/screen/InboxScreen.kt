package com.example.brainclean.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.brainclean.model.Thought
import com.example.brainclean.ui.component.ThoughtItem
import com.example.brainclean.ui.component.formatThoughtTimestamp

@Composable
fun InboxScreen(
    thoughts: List<Thought>,
    selectedThoughtIds: Set<Long>,
    onAddThought: (String) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onStartSelection: (Long) -> Unit,
    onMoveToToday: (Long) -> Unit,
    onMoveToLater: (Long) -> Unit,
    onMarkDone: (Thought) -> Unit,
    onDeleteThought: (Thought) -> Unit,
    onEditThought: (Long, String) -> Unit,
    onSetReminder: (Long, Long) -> Unit,
    onClearReminder: (Long) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inbox_input"),
            label = { Text("Add a thought") }
        )

        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    onAddThought(inputText.trim())
                    inputText = ""
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("add_thought_button")
        ) {
            Text("Add")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .testTag("inbox_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(thoughts, key = { it.id }) { thought ->
                ThoughtItem(
                    thought = thought,
                    metadataText = formatThoughtTimestamp("Created", thought.createdAt),
                    isSelectionMode = selectedThoughtIds.isNotEmpty(),
                    isSelected = thought.id in selectedThoughtIds,
                    onToggleSelection = { onToggleSelection(thought.id) },
                    onStartSelection = { onStartSelection(thought.id) },
                    firstButtonText = "Today",
                    secondButtonText = "Later",
                    onFirstClick = { onMoveToToday(thought.id) },
                    onSecondClick = { onMoveToLater(thought.id) },
                    onDoneClick = { onMarkDone(thought) },
                    onDeleteClick = { onDeleteThought(thought) },
                    onEditThought = { onEditThought(thought.id, it) },
                    onSetReminder = { onSetReminder(thought.id, it) },
                    onClearReminder = { onClearReminder(thought.id) }
                )
            }
        }
    }
}
