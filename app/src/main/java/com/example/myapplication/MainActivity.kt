package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Đã thêm
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField // Đã thêm import này
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Neumorphic Color Scheme
object NeumorphicColors {
    val background = Color(0xFFE0E5EC)
    val surface = Color(0xFFE0E5EC)
    val textPrimary = Color(0xFF4A5568)
    val textSecondary = Color(0xFFA0AEC0)
    val lightShadow = Color(0xFFFFFFFF)
    val darkShadow = Color(0xFFA3B1C6)
    val accentBlue = Color(0xFFB8D4F1)
    val accentPeach = Color(0xFFF5CDB6)
    val accentMint = Color(0xFFB8E6D5)
}

// Data Models
data class Task(
    val id: Int,
    val title: String,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.NORMAL,
    var isCompleted: Boolean = false
)

enum class Priority { LOW, NORMAL, HIGH }
enum class NavigationItem { MY_DAY, CALENDAR, COLLECTIONS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenTaskTheme {
                ZenTaskApp()
            }
        }
    }
}

@Composable
fun ZenTaskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = NeumorphicColors.background,
            surface = NeumorphicColors.surface,
            primary = NeumorphicColors.textPrimary
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenTaskApp() {
    var currentScreen by remember { mutableStateOf(NavigationItem.MY_DAY) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var tasks by remember {
        mutableStateOf(
            listOf(
                Task(1, "Morning meditation", LocalDate.now(), Priority.HIGH),
                Task(2, "Review project proposal", LocalDate.now(), Priority.NORMAL),
                Task(3, "Buy groceries", LocalDate.now().plusDays(1), Priority.LOW)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicColors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    NavigationItem.MY_DAY -> MyDayScreen(
                        tasks = tasks,
                        onTaskToggle = { taskId ->
                            tasks = tasks.map {
                                if (it.id == taskId) it.copy(isCompleted = !it.isCompleted)
                                else it
                            }
                        },
                        onTaskDelete = { taskId ->
                            tasks = tasks.filter { it.id != taskId }
                        }
                    )
                    else -> EmptyStateScreen(currentScreen.name)
                }
            }

            // Bottom Navigation
            NeumorphicBottomNav(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it }
            )
        }

        // Floating Action Button
        NeumorphicFAB(
            onClick = { showAddTaskSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp)
        )

        // Add Task Bottom Sheet
        if (showAddTaskSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddTaskSheet = false },
                containerColor = NeumorphicColors.surface,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            ) {
                AddTaskSheet(
                    onAddTask = { title, date, priority ->
                        tasks = tasks + Task(
                            id = tasks.maxOfOrNull { it.id }?.plus(1) ?: 1,
                            title = title,
                            dueDate = date,
                            priority = priority
                        )
                        showAddTaskSheet = false
                    },
                    onDismiss = { showAddTaskSheet = false }
                )
            }
        }
    }
}

@Composable
fun MyDayScreen(
    tasks: List<Task>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Greeting Header
        Text(
            text = "My Day",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.textPrimary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            fontSize = 16.sp,
            color = NeumorphicColors.textSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Task List
        if (tasks.isEmpty()) {
            EmptyDayState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { onTaskToggle(task.id) },
                        onDelete = { onTaskDelete(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Tối ưu hóa animation
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "drag"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = animatedOffset.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 100 -> {
                                onToggle()
                                offsetX = 0f
                            }
                            offsetX < -100 -> {
                                onDelete()
                                offsetX = 0f
                            }
                            else -> offsetX = 0f
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-150f, 150f)
                    }
                )
            }
    ) {
        NeumorphicCard(
            isPressed = task.isCompleted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicCheckbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = task.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicColors.textPrimary.copy(
                                alpha = if (task.isCompleted) 0.5f else 1f
                            ),
                            textDecoration = if (task.isCompleted)
                                TextDecoration.LineThrough else TextDecoration.None
                        )

                        task.dueDate?.let { date ->
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                                fontSize = 12.sp,
                                color = NeumorphicColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                PriorityIndicator(priority = task.priority)
            }
        }
    }
}

@Composable
fun NeumorphicCard(
    isPressed: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (isPressed) 0.dp else 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = NeumorphicColors.darkShadow,
                spotColor = NeumorphicColors.lightShadow
            )
            .background(
                brush = if (isPressed) {
                    Brush.linearGradient(
                        colors = listOf(
                            NeumorphicColors.darkShadow.copy(alpha = 0.2f),
                            NeumorphicColors.lightShadow.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            NeumorphicColors.surface,
                            NeumorphicColors.surface
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        content()
    }
}

@Composable
fun NeumorphicCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .shadow(
                elevation = if (checked) 0.dp else 6.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = NeumorphicColors.darkShadow,
                spotColor = NeumorphicColors.lightShadow
            )
            .background(
                color = if (checked) NeumorphicColors.accentBlue else NeumorphicColors.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onCheckedChange(!checked) }, // Sửa lại logic click
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = NeumorphicColors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PriorityIndicator(priority: Priority) {
    val color = when (priority) {
        Priority.HIGH -> NeumorphicColors.accentPeach
        Priority.NORMAL -> NeumorphicColors.accentBlue
        Priority.LOW -> NeumorphicColors.accentMint
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun NeumorphicFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = NeumorphicColors.darkShadow,
                spotColor = NeumorphicColors.lightShadow
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeumorphicColors.surface,
                        NeumorphicColors.background
                    )
                ),
                shape = CircleShape
            )
            .clickable { onClick() }, // Sửa logic click cho FAB
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Task",
            tint = NeumorphicColors.textPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun NeumorphicBottomNav(
    currentScreen: NavigationItem,
    onNavigate: (NavigationItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = NeumorphicColors.darkShadow,
                spotColor = NeumorphicColors.lightShadow
            )
            .background(
                color = NeumorphicColors.surface,
                shape = RoundedCornerShape(30.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Thay icon WbSunny -> Home để tránh lỗi import
            NavItem(Icons.Default.Home, "My Day", currentScreen == NavigationItem.MY_DAY) {
                onNavigate(NavigationItem.MY_DAY)
            }
            // Thay icon CalendarToday -> DateRange
            NavItem(Icons.Default.DateRange, "Calendar", currentScreen == NavigationItem.CALENDAR) {
                onNavigate(NavigationItem.CALENDAR)
            }
            // Thay icon GridView -> List
            NavItem(Icons.Default.List, "Collections", currentScreen == NavigationItem.COLLECTIONS) {
                onNavigate(NavigationItem.COLLECTIONS)
            }
            NavItem(Icons.Default.Settings, "Settings", currentScreen == NavigationItem.SETTINGS) {
                onNavigate(NavigationItem.SETTINGS)
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() } // Sửa logic click cho NavItem
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = if (isSelected) 0.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = if (isSelected)
                        NeumorphicColors.darkShadow.copy(alpha = 0.1f)
                    else NeumorphicColors.surface,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AddTaskSheet(
    onAddTask: (String, LocalDate?, Priority) -> Unit,
    onDismiss: () -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.NORMAL) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "New Task",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Task Input Field
        NeumorphicTextField(
            value = taskTitle,
            onValueChange = { taskTitle = it },
            placeholder = "What do you want to do?"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Priority Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Sửa lỗi values() -> entries
            Priority.entries.forEach { priority ->
                PriorityButton(
                    priority = priority,
                    isSelected = selectedPriority == priority,
                    onClick = { selectedPriority = priority }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Add Button
        NeumorphicButton(
            text = "Add Task",
            onClick = {
                if (taskTitle.isNotBlank()) {
                    onAddTask(taskTitle, LocalDate.now(), selectedPriority)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeumorphicColors.darkShadow.copy(alpha = 0.1f),
                        NeumorphicColors.lightShadow.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = NeumorphicColors.textSecondary,
                fontSize = 16.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle( // Sửa lại TextStyle
                color = NeumorphicColors.textPrimary,
                fontSize = 16.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PriorityButton(
    priority: Priority,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when (priority) {
        Priority.LOW -> "Low"
        Priority.NORMAL -> "Normal"
        Priority.HIGH -> "High"
    }

    Box(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 0.dp else 6.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isSelected)
                    NeumorphicColors.darkShadow.copy(alpha = 0.15f)
                else NeumorphicColors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() } // Sửa logic click
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeumorphicColors.accentBlue,
                        NeumorphicColors.accentBlue.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() } // Sửa logic click
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NeumorphicColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyDayState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✨",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All clear for today",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = NeumorphicColors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first task",
            fontSize = 14.sp,
            color = NeumorphicColors.textSecondary
        )
    }
}

@Composable
fun EmptyStateScreen(screenName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$screenName Coming Soon",
            fontSize = 18.sp,
            color = NeumorphicColors.textSecondary
        )
    }
}