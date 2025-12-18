package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.NeumorphicCard
import com.example.myapplication.ui.theme.NeumorphicColors

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("Cài đặt", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary, modifier = Modifier.padding(vertical = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { SettingsSection("Giao diện") { SettingsItem(Icons.Default.Face, "Chủ đề", "Soft UI (Neumorphic)") } }
            item { SettingsSection("Thông tin") { SettingsItem(Icons.Default.Info, "Phiên bản", "1.0.0") } }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textSecondary, modifier = Modifier.padding(vertical = 8.dp))
        NeumorphicCard(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 8.dp)) { content() } }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clickable{}, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, title, tint = NeumorphicColors.textPrimary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NeumorphicColors.textPrimary); Text(subtitle, fontSize = 13.sp, color = NeumorphicColors.textSecondary) }
        Icon(Icons.Default.KeyboardArrowRight, "Nav", tint = NeumorphicColors.textSecondary, modifier = Modifier.size(20.dp))
    }
}