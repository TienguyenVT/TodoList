package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import này chứa getValue và setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.db.TodoDatabase
import com.example.myapplication.data.local.db.entity.Task as DbTask
import com.example.myapplication.model.*
import com.example.myapplication.ui.components.NeumorphicBottomNav
import com.example.myapplication.ui.components.NeumorphicFAB
import com.example.myapplication.ui.screens.*
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
    val coroutineScope = rememberCoroutineScope()
    val db = remember { TodoDatabase.getInstance(context.applicationContext) }
    val taskDao = remember(db) { db.taskDao() }

    val dbTasks by taskDao.observeAll().collectAsState(initial = emptyList())
    val tasks by remember(dbTasks) { derivedStateOf { dbTasks.map { it.toUiTask() } } }

    // Chọn collection hiện tại
    var selectedCollection by remember { mutableStateOf<com.example.myapplication.model.Collection?>(null) }

    var collections by remember { mutableStateOf(emptyList<com.example.myapplication.model.Collection>()) }

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

    Box(Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (currentScreen) {
                    NavigationItem.MY_DAY -> MyDayScreen(
                        todayTasks,
                        { id ->
                            coroutineScope.launch {
                                val current = taskDao.getById(id) ?: return@launch
                                taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                            }
                        },
                        { id ->
                            coroutineScope.launch { taskDao.deleteById(id) }
                        }
                    )
                    NavigationItem.CALENDAR -> CalendarScreen(
                        tasks,
                        { id ->
                            coroutineScope.launch {
                                val current = taskDao.getById(id) ?: return@launch
                                taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                            }
                        },
                        { id ->
                            coroutineScope.launch { taskDao.deleteById(id) }
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
                        val targetCollectionId = selectedCollection?.id
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
                                groupId = targetCollectionId
                            )
                        )
                    }
                    showAddTaskSheet = false
                }, { showAddTaskSheet = false })
            }
        }

        if (showAddCollectionSheet) {
            ModalBottomSheet({ showAddCollectionSheet = false }, containerColor = NeumorphicColors.surface) {
                AddCollectionSheet({ n, c ->
                    collections = collections + com.example.myapplication.model.Collection((collections.maxOfOrNull { it.id } ?: 0) + 1, n, c, Icons.Default.List)
                    showAddCollectionSheet = false
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
                        val current = taskDao.getById(id) ?: return@launch
                        taskDao.update(current.copy(status = if (current.status == 1) 0 else 1))
                    }
                },
                { id ->
                    coroutineScope.launch { taskDao.deleteById(id) }
                }
            )
        }
    }
}

private fun DbTask.toUiTask(): Task {
    val zone = ZoneId.systemDefault()
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
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}