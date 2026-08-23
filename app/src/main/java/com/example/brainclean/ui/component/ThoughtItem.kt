package com.example.brainclean.ui.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.brainclean.R
import com.example.brainclean.capture.ThoughtShareLauncher
import com.example.brainclean.model.Thought
import java.text.DateFormat
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    enableDoneAction: Boolean = true,
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
        if (enableDoneAction) {
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
            CustomAccessibilityAction("Share thought") {
                if (!isEditing) {
                    ThoughtShareLauncher.share(context, thought.content)
                    true
                } else {
                    false
                }
            }
        )
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
        .padding(vertical = 3.dp)

    val cardColors = CardDefaults.cardColors(
        containerColor = if (isSelected) Color(0xFFF3F4F6) else Color.White
    )

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
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
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
                        .padding(top = 8.dp),
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
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }

                if (enableReminder) {
                    thought.remindAt?.takeIf { isReminderDetailsVisible }?.let {
                        Text(
                            text = "Reminder: ${reminderFormatter.format(it)}",
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (!isSelectionMode) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (enableReminder && thought.remindAt != null) {
                            IconButton(
                                onClick = {
                                    isReminderDetailsVisible = !isReminderDetailsVisible
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_reminder_info),
                                    contentDescription = if (isReminderDetailsVisible) {
                                        "Hide reminder details"
                                    } else {
                                        "Show reminder details"
                                    }
                                )
                            }
                        }

                        if (enableReminder) {
                            IconButton(
                                onClick = {
                                    openReminderPicker(
                                        context = context,
                                        initialReminder = thought.remindAt,
                                        onReminderSelected = onSetReminder
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_reminder_set),
                                    contentDescription = if (thought.remindAt == null) {
                                        "Set reminder"
                                    } else {
                                        "Change reminder"
                                    }
                                )
                            }

                            if (thought.remindAt != null) {
                                IconButton(onClick = onClearReminder) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_action_reminder_clear),
                                        contentDescription = "Clear reminder"
                                    )
                                }
                            }
                        }

                        if (enableDoneAction) {
                            IconButton(onClick = onDoneClick) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_done),
                                    contentDescription = "Mark as done"
                                )
                            }
                        }

                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_edit),
                                contentDescription = "Edit thought"
                            )
                        }

                        IconButton(
                            onClick = {
                                ThoughtShareLauncher.share(context, thought.content)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_share),
                                contentDescription = "Share thought"
                            )
                        }

                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_delete),
                                contentDescription = "Delete thought",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openReminderPicker(
    context: android.content.Context,
    initialReminder: Long?,
    onReminderSelected: (Long) -> Unit
) {
    val now = Calendar.getInstance()
    val initialCalendar = Calendar.getInstance().apply {
        timeInMillis = initialReminder?.takeIf { it > System.currentTimeMillis() }
            ?: System.currentTimeMillis()
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
