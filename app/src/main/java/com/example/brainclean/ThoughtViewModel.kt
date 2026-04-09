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

enum class ThoughtSortOption {
    RECENT,
    OLDEST,
    REMINDER
}

private fun defaultSearchQueries(): Map<ThoughtTab, String> {
    return ThoughtTab.entries.associateWith { "" }
}

data class ThoughtUiState(
    val thoughts: List<Thought> = emptyList(),
    val selectedTab: ThoughtTab = ThoughtTab.INBOX,
    val searchQueries: Map<ThoughtTab, String> = defaultSearchQueries(),
    val sortOption: ThoughtSortOption = ThoughtSortOption.RECENT
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
            sortOption = restoreSortOption()
        )
    )
    val uiState: StateFlow<ThoughtUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ThoughtUiEvent>()
    val events: SharedFlow<ThoughtUiEvent> = _events.asSharedFlow()

    private var hasSeededDatabase = false
    private val seedBaseTime = System.currentTimeMillis()

    private val defaultThoughts = listOf(
        Thought(
            id = seedBaseTime - 2_000L,
            content = "Write down the idea for tomorrow's workout",
            status = ThoughtStatus.INBOX,
            remindAt = null,
            createdAt = seedBaseTime - 2_000L,
            inboxEnteredAt = seedBaseTime - 2_000L
        ),
        Thought(
            id = seedBaseTime - 1_000L,
            content = "Break the project into the first small step",
            status = ThoughtStatus.INBOX,
            remindAt = null,
            createdAt = seedBaseTime - 1_000L,
            inboxEnteredAt = seedBaseTime - 1_000L
        )
    )

    init {
        observeThoughts()
    }

    fun addThought(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return

        viewModelScope.launch {
            val createdAt = System.currentTimeMillis()
            val newThought = Thought(
                id = createdAt,
                content = trimmedContent,
                status = ThoughtStatus.INBOX,
                remindAt = null,
                createdAt = createdAt,
                inboxEnteredAt = createdAt
            )

            thoughtDao.insertThought(ThoughtEntity.fromThought(newThought))
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun updateThoughtStatus(id: Long, status: ThoughtStatus) {
        val existingThought = uiState.value.thoughts.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedThought = if (status == ThoughtStatus.DONE) {
                existingThought.copy(
                    status = status,
                    remindAt = null,
                    completedAt = now
                )
            } else if (status == ThoughtStatus.INBOX && existingThought.status != ThoughtStatus.INBOX) {
                existingThought.copy(
                    status = status,
                    completedAt = null,
                    inboxEnteredAt = now,
                    staleInboxReminderSentAt = null
                )
            } else {
                existingThought.copy(
                    status = status,
                    completedAt = null
                )
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
            val updatedThought = existingThought.copy(
                content = trimmedContent,
                inboxEnteredAt = if (existingThought.status == ThoughtStatus.INBOX) {
                    System.currentTimeMillis()
                } else {
                    existingThought.inboxEnteredAt
                },
                staleInboxReminderSentAt = if (existingThought.status == ThoughtStatus.INBOX) {
                    null
                } else {
                    existingThought.staleInboxReminderSentAt
                }
            )
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
            reminderScheduler.cancelStaleInboxReminder(existingThought.id)
            BrainCleanHomeWidget.refreshAll(getApplication())
        }
    }

    fun deleteThoughts(ids: Set<Long>) {
        if (ids.isEmpty()) return

        val thoughtsToDelete = uiState.value.thoughts.filter { it.id in ids }
        if (thoughtsToDelete.isEmpty()) return

        viewModelScope.launch {
            thoughtsToDelete.forEach { thought ->
                thoughtDao.deleteThought(ThoughtEntity.fromThought(thought))
                reminderScheduler.cancelReminder(thought.id)
                reminderScheduler.cancelStaleInboxReminder(thought.id)
            }
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

    fun setSortOption(sortOption: ThoughtSortOption) {
        savedStateHandle[SORT_OPTION_KEY] = sortOption.name
        _uiState.value = _uiState.value.copy(sortOption = sortOption)
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

    private fun restoreSortOption(): ThoughtSortOption {
        val savedSortOption = savedStateHandle.get<String>(SORT_OPTION_KEY)
        if (savedSortOption != null) {
            return ThoughtSortOption.entries.firstOrNull { it.name == savedSortOption }
                ?: ThoughtSortOption.RECENT
        }

        val legacySortRecentFirst = savedStateHandle.get<Boolean>(SORT_RECENT_FIRST_KEY)
        return if (legacySortRecentFirst == false) {
            ThoughtSortOption.OLDEST
        } else {
            ThoughtSortOption.RECENT
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
        const val SORT_OPTION_KEY = "sort_option"
        const val SORT_RECENT_FIRST_KEY = "sort_recent_first"
    }
}
