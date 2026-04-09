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

@Composable
fun TodayScreen(
    thoughts: List<Thought>,
    onMoveToInbox: (Long) -> Unit,
    onMoveToLater: (Long) -> Unit,
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
        Text("Today")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .testTag("today_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(thoughts, key = { it.id }) { thought ->
                ThoughtItem(
                    thought = thought,
                    firstButtonText = "Inbox",
                    secondButtonText = "Later",
                    onFirstClick = { onMoveToInbox(thought.id) },
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
