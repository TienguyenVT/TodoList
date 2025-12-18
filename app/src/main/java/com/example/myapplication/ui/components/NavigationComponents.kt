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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .shadow(10.dp, RoundedCornerShape(30.dp), ambientColor = NeumorphicColors.darkShadow, spotColor = NeumorphicColors.lightShadow)
            .background(NeumorphicColors.surface, RoundedCornerShape(30.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
    Box(
        modifier = Modifier.size(48.dp)
            .shadow(if (isSelected) 0.dp else 4.dp, RoundedCornerShape(16.dp))
            .background(if (isSelected) NeumorphicColors.darkShadow.copy(0.1f) else NeumorphicColors.surface, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary)
    }
}

@Composable
fun NeumorphicFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(64.dp)
            .shadow(12.dp, CircleShape, ambientColor = NeumorphicColors.darkShadow, spotColor = NeumorphicColors.lightShadow)
            .background(Brush.radialGradient(listOf(NeumorphicColors.surface, NeumorphicColors.background)), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // SỬA LỖI: Import Icons.Filled.Add rõ ràng ở trên đầu file
        Icon(Icons.Filled.Add, "Add", tint = NeumorphicColors.textPrimary, modifier = Modifier.size(28.dp))
    }
}