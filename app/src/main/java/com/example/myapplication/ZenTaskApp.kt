package com.example.myapplication

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import kotlinx.coroutines.delay
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.Collection as UiCollection
import com.example.myapplication.model.NavigationItem
import com.example.myapplication.ui.components.MascotBottomNav
import com.example.myapplication.ui.components.NeumorphicFAB
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.components.FloatingCommandDeck
import com.example.myapplication.ui.components.ZenTaskContent
import com.example.myapplication.ui.sheets.AddCollectionSheet
import com.example.myapplication.ui.sheets.AddTaskSheet
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.LocalDateTime
import com.example.myapplication.utils.PerfLogger
import com.example.myapplication.model.Task
import com.example.myapplication.model.Priority
import com.example.myapplication.model.toUiTask
import com.example.myapplication.model.toUiCollection
import com.example.myapplication.ui.state.rememberZenTaskAppState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenTaskApp(onAppReady: (() -> Unit)? = null) {
    val appState = rememberZenTaskAppState()
    
    // Derived State from Flow
    val tasks by appState.tasksFlow.collectAsState(initial = emptyList<Task>())
    val collections by appState.collectionsFlow.collectAsState(initial = emptyList<UiCollection>())
    
    val collectionNameMap by remember(collections) {
        derivedStateOf { collections.associate { it.id to it.name } }
    }

    val tasksBySelectedCollection by remember(appState.selectedCollection, tasks) {
        derivedStateOf {
            appState.selectedCollection?.let { col -> 
                tasks.filter { it.collectionId == col.id } 
            } ?: emptyList()
        }
    }

    // Effect: Screen Change Handling
    LaunchedEffect(appState.currentScreen) {
        appState.onScreenChange(appState.currentScreen)
    }

    // Effect: Chrome Entrance
    LaunchedEffect(Unit) {
        onAppReady?.invoke()
        kotlinx.coroutines.delay(500)
        appState.chromeVisible = true
    }

    // Effect: Validate Selected Collection
    LaunchedEffect(collections) {
        appState.validateSelectedCollection(collections)
    }

    // Render UI
    Box(Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        Column(Modifier.fillMaxSize()) {
            ZenTaskContent(
                uiState = com.example.myapplication.ui.components.ZenTaskUiState(
                    currentScreen = appState.currentScreen,
                    renderedScreen = appState.renderedScreen,
                    tasks = tasks,
                    collections = collections,
                    collectionNameMap = collectionNameMap
                ),
                callbacks = com.example.myapplication.ui.components.ZenTaskCallbacks(
                    onTaskToggle = appState::toggleTask,
                    onTaskDelete = appState::deleteTask,
                    onTaskStatusChange = appState::changeTaskStatus,
                    onCollectionSelected = { appState.selectedCollection = it },
                    onAddCollectionClick = { appState.showAddCollectionSheet = true }
                ),
                modifier = Modifier.weight(1f)
            )

            ZenTaskBottomNav(appState)
        }

        ZenTaskCommandDeck(appState)
        ZenTaskFab(appState)
        ZenTaskSheets(appState)
        ZenTaskDetailOverlay(appState, tasksBySelectedCollection)
    }
}

@Composable
private fun ZenTaskBottomNav(appState: com.example.myapplication.ui.state.ZenTaskAppState) {
    AnimatedVisibility(
        visible = appState.chromeVisible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    ) {
        MascotBottomNav(
            state = com.example.myapplication.ui.components.MascotNavState(
                currentScreen = appState.currentScreen,
                isMenuOpen = appState.isMenuVisible,
                dynamicSlotIcon = appState.getDynamicSlotIcon()
            ),
            actions = com.example.myapplication.ui.components.MascotNavActions(
                onMenuClick = { appState.isMenuVisible = !appState.isMenuVisible },
                onDynamicSlotClick = appState::handleDynamicSlotClick,
                onNavigate = appState::navigateTo
            )
        )
    }
}

@Composable
private fun ZenTaskCommandDeck(appState: com.example.myapplication.ui.state.ZenTaskAppState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 90.dp, end = 24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingCommandDeck(
            isVisible = appState.showCommandDeck,
            onDismiss = { appState.isMenuVisible = false },
            onSettingsClick = {
                appState.onMenuAction {
                    appState.dynamicSlotItem = NavigationItem.SETTINGS
                    appState.currentScreen = NavigationItem.SETTINGS
                }
            },
            onLogoutClick = {
                appState.onMenuAction {
                    appState.showToast("Logout Clicked")
                }
            }
        )
    }
}

@Composable
private fun BoxScope.ZenTaskFab(appState: com.example.myapplication.ui.state.ZenTaskAppState) {
    if (appState.showFab) {
        AnimatedVisibility(
            visible = appState.chromeVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 2 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        ) {
            NeumorphicFAB(
                onClick = { appState.showAddTaskSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 90.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZenTaskSheets(appState: com.example.myapplication.ui.state.ZenTaskAppState) {
    if (appState.showAddTaskCondition) {
        ModalBottomSheet({ appState.showAddTaskSheet = false }, containerColor = NeumorphicColors.surface) {
            AddTaskSheet(
                onAddTask = { t, desc, d, p, c, img ->
                    appState.addTask(com.example.myapplication.ui.state.AddTaskRequest(t, desc, d, p, c, img))
                    appState.showAddTaskSheet = false
                },
                onDismiss = { appState.showAddTaskSheet = false }
            )
        }
    }

    if (appState.showAddCollectionSheet) {
        ModalBottomSheet({ appState.showAddCollectionSheet = false }, containerColor = NeumorphicColors.surface) {
            AddCollectionSheet(
                onAddCollection = { n, c ->
                    appState.addCollection(n, c)
                    appState.showAddCollectionSheet = false
                },
                onDismiss = { appState.showAddCollectionSheet = false }
            )
        }
    }
}

@Composable
private fun ZenTaskDetailOverlay(
    appState: com.example.myapplication.ui.state.ZenTaskAppState,
    tasksBySelectedCollection: List<Task>
) {
    appState.selectedCollection?.let { collection ->
        CollectionDetailView(
            collection,
            tasksBySelectedCollection,
            { appState.selectedCollection = null },
            appState::toggleTask,
            appState::deleteTask
        )
    }
}


