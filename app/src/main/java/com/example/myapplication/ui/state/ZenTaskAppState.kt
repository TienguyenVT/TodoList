package com.example.myapplication.ui.state

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import com.example.myapplication.data.local.db.TodoDatabase
import com.example.myapplication.data.local.db.entity.Task as DbTask
import com.example.myapplication.data.local.db.entity.TaskGroup as DbTaskGroup
import com.example.myapplication.model.*
import com.example.myapplication.model.Collection as UiCollection
import com.example.myapplication.ui.screens.home.kanban.KanbanColumn
import com.example.myapplication.utils.PerfLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import android.database.SQLException
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
    private val taskDao = db.taskDao()
    private val taskGroupDao = db.taskGroupDao()

    // Data Flows
    val tasksFlow: Flow<List<Task>> = taskDao.observeAll().map { list -> list.map { it.toUiTask() } }
    val collectionsFlow: Flow<List<UiCollection>> = taskGroupDao.observeAll().map { list -> list.map { it.toUiCollection() } }

    // Helper State for non-completed task tracking
    private val lastNonCompletedStatus = mutableStateMapOf<Int, Int>()

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

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun addTask(request: AddTaskRequest) {
        coroutineScope.launch {
            try {
                val targetCollectionId = request.collectionId ?: selectedCollection?.id
                val (validatedGroupId, wasInvalidGroup) = withContext(Dispatchers.IO) {
                    if (targetCollectionId == null) {
                        null to false
                    } else {
                        val exists = taskGroupDao.getById(targetCollectionId) != null
                        if (exists) targetCollectionId to false else null to true
                    }
                }

                if (wasInvalidGroup) {
                    Log.w(TAG, "Add task: groupId=$targetCollectionId not found; saving as uncategorized")
                    showToast("Danh mục không còn tồn tại, sẽ lưu vào Không phân loại")
                }

                withContext(Dispatchers.IO) {
                    taskDao.upsert(
                        DbTask(
                            title = request.title,
                            description = request.description,
                            progressNotes = null,
                            dueDate = request.dueDate?.toEpochMillis(),
                            urlLink = null,
                            imagePath = request.imageUri,
                            status = 0,
                            priority = request.priority.toDbPriority(),
                            groupId = validatedGroupId
                        )
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SQLException) {
                Log.e(TAG, "Add task failed", e)
                showToast("Lỗi khi thêm công việc")
            }
        }
    }

    fun addCollection(name: String, color: androidx.compose.ui.graphics.Color) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    taskGroupDao.upsert(
                        DbTaskGroup(
                            groupName = name,
                            groupColor = color.toArgb(),
                            description = null
                        )
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SQLException) {
                Log.e(TAG, "Add collection failed", e)
                showToast("Lỗi khi tạo danh mục")
            }
        }
    }

    fun toggleTask(id: Int) {
        coroutineScope.launch {
            try {
                val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                if (current == null) {
                    Log.w(TAG, "Toggle task failed: taskId=$id not found")
                    showToast("Không tìm thấy công việc")
                } else {
                    val newStatus = if (current.status == 1) {
                        lastNonCompletedStatus.remove(id) ?: 0
                    } else {
                        lastNonCompletedStatus[id] = current.status
                        1
                    }

                    withContext(Dispatchers.IO) {
                        taskDao.update(current.copy(status = newStatus))
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SQLException) {
                Log.e(TAG, "Toggle task failed: taskId=$id", e)
                showToast("Lỗi khi cập nhật công việc")
            }
        }
    }

    fun deleteTask(id: Int) {
        coroutineScope.launch {
            try {
                lastNonCompletedStatus.remove(id)
                withContext(Dispatchers.IO) { taskDao.deleteById(id) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SQLException) {
                Log.e(TAG, "Delete task failed: taskId=$id", e)
                showToast("Lỗi khi xoá công việc")
            }
        }
    }

    fun changeTaskStatus(id: Int, column: KanbanColumn) {
        coroutineScope.launch {
            try {
                val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                if (current != null) {
                    val newStatus = when (column) {
                        KanbanColumn.COMPLETED -> 1
                        KanbanColumn.IN_PROGRESS -> 2
                        KanbanColumn.UNCOMPLETED -> 0
                    }

                    withContext(Dispatchers.IO) {
                        taskDao.update(current.copy(status = newStatus))
                    }

                    if (newStatus == 1 && current.status != 1) {
                        lastNonCompletedStatus[id] = current.status
                    } else if (newStatus != 1) {
                        lastNonCompletedStatus[id] = newStatus
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SQLException) {
                Log.e(TAG, "Change status failed: taskId=$id", e)
                showToast("Lỗi khi cập nhật trạng thái")
            }
        }
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
