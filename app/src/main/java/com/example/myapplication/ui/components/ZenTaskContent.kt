package com.example.myapplication.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.Collection as UiCollection
import com.example.myapplication.model.NavigationItem
import com.example.myapplication.model.Task
import com.example.myapplication.ui.screens.home.kanban.KanbanHomeScreen
import com.example.myapplication.ui.screens.home.kanban.KanbanColumn
import com.example.myapplication.ui.screens.calendar.CalendarScreen
import com.example.myapplication.ui.screens.CollectionsScreen
import com.example.myapplication.ui.screens.SettingsScreen

@Composable
fun ZenTaskContent(
    uiState: ZenTaskUiState,
    callbacks: ZenTaskCallbacks,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        // Track if content is still being rendered (during deferred load)
        val isRendering = uiState.currentScreen != uiState.renderedScreen

        // Show skeleton placeholder during transition
        if (isRendering) {
            when (uiState.currentScreen) {
                NavigationItem.MY_DAY -> SkeletonKanbanScreen(
                    modifier = Modifier.fillMaxSize()
                )
                NavigationItem.CALENDAR -> SkeletonTaskList(
                    itemCount = 5,
                    modifier = Modifier.fillMaxSize().padding(top = 100.dp)
                )
                else -> Box(Modifier.fillMaxSize()) // Empty placeholder for other screens
            }
        }

        // Sử dụng Crossfade để chuyển đổi màn hình mượt mà hơn, tránh hiện tượng giật cục khi spam tab
        Crossfade(
            targetState = uiState.renderedScreen, // Sử dụng renderedScreen (đã delay) thay vì currentScreen
            label = "ScreenTransition",
            animationSpec = tween(durationMillis = 300) // Thời gian chuyển cảnh hợp lý
        ) { targetScreen ->
            when (targetScreen) {
                NavigationItem.MY_DAY -> KanbanHomeScreen(
                    tasks = uiState.tasks,
                    collections = uiState.collectionNameMap,
                    onTaskToggle = callbacks.onTaskToggle,
                    onTaskDelete = callbacks.onTaskDelete,
                    onTaskStatusChange = callbacks.onTaskStatusChange
                )
                NavigationItem.CALENDAR -> CalendarScreen(
                    tasks = uiState.tasks,
                    collections = uiState.collections,
                    onTaskToggle = callbacks.onTaskToggle,
                    onTaskDelete = callbacks.onTaskDelete
                )
                NavigationItem.COLLECTIONS -> CollectionsScreen(
                    uiState.collections,
                    uiState.tasks,
                    callbacks.onCollectionSelected,
                    callbacks.onAddCollectionClick
                )
                NavigationItem.SETTINGS -> SettingsScreen()
            }
        }
    }
}

data class ZenTaskUiState(
    val currentScreen: NavigationItem,
    val renderedScreen: NavigationItem,
    val tasks: List<Task>,
    val collections: List<UiCollection>,
    val collectionNameMap: Map<Int, String>
)

data class ZenTaskCallbacks(
    val onTaskToggle: (Int) -> Unit,
    val onTaskDelete: (Int) -> Unit,
    val onTaskStatusChange: (Int, KanbanColumn) -> Unit,
    val onCollectionSelected: (UiCollection?) -> Unit,
    val onAddCollectionClick: () -> Unit
)
