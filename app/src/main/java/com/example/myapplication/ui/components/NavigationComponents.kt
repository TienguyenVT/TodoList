package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.NavigationItem
import com.example.myapplication.ui.theme.NeumorphicColors

@Composable
fun NeumorphicBottomNav(currentScreen: NavigationItem, onNavigate: (NavigationItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            // SỬA LỖI: Gọi trực tiếp từng NavItem thay vì dùng vòng lặp Triple gây lỗi biên dịch

            NavItem(
                icon = Icons.Filled.Home,
                isSelected = currentScreen == NavigationItem.MY_DAY,
                onClick = { onNavigate(NavigationItem.MY_DAY) }
            )

            NavItem(
                icon = Icons.Filled.DateRange,
                isSelected = currentScreen == NavigationItem.CALENDAR,
                onClick = { onNavigate(NavigationItem.CALENDAR) }
            )

            NavItem(
                icon = Icons.Filled.List,
                isSelected = currentScreen == NavigationItem.COLLECTIONS,
                onClick = { onNavigate(NavigationItem.COLLECTIONS) }
            )

            NavItem(
                icon = Icons.Filled.Settings,
                isSelected = currentScreen == NavigationItem.SETTINGS,
                onClick = { onNavigate(NavigationItem.SETTINGS) }
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(48.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) NeumorphicColors.darkShadow.copy(0.1f) else NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 4.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary)
        }
    }
}

@Composable
fun NeumorphicFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.size(64.dp).clickable { onClick() },
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(NeumorphicColors.surface, NeumorphicColors.background))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, "Add", tint = NeumorphicColors.textPrimary, modifier = Modifier.size(28.dp))
        }
    }
}