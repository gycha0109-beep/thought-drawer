package com.example.brainclean.ui.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtItem(
    thought: Thought,
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

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!isEditing && enableDoneSwipe) {
                        onDoneClick()
                    }
                    !isEditing && enableDoneSwipe
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    if (!isEditing) {
                        onDeleteClick()
                    }
                    true
                }

                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isEditing && enableDoneSwipe,
        enableDismissFromEndToStart = !isEditing,
        backgroundContent = {
            ThoughtItemSwipeBackground(
                dismissState = dismissState,
                isEditing = isEditing,
                enableDoneSwipe = enableDoneSwipe
            )
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        customActions = accessibilityActions
                    }
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text(text = thought.content)

                        if (enableReminder) {
                            val reminderLabel = thought.remindAt?.let {
                                "Reminder: ${reminderFormatter.format(it)}"
                            } ?: "No reminder"

                            Text(
                                text = reminderLabel,
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color(0xFF6B7280)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            if (enableReminder) {
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThoughtItemSwipeBackground(
    dismissState: SwipeToDismissBoxState,
    isEditing: Boolean,
    enableDoneSwipe: Boolean
) {
    val (backgroundColor, label, alignment) = when {
        isEditing -> Triple(Color(0xFFE5E7EB), "", Alignment.Center)
        dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd && enableDoneSwipe ->
            Triple(Color(0xFFDCFCE7), "Done", Alignment.CenterStart)

        dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ->
            Triple(Color(0xFFFEE2E2), "Delete", Alignment.CenterEnd)

        enableDoneSwipe -> Triple(Color(0xFFF3F4F6), "Swipe to manage", Alignment.Center)
        else -> Triple(Color(0xFFF3F4F6), "Swipe left to delete", Alignment.Center)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp)
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
