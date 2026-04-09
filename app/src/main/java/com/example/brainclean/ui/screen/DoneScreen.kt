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
fun DoneScreen(
    thoughts: List<Thought>,
    onMoveToInbox: (Long) -> Unit,
    onDeleteThought: (Thought) -> Unit,
    onEditThought: (Long, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Done")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .testTag("done_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(thoughts, key = { it.id }) { thought ->
                ThoughtItem(
                    thought = thought,
                    firstButtonText = "Inbox",
                    onFirstClick = { onMoveToInbox(thought.id) },
                    onDoneClick = {},
                    onDeleteClick = { onDeleteThought(thought) },
                    onEditThought = { onEditThought(thought.id, it) },
                    onSetReminder = {},
                    onClearReminder = {},
                    enableDoneSwipe = false,
                    enableReminder = false
                )
            }
        }
    }
}
