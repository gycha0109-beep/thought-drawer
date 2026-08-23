package com.example.brainclean

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.brainclean.capture.CaptureNotificationManager
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import com.example.brainclean.reminder.ThoughtReminderReceiver
import com.example.brainclean.ui.screen.DoneScreen
import com.example.brainclean.ui.screen.InboxScreen
import com.example.brainclean.ui.screen.LaterScreen
import com.example.brainclean.ui.screen.TodayScreen
import com.example.brainclean.ui.theme.BrainCleanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var thoughtViewModel: ThoughtViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thoughtViewModel = ViewModelProvider(this)[ThoughtViewModel::class.java]
        ThoughtReminderReceiver.createNotificationChannel(this)

        setContent {
            BrainCleanTheme {
                BrainCleanApp(thoughtViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::thoughtViewModel.isInitialized) {
            thoughtViewModel.syncScheduledReminders()
            CaptureNotificationManager.ensureVisible(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrainCleanApp(thoughtViewModel: ThoughtViewModel) {
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    var hasNotificationRuntimePermission by remember {
        mutableStateOf(CaptureNotificationManager.hasRuntimePermission(context))
    }
    val uiState by thoughtViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val currentTab = uiState.selectedTab
    var selectedThoughtIds by remember(currentTab) { mutableStateOf(emptySet<Long>()) }
    val searchQuery = uiState.searchQueries[currentTab].orEmpty()
    val sortOption = uiState.sortOption
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationRuntimePermission = granted ||
            CaptureNotificationManager.hasRuntimePermission(context)
        if (hasNotificationRuntimePermission) {
            CaptureNotificationManager.ensureVisible(context)
        }
    }

    LaunchedEffect(thoughtViewModel) {
        thoughtViewModel.events.collect { event ->
            when (event) {
                ThoughtUiEvent.RequestExactAlarmPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }

                ThoughtUiEvent.RequestNotificationPermission -> {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !CaptureNotificationManager.hasRuntimePermission(context)
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        CaptureNotificationManager.ensureVisible(context)
                    }
                }
            }
        }
    }

    fun applySearchAndSort(thoughts: List<Thought>): List<Thought> {
        val filteredThoughts = if (searchQuery.isBlank()) {
            thoughts
        } else {
            thoughts.filter { thought ->
                thought.content.contains(searchQuery.trim(), ignoreCase = true)
            }
        }

        return when (sortOption) {
            ThoughtSortOption.RECENT -> filteredThoughts.sortedByDescending { it.createdAt }
            ThoughtSortOption.OLDEST -> filteredThoughts.sortedBy { it.createdAt }
            ThoughtSortOption.REMINDER -> filteredThoughts.sortedWith(
                compareBy<Thought>(
                    { it.remindAt == null },
                    { it.remindAt ?: Long.MAX_VALUE },
                    { -it.createdAt }
                )
            )
        }
    }

    val inboxThoughts = applySearchAndSort(
        uiState.thoughts.filter { it.status == ThoughtStatus.INBOX }
    )
    val todayThoughts = applySearchAndSort(
        uiState.thoughts.filter { it.status == ThoughtStatus.TODAY }
    )
    val laterThoughts = applySearchAndSort(
        uiState.thoughts.filter { it.status == ThoughtStatus.LATER }
    )
    val doneThoughts = applySearchAndSort(
        uiState.thoughts.filter { it.status == ThoughtStatus.DONE }
    )

    fun showUndoSnackbar(message: String, originalThought: Thought) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                thoughtViewModel.restoreThought(originalThought)
            }
        }
    }

    fun handleDone(thought: Thought) {
        thoughtViewModel.markThoughtDone(thought.id)
        showUndoSnackbar("Thought marked as done", thought)
    }

    fun handleDelete(thought: Thought) {
        thoughtViewModel.deleteThought(thought.id)
        showUndoSnackbar("Thought deleted", thought)
    }

    fun toggleSelection(thoughtId: Long) {
        selectedThoughtIds = if (thoughtId in selectedThoughtIds) {
            selectedThoughtIds - thoughtId
        } else {
            selectedThoughtIds + thoughtId
        }
    }

    fun startSelection(thoughtId: Long) {
        selectedThoughtIds = selectedThoughtIds + thoughtId
    }

    fun clearSelection() {
        selectedThoughtIds = emptySet()
    }

    fun deleteSelectedThoughts() {
        thoughtViewModel.deleteThoughts(selectedThoughtIds)
        clearSelection()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (selectedThoughtIds.isNotEmpty()) {
                                "${selectedThoughtIds.size} selected"
                            } else {
                                when (currentTab) {
                                    ThoughtTab.INBOX -> "Inbox"
                                    ThoughtTab.TODAY -> "Today"
                                    ThoughtTab.LATER -> "Later"
                                    ThoughtTab.DONE -> "Done"
                                }
                            }
                        )
                    },
                    actions = {
                        if (selectedThoughtIds.isNotEmpty()) {
                            TextButton(onClick = ::deleteSelectedThoughts) {
                                Text("Delete")
                            }
                            TextButton(onClick = ::clearSelection) {
                                Text("Cancel")
                            }
                        } else if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !hasNotificationRuntimePermission
                        ) {
                            TextButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.enable_capture_notification_action))
                            }
                        }
                    }
                )

                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_bar"),
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.testTag("search_input"),
                            query = searchQuery,
                            onQueryChange = {
                                thoughtViewModel.updateSearchQuery(currentTab, it)
                            },
                            onSearch = { isSearchExpanded = false },
                            expanded = isSearchExpanded,
                            onExpandedChange = { isSearchExpanded = it },
                            placeholder = { Text("Search in current tab") }
                        )
                    },
                    expanded = isSearchExpanded,
                    onExpandedChange = { isSearchExpanded = it }
                ) {}

                if (selectedThoughtIds.isEmpty()) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sortOption == ThoughtSortOption.RECENT,
                            onClick = { thoughtViewModel.setSortOption(ThoughtSortOption.RECENT) },
                            modifier = Modifier.testTag("sort_recent"),
                            label = { Text("Recent") }
                        )
                        FilterChip(
                            selected = sortOption == ThoughtSortOption.OLDEST,
                            onClick = { thoughtViewModel.setSortOption(ThoughtSortOption.OLDEST) },
                            modifier = Modifier.testTag("sort_oldest"),
                            label = { Text("Oldest") }
                        )
                        FilterChip(
                            selected = sortOption == ThoughtSortOption.REMINDER,
                            onClick = { thoughtViewModel.setSortOption(ThoughtSortOption.REMINDER) },
                            modifier = Modifier.testTag("sort_reminder"),
                            label = { Text("Reminder") }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == ThoughtTab.INBOX,
                    onClick = { thoughtViewModel.updateSelectedTab(ThoughtTab.INBOX) },
                    modifier = Modifier.testTag("tab_inbox"),
                    icon = {},
                    label = { Text("Inbox") }
                )
                NavigationBarItem(
                    selected = currentTab == ThoughtTab.TODAY,
                    onClick = { thoughtViewModel.updateSelectedTab(ThoughtTab.TODAY) },
                    modifier = Modifier.testTag("tab_today"),
                    icon = {},
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = currentTab == ThoughtTab.LATER,
                    onClick = { thoughtViewModel.updateSelectedTab(ThoughtTab.LATER) },
                    modifier = Modifier.testTag("tab_later"),
                    icon = {},
                    label = { Text("Later") }
                )
                NavigationBarItem(
                    selected = currentTab == ThoughtTab.DONE,
                    onClick = { thoughtViewModel.updateSelectedTab(ThoughtTab.DONE) },
                    modifier = Modifier.testTag("tab_done"),
                    icon = {},
                    label = { Text("Done") }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ThoughtTab.INBOX -> InboxScreen(
                    thoughts = inboxThoughts,
                    selectedThoughtIds = selectedThoughtIds,
                    onAddThought = thoughtViewModel::addThought,
                    onToggleSelection = ::toggleSelection,
                    onStartSelection = ::startSelection,
                    onMoveToToday = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.TODAY) },
                    onMoveToLater = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.LATER) },
                    onMarkDone = ::handleDone,
                    onDeleteThought = ::handleDelete,
                    onEditThought = thoughtViewModel::updateThoughtContent,
                    onSetReminder = thoughtViewModel::updateThoughtReminder,
                    onClearReminder = { thoughtViewModel.updateThoughtReminder(it, null) }
                )

                ThoughtTab.TODAY -> TodayScreen(
                    thoughts = todayThoughts,
                    selectedThoughtIds = selectedThoughtIds,
                    onToggleSelection = ::toggleSelection,
                    onStartSelection = ::startSelection,
                    onMoveToInbox = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.INBOX) },
                    onMoveToLater = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.LATER) },
                    onMarkDone = ::handleDone,
                    onDeleteThought = ::handleDelete,
                    onEditThought = thoughtViewModel::updateThoughtContent,
                    onSetReminder = thoughtViewModel::updateThoughtReminder,
                    onClearReminder = { thoughtViewModel.updateThoughtReminder(it, null) }
                )

                ThoughtTab.LATER -> LaterScreen(
                    thoughts = laterThoughts,
                    selectedThoughtIds = selectedThoughtIds,
                    onToggleSelection = ::toggleSelection,
                    onStartSelection = ::startSelection,
                    onMoveToInbox = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.INBOX) },
                    onMoveToToday = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.TODAY) },
                    onMarkDone = ::handleDone,
                    onDeleteThought = ::handleDelete,
                    onEditThought = thoughtViewModel::updateThoughtContent,
                    onSetReminder = thoughtViewModel::updateThoughtReminder,
                    onClearReminder = { thoughtViewModel.updateThoughtReminder(it, null) }
                )

                ThoughtTab.DONE -> DoneScreen(
                    thoughts = doneThoughts,
                    selectedThoughtIds = selectedThoughtIds,
                    onToggleSelection = ::toggleSelection,
                    onStartSelection = ::startSelection,
                    onMoveToInbox = { thoughtViewModel.updateThoughtStatus(it, ThoughtStatus.INBOX) },
                    onDeleteThought = ::handleDelete,
                    onEditThought = thoughtViewModel::updateThoughtContent
                )
            }
        }
    }
}
