package com.example.myapplication.ui.state

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import com.example.myapplication.data.local.db.TodoDatabase
import com.example.myapplication.model.*
import com.example.myapplication.model.Collection as UiCollection
import com.example.myapplication.utils.PerfLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings

private const val TAG = "ZenTaskApp"

@Stable
class ZenTaskAppState(
    val context: Context,
    val coroutineScope: CoroutineScope,
    private val db: TodoDatabase
) {
    // Navigation State
    var currentScreen by mutableStateOf(NavigationItem.MY_DAY)
    var renderedScreen by mutableStateOf(NavigationItem.MY_DAY)
        private set
    
    var isMenuVisible by mutableStateOf(false)
    var chromeVisible by mutableStateOf(false)
    var dynamicSlotItem by mutableStateOf<NavigationItem?>(null)

    // Sheet State
    var showAddTaskSheet by mutableStateOf(false)
    var showAddCollectionSheet by mutableStateOf(false)

    // Data Selection State
    var selectedCollection by mutableStateOf<UiCollection?>(null)
    var currentDate by mutableStateOf(LocalDate.now())

    // DAOs
    internal val taskDao = db.taskDao()
    internal val taskGroupDao = db.taskGroupDao()

    // Data Flows
    val tasksFlow: Flow<List<Task>> = taskDao.observeAll().map { list -> list.map { it.toUiTask() } }
    val collectionsFlow: Flow<List<UiCollection>> = taskGroupDao.observeAll().map { list -> list.map { it.toUiCollection() } }

    // Helper State for non-completed task tracking
    internal val lastNonCompletedStatus = mutableStateMapOf<Int, Int>()

    // Computed properties to reduce cognitive complexity in composables
    val showFab: Boolean get() = currentScreen == NavigationItem.COLLECTIONS && !isMenuVisible
    val showCommandDeck: Boolean get() = isMenuVisible && chromeVisible
    val showAddTaskCondition: Boolean get() = showAddTaskSheet && currentScreen == NavigationItem.COLLECTIONS

    // Initialize background jobs
    init {
        startMidnightUpdateJob()
    }

    private fun startMidnightUpdateJob() {
        coroutineScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val waitMillis = java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000)
                delay(waitMillis)
                currentDate = LocalDate.now()
            }
        }
    }

    fun validateSelectedCollection(collections: List<UiCollection>) {
        val currentSelectedId = selectedCollection?.id ?: return
        if (collections.none { it.id == currentSelectedId }) {
            selectedCollection = null
        }
    }

    fun getDynamicSlotIcon(): androidx.compose.ui.graphics.vector.ImageVector? {
        return when (dynamicSlotItem) {
            NavigationItem.SETTINGS -> Icons.Filled.Settings
            else -> null
        }
    }

    fun handleDynamicSlotClick() {
        dynamicSlotItem?.let { currentScreen = it } ?: run { isMenuVisible = true }
    }

    fun navigateTo(item: NavigationItem) {
        PerfLogger.logAction(
            file = "ZenTaskAppState.kt",
            function = "navigateTo",
            action = "Navigate to ${item.name}"
        )
        isMenuVisible = false
        currentScreen = item
    }
    private var screenChangeJob: kotlinx.coroutines.Job? = null
    fun onScreenChange(item: NavigationItem) {
        if (item != NavigationItem.COLLECTIONS) {
            showAddTaskSheet = false
        }
        Log.d(TAG, "Screen changed to: $item. Waiting for debounce...")

        screenChangeJob?.cancel()
        screenChangeJob = coroutineScope.launch {
            // Deferred Rendering logic
            delay(350)
            renderedScreen = item
            Log.d(TAG, "Rendered screen updated to: $renderedScreen")
        }
    }

    fun onMenuAction(action: () -> Unit) {
        action()
        isMenuVisible = false
    }
}

data class AddTaskRequest(
    val title: String,
    val description: String?,
    val dueDate: LocalDate?,
    val priority: Priority,
    val collectionId: Int?,
    val imageUri: String?
)

@Composable
fun rememberZenTaskAppState(
    context: Context = androidx.compose.ui.platform.LocalContext.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ZenTaskAppState {
    val db = remember { TodoDatabase.getInstance(context.applicationContext) }
    return remember(context, coroutineScope, db) {
        ZenTaskAppState(context, coroutineScope, db)
    }
}
