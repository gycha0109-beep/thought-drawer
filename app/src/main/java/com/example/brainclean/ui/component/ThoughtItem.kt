package com.example.brainclean.ui.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.brainclean.model.Thought
import java.text.DateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ThoughtItem(
    thought: Thought,
    metadataText: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    onStartSelection: (() -> Unit)? = null,
    firstButtonText: String,
    secondButtonText: String? = null,
    onFirstClick: () -> Unit,
    onSecondClick: (() -> Unit)? = null,
    onDoneClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditThought: (String) -> Unit,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    enableDoneSwipe: Boolean = true,
    enableReminder: Boolean = true
) {
    val context = LocalContext.current
    var isEditing by remember(thought.id) { mutableStateOf(false) }
    var editedText by remember(thought.id, thought.content) { mutableStateOf(thought.content) }
    var isReminderDetailsVisible by remember(thought.id, thought.remindAt) { mutableStateOf(false) }
    val reminderFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    val accessibilityActions = buildList {
        add(
            CustomAccessibilityAction("Edit thought") {
                if (!isEditing) {
                    isEditing = true
                    true
                } else {
                    false
                }
            }
        )
        add(
            CustomAccessibilityAction("Move to $firstButtonText") {
                if (!isEditing) {
                    onFirstClick()
                    true
                } else {
                    false
                }
            }
        )
        if (secondButtonText != null && onSecondClick != null) {
            add(
                CustomAccessibilityAction("Move to $secondButtonText") {
                    if (!isEditing) {
                        onSecondClick()
                        true
                    } else {
                        false
                    }
                }
            )
        }
        if (enableDoneSwipe) {
            add(
                CustomAccessibilityAction("Mark as done") {
                    if (!isEditing) {
                        onDoneClick()
                        true
                    } else {
                        false
                    }
                }
            )
        }
        add(
            CustomAccessibilityAction("Delete thought") {
                if (!isEditing) {
                    onDeleteClick()
                    true
                } else {
                    false
                }
            }
        )
        if (enableReminder) {
            add(
                CustomAccessibilityAction("Set reminder") {
                    if (!isEditing) {
                        openReminderPicker(
                            context = context,
                            initialReminder = thought.remindAt,
                            onReminderSelected = onSetReminder
                        )
                        true
                    } else {
                        false
                    }
                }
            )
        }
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (isSelectionMode) {
                    onToggleSelection?.invoke()
                }
            },
            onLongClick = {
                if (!isSelectionMode) {
                    onStartSelection?.invoke()
                }
            }
        )
        .semantics {
            customActions = accessibilityActions
        }
        .padding(vertical = 4.dp)

    val cardColors = CardDefaults.cardColors(
        containerColor = if (isSelected) Color(0xFFF3F4F6) else Color.White
    )

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!isEditing && !isSelectionMode && enableDoneSwipe) {
                        onDoneClick()
                    }
                    !isEditing && !isSelectionMode && enableDoneSwipe
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    if (!isEditing && !isSelectionMode) {
                        onDeleteClick()
                    }
                    !isEditing && !isSelectionMode
                }

                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isEditing && !isSelectionMode && enableDoneSwipe,
        enableDismissFromEndToStart = !isEditing && !isSelectionMode,
        backgroundContent = {
            ThoughtItemSwipeBackground(
                dismissState = dismissState,
                isEditing = isEditing,
                enableDoneSwipe = enableDoneSwipe
            )
        },
        content = {
            Card(
                modifier = cardModifier.then(
                    if (isSelected) {
                        Modifier.border(1.dp, Color(0xFF111827), CardDefaults.shape)
                    } else {
                        Modifier
                    }
                ),
                colors = cardColors
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Edit thought") }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val trimmedText = editedText.trim()
                                    if (trimmedText.isNotBlank()) {
                                        onEditThought(trimmedText)
                                        isEditing = false
                                    }
                                }
                            ) {
                                Text("Save")
                            }

                            OutlinedButton(
                                onClick = {
                                    editedText = thought.content
                                    isEditing = false
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    } else {
                        Text(
                            text = thought.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF111827)
                        )

                        metadataText?.let { label ->
                            Text(
                                text = label,
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }

                        if (enableReminder) {
                            thought.remindAt?.takeIf { isReminderDetailsVisible }?.let {
                                Text(
                                    text = "Reminder: ${reminderFormatter.format(it)}",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        if (!isSelectionMode) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(onClick = onFirstClick) {
                                    Text(firstButtonText)
                                }
                                if (secondButtonText != null && onSecondClick != null) {
                                    Button(onClick = onSecondClick) {
                                        Text(secondButtonText)
                                    }
                                }
                            }
                        }

                        if (!isSelectionMode) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (enableReminder) {
                                    if (thought.remindAt != null) {
                                        TextButton(
                                            onClick = {
                                                isReminderDetailsVisible = !isReminderDetailsVisible
                                            }
                                        ) {
                                            Text(
                                                if (isReminderDetailsVisible) {
                                                    "Hide reminder"
                                                } else {
                                                    "Show reminder"
                                                }
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            openReminderPicker(
                                                context = context,
                                                initialReminder = thought.remindAt,
                                                onReminderSelected = onSetReminder
                                            )
                                        }
                                    ) {
                                        Text(if (thought.remindAt == null) "Remind" else "Change reminder")
                                    }

                                    if (thought.remindAt != null) {
                                        TextButton(onClick = onClearReminder) {
                                            Text("Clear reminder")
                                        }
                                    }
                                }

                                TextButton(onClick = { isEditing = true }) {
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThoughtItemSwipeBackground(
    dismissState: SwipeToDismissBoxState,
    isEditing: Boolean,
    enableDoneSwipe: Boolean
) {
    val actionReady = dismissState.targetValue != SwipeToDismissBoxValue.Settled
    val (backgroundColor, label, alignment) = when {
        isEditing -> Triple(Color(0xFFE5E7EB), "", Alignment.Center)
        dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd && enableDoneSwipe && actionReady ->
            Triple(Color(0xFF86EFAC), "Release to mark done", Alignment.CenterStart)

        dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd && enableDoneSwipe ->
            Triple(Color(0xFFDCFCE7), "Swipe farther to mark done", Alignment.CenterStart)

        dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart && actionReady ->
            Triple(Color(0xFFFCA5A5), "Release to delete", Alignment.CenterEnd)

        dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ->
            Triple(Color(0xFFFEE2E2), "Swipe farther to delete", Alignment.CenterEnd)

        enableDoneSwipe -> Triple(Color(0xFFF3F4F6), "Swipe to manage", Alignment.Center)
        else -> Triple(Color(0xFFF3F4F6), "Swipe left to delete", Alignment.Center)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .background(backgroundColor),
        contentAlignment = alignment
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF111827)
        )
    }
}

private fun openReminderPicker(
    context: android.content.Context,
    initialReminder: Long?,
    onReminderSelected: (Long) -> Unit
) {
    val now = Calendar.getInstance()
    val initialCalendar = Calendar.getInstance().apply {
        timeInMillis = initialReminder?.takeIf { it > System.currentTimeMillis() } ?: System.currentTimeMillis()
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDateTime = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, initialCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, initialCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedDateTime.set(Calendar.MINUTE, minute)

                    if (selectedDateTime.timeInMillis <= now.timeInMillis) {
                        Toast.makeText(context, "Choose a future time", Toast.LENGTH_SHORT).show()
                    } else {
                        onReminderSelected(selectedDateTime.timeInMillis)
                    }
                },
                initialCalendar.get(Calendar.HOUR_OF_DAY),
                initialCalendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
