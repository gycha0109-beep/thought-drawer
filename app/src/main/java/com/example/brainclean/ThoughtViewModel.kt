package com.example.brainclean

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.CaptureResult
import com.example.brainclean.domain.ThoughtCommandService
import com.example.brainclean.model.CaptureSource
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import com.example.brainclean.reminder.ReminderSyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repository = ThoughtRepository(
        BrainCleanDatabase.getDatabase(application).thoughtDao()
    )
    private val commandService = ThoughtCommandService(
        context = application,
        repository = repository
    )

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

    init {
        observeThoughts()
    }

    fun addThought(content: String) {
        viewModelScope.launch {
            when (
                val result = commandService.captureThought(
                    content = content,
                    source = CaptureSource.APP
                )
            ) {
                CaptureResult.Blank -> Unit
                is CaptureResult.Success -> emitReminderEvents(result.reminderSyncResult)
            }
        }
    }

    fun updateThoughtStatus(id: Long, status: ThoughtStatus) {
        viewModelScope.launch {
            val result = commandService.updateThoughtStatus(id, status)
            emitReminderEvents(result)
        }
    }

    fun updateThoughtContent(id: Long, content: String) {
        viewModelScope.launch {
            val result = commandService.updateThoughtContent(id, content)
            emitReminderEvents(result)
        }
    }

    fun updateThoughtReminder(id: Long, remindAt: Long?) {
        viewModelScope.launch {
            val result = commandService.updateThoughtReminder(id, remindAt)
            emitReminderEvents(result)
        }
    }

    fun markThoughtDone(id: Long) {
        updateThoughtStatus(id, ThoughtStatus.DONE)
    }

    fun deleteThought(id: Long) {
        viewModelScope.launch {
            commandService.deleteThought(id)
        }
    }

    fun deleteThoughts(ids: Set<Long>) {
        viewModelScope.launch {
            commandService.deleteThoughts(ids)
        }
    }

    fun restoreThought(thought: Thought) {
        viewModelScope.launch {
            val result = commandService.restoreThought(thought)
            emitReminderEvents(result)
        }
    }

    fun syncScheduledReminders() {
        viewModelScope.launch {
            commandService.syncScheduledReminders()
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
            repository.observeThoughts().collect { thoughts ->
                _uiState.value = _uiState.value.copy(thoughts = thoughts)
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

    private suspend fun emitReminderEvents(result: ReminderSyncResult) {
        if (result.needsExactAlarmPermission) {
            _events.emit(ThoughtUiEvent.RequestExactAlarmPermission)
        }
        if (result.needsNotificationPermission) {
            _events.emit(ThoughtUiEvent.RequestNotificationPermission)
        }
    }

    private companion object {
        const val SELECTED_TAB_KEY = "selected_tab"
        const val SORT_OPTION_KEY = "sort_option"
        const val SORT_RECENT_FIRST_KEY = "sort_recent_first"
    }
}
