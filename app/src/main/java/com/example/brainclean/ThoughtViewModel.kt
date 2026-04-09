package com.example.brainclean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtEntity
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import com.example.brainclean.reminder.ReminderSyncResult
import com.example.brainclean.reminder.ThoughtReminderScheduler
import com.example.brainclean.widget.BrainCleanHomeWidget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class ThoughtTab {
    INBOX,
    TODAY,
    LATER,
    DONE
}

private fun defaultSearchQueries(): Map<ThoughtTab, String> {
    return ThoughtTab.entries.associateWith { "" }
}

data class ThoughtUiState(
    val thoughts: List<Thought> = emptyList(),
    val selectedTab: ThoughtTab = ThoughtTab.INBOX,
    val searchQueries: Map<ThoughtTab, String> = defaultSearchQueries(),
    val sortRecentFirst: Boolean = true
)

sealed interface ThoughtUiEvent {
    data object RequestExactAlarmPermission : ThoughtUiEvent
    data object RequestNotificationPermission : ThoughtUiEvent
}

class ThoughtViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val thoughtDao = BrainCleanDatabase.getDatabase(application).thoughtDao()
    private val reminderScheduler = ThoughtReminderScheduler(application)
    private val _uiState = MutableStateFlow(
        ThoughtUiState(
            selectedTab = restoreSelectedTab(),
            searchQueries = restoreSearchQueries(),
            sortRecentFirst = savedStateHandle[SORT_RECENT_FIRST_KEY] ?: true
        )
    )
    val uiState: StateFlow<ThoughtUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ThoughtUiEvent>()
    val events: SharedFlow<ThoughtUiEvent> = _events.asSharedFlow()

    private var hasSeededDatabase = false

    private val defaultThoughts = listOf(
        Thought(1, "Write down the idea for tomorrow's workout", ThoughtStatus.INBOX, null),
        Thought(2, "Break the project into the first small step", ThoughtStatus.INBOX, null)
    )

    init {
        observeThoughts()
    }

    fun addThought(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return

        viewModelScope.launch {
            val newThought = Thought(
                id = System.currentTimeMillis(),
                content = trimmedContent,
                status = ThoughtStatus.INBOX,
                remindAt = null
            )

            thoughtDao.insertThought(ThoughtEntity.fromThought(newThought))
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun updateThoughtStatus(id: Long, status: ThoughtStatus) {
        val existingThought = uiState.value.thoughts.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            val updatedThought = if (status == ThoughtStatus.DONE) {
                existingThought.copy(status = status, remindAt = null)
            } else {
                existingThought.copy(status = status)
            }
            thoughtDao.updateThought(
                ThoughtEntity.fromThought(updatedThought)
            )
            syncReminder(updatedThought)
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun updateThoughtContent(id: Long, content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return

        val existingThought = uiState.value.thoughts.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            val updatedThought = existingThought.copy(content = trimmedContent)
            thoughtDao.updateThought(ThoughtEntity.fromThought(updatedThought))
            syncReminder(updatedThought)
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun updateThoughtReminder(id: Long, remindAt: Long?) {
        val existingThought = uiState.value.thoughts.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            val updatedThought = existingThought.copy(remindAt = remindAt)
            thoughtDao.updateThought(ThoughtEntity.fromThought(updatedThought))
            syncReminder(updatedThought)
        }
    }

    fun markThoughtDone(id: Long) {
        updateThoughtStatus(id, ThoughtStatus.DONE)
    }

    fun deleteThought(id: Long) {
        val existingThought = uiState.value.thoughts.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            thoughtDao.deleteThought(ThoughtEntity.fromThought(existingThought))
            reminderScheduler.cancelReminder(existingThought.id)
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun restoreThought(thought: Thought) {
        viewModelScope.launch {
            thoughtDao.insertThought(ThoughtEntity.fromThought(thought))
            syncReminder(thought)
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun syncScheduledReminders() {
        viewModelScope.launch {
            uiState.value.thoughts.fold(ReminderSyncResult()) { acc, thought ->
                acc + syncReminder(thought, emitEvents = false)
            }
        }
    }

    fun updateSelectedTab(tab: ThoughtTab) {
        savedStateHandle[SELECTED_TAB_KEY] = tab.name
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun updateSearchQuery(tab: ThoughtTab, query: String) {
        savedStateHandle[searchQueryKey(tab)] = query
        _uiState.value = _uiState.value.copy(
            searchQueries = _uiState.value.searchQueries + (tab to query)
        )
    }

    fun setSortRecentFirst(sortRecentFirst: Boolean) {
        savedStateHandle[SORT_RECENT_FIRST_KEY] = sortRecentFirst
        _uiState.value = _uiState.value.copy(sortRecentFirst = sortRecentFirst)
    }

    private fun observeThoughts() {
        viewModelScope.launch {
            thoughtDao.observeAllThoughts().collect { thoughtEntities ->
                if (thoughtEntities.isEmpty() && !hasSeededDatabase) {
                    hasSeededDatabase = true
                    defaultThoughts.forEach { thought ->
                        thoughtDao.insertThought(ThoughtEntity.fromThought(thought))
                    }
                    BrainCleanHomeWidget.refreshAll(getApplication())
                } else {
                    _uiState.value = _uiState.value.copy(
                        thoughts = thoughtEntities.map { it.toThought() }
                    )
                }
            }
        }
    }

    private fun restoreSelectedTab(): ThoughtTab {
        val savedTabName = savedStateHandle.get<String>(SELECTED_TAB_KEY)
        return ThoughtTab.entries.firstOrNull { it.name == savedTabName } ?: ThoughtTab.INBOX
    }

    private fun restoreSearchQueries(): Map<ThoughtTab, String> {
        return ThoughtTab.entries.associateWith { tab ->
            savedStateHandle.get<String>(searchQueryKey(tab)).orEmpty()
        }
    }

    private fun searchQueryKey(tab: ThoughtTab): String {
        return "search_query_${tab.name.lowercase()}"
    }

    private suspend fun syncReminder(
        thought: Thought,
        emitEvents: Boolean = true
    ): ReminderSyncResult {
        val result = reminderScheduler.syncReminder(thought)

        if (emitEvents && result.needsExactAlarmPermission) {
            _events.emit(ThoughtUiEvent.RequestExactAlarmPermission)
        }
        if (emitEvents && result.needsNotificationPermission) {
            _events.emit(ThoughtUiEvent.RequestNotificationPermission)
        }

        return result
    }

    private companion object {
        const val SELECTED_TAB_KEY = "selected_tab"
        const val SORT_RECENT_FIRST_KEY = "sort_recent_first"
    }
}
