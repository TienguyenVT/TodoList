package com.example.myapplication

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import này chứa getValue và setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.db.TodoDatabase
import com.example.myapplication.data.local.db.entity.Task as DbTask
import com.example.myapplication.data.local.db.entity.TaskGroup as DbTaskGroup
import com.example.myapplication.model.Collection as UiCollection
import com.example.myapplication.model.NavigationItem
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.components.NeumorphicBottomNav
import com.example.myapplication.ui.components.NeumorphicFAB
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.screens.home.kanban.KanbanHomeScreen
import com.example.myapplication.ui.screens.home.kanban.KanbanColumn
import com.example.myapplication.ui.screens.calendar.CalendarScreen
import com.example.myapplication.ui.sheets.AddCollectionSheet
import com.example.myapplication.ui.sheets.AddTaskSheet
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenTaskApp() {
    var currentScreen by remember { mutableStateOf(NavigationItem.MY_DAY) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddCollectionSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tag = "ZenTaskApp"
    val coroutineScope = rememberCoroutineScope()
    val db = remember { TodoDatabase.getInstance(context.applicationContext) }
    val taskDao = remember(db) { db.taskDao() }
    val taskGroupDao = remember(db) { db.taskGroupDao() }

    val dbTasks by taskDao.observeAll().collectAsState(initial = emptyList())
    val tasks by remember(dbTasks) { derivedStateOf { dbTasks.map { it.toUiTask() } } }

    val dbCollections by taskGroupDao.observeAll().collectAsState(initial = emptyList())
    val collections by remember(dbCollections) {
        derivedStateOf { dbCollections.map { it.toUiCollection() } }
    }

    // Collection name map for UI components
    val collectionNameMap by remember(collections) {
        derivedStateOf { collections.associate { it.id to it.name } }
    }

    // Chọn collection hiện tại
    var selectedCollection by remember { mutableStateOf<UiCollection?>(null) }

    // Stable current date that updates at midnight so 'todayTasks' refreshes automatically
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(currentScreen) {
        if (currentScreen != NavigationItem.COLLECTIONS) {
            showAddTaskSheet = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val waitMillis = java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000)
            delay(waitMillis)
            currentDate.value = LocalDate.now()
        }
    }

    // Memoize filtered lists; derivedStateOf will track snapshot reads (no extra keys required)
    val todayTasks by remember { derivedStateOf { tasks.filter { it.dueDate == currentDate.value } } }
    val tasksBySelectedCollection by remember { derivedStateOf {
        selectedCollection?.let { col -> tasks.filter { it.collectionId == col.id } } ?: emptyList()
    } }

    LaunchedEffect(collections) {
        val currentSelectedId = selectedCollection?.id ?: return@LaunchedEffect
        if (collections.none { it.id == currentSelectedId }) {
            selectedCollection = null
        }
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Box(Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (currentScreen) {
                    NavigationItem.MY_DAY -> KanbanHomeScreen(
                        tasks = tasks,
                        collections = collectionNameMap,
                        onTaskToggle = { id ->
                            coroutineScope.launch {
                                try {
                                    val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                                    if (current == null) {
                                        Log.w(tag, "Toggle task failed: taskId=$id not found")
                                        showToast("Không tìm thấy công việc")
                                        return@launch
                                    }

                                    withContext(Dispatchers.IO) {
                                        taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                                    }
                                } catch (t: Throwable) {
                                    Log.e(tag, "Toggle task failed: taskId=$id", t)
                                    showToast("Lỗi khi cập nhật công việc")
                                }
                            }
                        },
                        onTaskDelete = { id ->
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) { taskDao.deleteById(id) }
                                } catch (t: Throwable) {
                                    Log.e(tag, "Delete task failed: taskId=$id", t)
                                    showToast("Lỗi khi xoá công việc")
                                }
                            }
                        },
                        onTaskStatusChange = { id, column ->
                            coroutineScope.launch {
                                try {
                                    val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                                    if (current != null) {
                                        val (newStatus, newPriority) = when (column) {
                                            KanbanColumn.COMPLETED -> 1 to current.priority
                                            KanbanColumn.IN_PROGRESS -> 0 to 2 // HIGH
                                            KanbanColumn.UNCOMPLETED -> {
                                                val adjustedPriority = if (current.priority == 2) 1 else current.priority
                                                0 to adjustedPriority
                                            }
                                        }

                                        withContext(Dispatchers.IO) {
                                            taskDao.update(
                                                current.copy(
                                                    status = newStatus,
                                                    priority = newPriority
                                                )
                                            )
                                        }
                                    }
                                } catch (t: Throwable) {
                                    Log.e(tag, "Change status failed: taskId=$id", t)
                                    showToast("Lỗi khi cập nhật trạng thái")
                                }
                            }
                        }
                    )
                    NavigationItem.CALENDAR -> CalendarScreen(
                        tasks = tasks,
                        collections = collections,
                        onTaskToggle = { id ->
                            coroutineScope.launch {
                                try {
                                    val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                                    if (current == null) {
                                        Log.w(tag, "Toggle task failed: taskId=$id not found")
                                        showToast("Không tìm thấy công việc")
                                        return@launch
                                    }

                                    withContext(Dispatchers.IO) {
                                        taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                                    }
                                } catch (t: Throwable) {
                                    Log.e(tag, "Toggle task failed: taskId=$id", t)
                                    showToast("Lỗi khi cập nhật công việc")
                                }
                            }
                        },
                        onTaskDelete = { id ->
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) { taskDao.deleteById(id) }
                                } catch (t: Throwable) {
                                    Log.e(tag, "Delete task failed: taskId=$id", t)
                                    showToast("Lỗi khi xoá công việc")
                                }
                            }
                        }
                    )
                    NavigationItem.COLLECTIONS -> CollectionsScreen(
                        collections,
                        tasks,
                        { selectedCollection = it },
                        { showAddCollectionSheet = true }
                    )
                    NavigationItem.SETTINGS -> SettingsScreen()
                }
            }
            NeumorphicBottomNav(currentScreen) { currentScreen = it }
        }

        if (currentScreen == NavigationItem.COLLECTIONS) {
            NeumorphicFAB(
                onClick = { showAddTaskSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 90.dp)
            )
        }

        // Các Bottom Sheet
        if (showAddTaskSheet && currentScreen == NavigationItem.COLLECTIONS) {
            ModalBottomSheet({ showAddTaskSheet = false }, containerColor = NeumorphicColors.surface) {
                AddTaskSheet({ t, desc, d, p, c, imageUri ->
                    coroutineScope.launch {
                        try {
                            val targetCollectionId = c ?: selectedCollection?.id

                            val (validatedGroupId, wasInvalidGroup) = withContext(Dispatchers.IO) {
                                if (targetCollectionId == null) {
                                    null to false
                                } else {
                                    val exists = taskGroupDao.getById(targetCollectionId) != null
                                    if (exists) targetCollectionId to false else null to true
                                }
                            }

                            if (wasInvalidGroup) {
                                Log.w(tag, "Add task: groupId=$targetCollectionId not found; saving as uncategorized")
                                showToast("Danh mục không còn tồn tại, sẽ lưu vào Không phân loại")
                            }

                            withContext(Dispatchers.IO) {
                                taskDao.upsert(
                                    DbTask(
                                        title = t,
                                        description = desc,
                                        progressNotes = null,
                                        dueDate = d?.toEpochMillis(),
                                        urlLink = null,
                                        imagePath = imageUri,
                                        status = 0,
                                        priority = p.toDbPriority(),
                                        groupId = validatedGroupId
                                    )
                                )
                            }
                        } catch (t: Throwable) {
                            Log.e(tag, "Add task failed", t)
                            showToast("Lỗi khi thêm công việc")
                        }
                    }
                    showAddTaskSheet = false
                }, { showAddTaskSheet = false })
            }
        }

        if (showAddCollectionSheet) {
            ModalBottomSheet({ showAddCollectionSheet = false }, containerColor = NeumorphicColors.surface) {
                AddCollectionSheet({ n, c ->
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                taskGroupDao.upsert(
                                    DbTaskGroup(
                                        groupName = n,
                                        groupColor = c.toArgb(),
                                        description = null
                                    )
                                )
                            }
                            showAddCollectionSheet = false
                        } catch (t: Throwable) {
                            Log.e(tag, "Add collection failed", t)
                            showToast("Lỗi khi tạo danh mục")
                        }
                    }
                }, { showAddCollectionSheet = false })
            }
        }

        // Detail View
        selectedCollection?.let { collection ->
            CollectionDetailView(
                collection,
                tasksBySelectedCollection,
                { selectedCollection = null },
                { id ->
                    coroutineScope.launch {
                        try {
                            val current = withContext(Dispatchers.IO) { taskDao.getById(id) }
                            if (current == null) {
                                Log.w(tag, "Toggle task failed: taskId=$id not found")
                                showToast("Không tìm thấy công việc")
                                return@launch
                            }

                            withContext(Dispatchers.IO) {
                                taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                            }
                        } catch (t: Throwable) {
                            Log.e(tag, "Toggle task failed: taskId=$id", t)
                            showToast("Lỗi khi cập nhật công việc")
                        }
                    }
                },
                { id ->
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) { taskDao.deleteById(id) }
                        } catch (t: Throwable) {
                            Log.e(tag, "Delete task failed: taskId=$id", t)
                            showToast("Lỗi khi xoá công việc")
                        }
                    }
                }
            )
        }
    }
}

private fun DbTask.toUiTask(): Task {
    val zone = ZoneId.of("UTC")
    return Task(
        id = id,
        title = title,
        dueDate = dueDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() },
        priority = priority.toUiPriority(),
        isCompleted = status == 1,
        collectionId = groupId,
        description = description,
        imageUri = imagePath
    )
}

private fun Int.toUiPriority(): Priority {
    return when (this) {
        0 -> Priority.LOW
        2 -> Priority.HIGH
        else -> Priority.NORMAL
    }
}

private fun Priority.toDbPriority(): Int {
    return when (this) {
        Priority.LOW -> 0
        Priority.NORMAL -> 1
        Priority.HIGH -> 2
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}

private fun DbTaskGroup.toUiCollection(): UiCollection {
    return UiCollection(
        id = groupId,
        name = groupName,
        color = Color(groupColor),
        icon = Icons.Default.List
    )
}
