package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import này chứa getValue và setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.*
import com.example.myapplication.ui.components.NeumorphicBottomNav
import com.example.myapplication.ui.components.NeumorphicFAB
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.sheets.AddCollectionSheet
import com.example.myapplication.ui.sheets.AddTaskSheet
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenTaskApp() {
    var currentScreen by remember { mutableStateOf(NavigationItem.MY_DAY) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showAddCollectionSheet by remember { mutableStateOf(false) }

    // Chọn collection hiện tại
    var selectedCollection by remember { mutableStateOf<com.example.myapplication.model.Collection?>(null) }

    // Start with empty dataset so user can rebuild from scratch
    var tasks by remember { mutableStateOf(emptyList<Task>()) }

    var collections by remember { mutableStateOf(emptyList<com.example.myapplication.model.Collection>()) }

    // Stable current date that updates at midnight so 'todayTasks' refreshes automatically
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

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
                        { id -> tasks = tasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it } },
                        { id -> tasks = tasks.filter { it.id != id } }
                    )
                    NavigationItem.CALENDAR -> CalendarScreen(
                        tasks,
                        { id -> tasks = tasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it } },
                        { id -> tasks = tasks.filter { it.id != id } }
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

        NeumorphicFAB(
            onClick = { showAddTaskSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 90.dp)
        )

        // Các Bottom Sheet
        if (showAddTaskSheet) {
            ModalBottomSheet({ showAddTaskSheet = false }, containerColor = NeumorphicColors.surface) {
                AddTaskSheet({ t, d, p, c ->
                    tasks = tasks + Task((tasks.maxOfOrNull { it.id } ?: 0) + 1, t, d, p, false, c)
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
                { id -> tasks = tasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it } },
                { id -> tasks = tasks.filter { it.id != id } }
            )
        }
    }
}