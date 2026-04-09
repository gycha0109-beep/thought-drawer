package com.example.brainclean.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.brainclean.model.Thought
import com.example.brainclean.ui.component.ThoughtItem
import com.example.brainclean.ui.component.formatThoughtTimestamp

@Composable
fun LaterScreen(
    thoughts: List<Thought>,
    selectedThoughtIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onStartSelection: (Long) -> Unit,
    onMoveToInbox: (Long) -> Unit,
    onMoveToToday: (Long) -> Unit,
    onMarkDone: (Thought) -> Unit,
    onDeleteThought: (Thought) -> Unit,
    onEditThought: (Long, String) -> Unit,
    onSetReminder: (Long, Long) -> Unit,
    onClearReminder: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Later")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .testTag("later_list"),
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
                    firstButtonText = "Inbox",
                    secondButtonText = "Today",
                    onFirstClick = { onMoveToInbox(thought.id) },
                    onSecondClick = { onMoveToToday(thought.id) },
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
