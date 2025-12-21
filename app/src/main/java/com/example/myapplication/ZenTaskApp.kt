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

    // Effect: Update Current Date at Midnight
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val waitMillis = java.time.Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000)
            delay(waitMillis)
            appState.currentDate = LocalDate.now()
        }
    }

    // Effect: Chrome Entrance
    LaunchedEffect(Unit) {
        onAppReady?.invoke()
        kotlinx.coroutines.delay(500)
        appState.chromeVisible = true
    }

    // Effect: Validate Selected Collection
    LaunchedEffect(collections) {
        val currentSelectedId = appState.selectedCollection?.id ?: return@LaunchedEffect
        if (collections.none { it.id == currentSelectedId }) {
            appState.selectedCollection = null
        }
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
                    state = androidx.compose.runtime.remember(appState.currentScreen, appState.isMenuVisible, appState.dynamicSlotItem) {
                        com.example.myapplication.ui.components.MascotNavState(
                            currentScreen = appState.currentScreen,
                            isMenuOpen = appState.isMenuVisible,
                            dynamicSlotIcon = appState.dynamicSlotItem?.let { 
                                when (it) {
                                     NavigationItem.SETTINGS -> Icons.Filled.Settings
                                     else -> null
                                }
                            }
                        )
                    },
                    actions = androidx.compose.runtime.remember(appState) {
                        com.example.myapplication.ui.components.MascotNavActions(
                            onMenuClick = { appState.isMenuVisible = !appState.isMenuVisible },
                            onDynamicSlotClick = { 
                                if (appState.dynamicSlotItem != null) {
                                    appState.currentScreen = appState.dynamicSlotItem!!
                                } else {
                                    appState.isMenuVisible = true
                                }
                            },
                            onNavigate = appState::navigateTo
                        )
                    }
                )
            }
        }

        // Floating Command Deck
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingCommandDeck(
                isVisible = appState.isMenuVisible && appState.chromeVisible,
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

        // FAB
        if (appState.currentScreen == NavigationItem.COLLECTIONS && !appState.isMenuVisible) {
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

        // Sheets
        if (appState.showAddTaskSheet && appState.currentScreen == NavigationItem.COLLECTIONS) {
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

        // Detail View
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
}


