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
    currentScreen: NavigationItem,
    renderedScreen: NavigationItem,
    tasks: List<Task>,
    collections: List<UiCollection>,
    collectionNameMap: Map<Int, String>,
    selectedCollection: UiCollection?,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit,
    onTaskStatusChange: (Int, KanbanColumn) -> Unit,
    onCollectionSelected: (UiCollection?) -> Unit,
    onAddCollectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        // Track if content is still being rendered (during deferred load)
        val isRendering = currentScreen != renderedScreen

        // Show skeleton placeholder during transition
        if (isRendering) {
            when (currentScreen) {
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
            targetState = renderedScreen, // Sử dụng renderedScreen (đã delay) thay vì currentScreen
            label = "ScreenTransition",
            animationSpec = tween(durationMillis = 300) // Thời gian chuyển cảnh hợp lý
        ) { targetScreen ->
            when (targetScreen) {
                NavigationItem.MY_DAY -> KanbanHomeScreen(
                    tasks = tasks,
                    collections = collectionNameMap,
                    onTaskToggle = onTaskToggle,
                    onTaskDelete = onTaskDelete,
                    onTaskStatusChange = onTaskStatusChange
                )
                NavigationItem.CALENDAR -> CalendarScreen(
                    tasks = tasks,
                    collections = collections,
                    onTaskToggle = onTaskToggle,
                    onTaskDelete = onTaskDelete
                )
                NavigationItem.COLLECTIONS -> CollectionsScreen(
                    collections,
                    tasks,
                    onCollectionSelected,
                    onAddCollectionClick
                )
                NavigationItem.SETTINGS -> SettingsScreen()
            }
        }
    }
}
