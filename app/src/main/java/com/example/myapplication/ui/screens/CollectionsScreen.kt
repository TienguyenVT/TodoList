package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Collection
import com.example.myapplication.model.Task
import com.example.myapplication.ui.components.NeumorphicCard
import com.example.myapplication.ui.components.TaskCard
import com.example.myapplication.ui.theme.NeumorphicColors
import com.example.myapplication.utils.PerfLogger

@Composable
fun CollectionsScreen(collections: List<Collection>, tasks: List<Task>, onCollectionClick: (Collection) -> Unit, onAddCollection: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Danh mục", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary)
            IconButton(onClick = onAddCollection) { Icon(Icons.Default.Add, "Thêm", tint = NeumorphicColors.textPrimary) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(collections, key = { it.id }) { collection ->
                val count = tasks.filter { it.collectionId == collection.id }.size
                val completed = tasks.filter { it.collectionId == collection.id && it.isCompleted }.size
                CollectionCard(collection, count, completed) { onCollectionClick(collection) }
            }
        }
        
        // Performance logging
        androidx.compose.runtime.SideEffect {
            PerfLogger.logRender(
                file = "CollectionsScreen.kt",
                function = "CollectionsScreen",
                itemCount = collections.size
            )
        }
    }
}

@Composable
fun CollectionCard(collection: Collection, count: Int, completed: Int, onClick: () -> Unit) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = collection.color),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(collection.icon, collection.name, tint = NeumorphicColors.textPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(collection.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicColors.textPrimary)
                    Text("$completed / $count việc", fontSize = 14.sp, color = NeumorphicColors.textSecondary)
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, "Xem", tint = NeumorphicColors.textSecondary)
        }
    }
}

@Composable
fun CollectionDetailView(collection: Collection, tasks: List<Task>, onDismiss: () -> Unit, onTaskToggle: (Int) -> Unit, onTaskDelete: (Int) -> Unit) {
    Box(Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Back", tint = NeumorphicColors.textPrimary) }
                Text(collection.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary); Spacer(Modifier.width(48.dp))
            }
            if (tasks.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chưa có việc nào", fontSize = 16.sp, color = NeumorphicColors.textSecondary) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { items(tasks, key = { it.id }) { task -> TaskCard(task, { onTaskToggle(task.id) }, { onTaskDelete(task.id) }) } }
        }
    }
}