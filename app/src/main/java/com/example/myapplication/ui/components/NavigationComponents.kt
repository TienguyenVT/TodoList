package com.example.myapplication.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.myapplication.R
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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
fun MascotBottomNav(currentScreen: NavigationItem, onNavigate: (NavigationItem) -> Unit) {
    val tabCount = 4
    val catWidth = 64.dp
    val rowHorizontalPadding = 12.dp

    // Giữ nguyên mức điều chỉnh độ cao (40.dp)
    val yOffsetAdjustment = 27.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val maxWidth = maxWidth

        val selectedIndex = when (currentScreen) {
            NavigationItem.MY_DAY -> 0
            NavigationItem.CALENDAR -> 1
            NavigationItem.COLLECTIONS -> 2
            NavigationItem.SETTINGS -> 3
        }

        var previousIndex by remember { mutableIntStateOf(selectedIndex) }
        var facingRight by remember { mutableStateOf(true) }

        // --- Tính toán tọa độ ---
        val rowWidth = maxWidth - rowHorizontalPadding * 2
        val sectionWidth = rowWidth / tabCount
        val targetCenter = rowHorizontalPadding + sectionWidth * selectedIndex + sectionWidth / 2
        val targetOffsetX = targetCenter - catWidth / 2

        // SỬA ĐỔI CHÍNH: Thêm hiệu ứng lắc nhẹ (Spring)
        val animatedOffsetX by animateDpAsState(
            targetValue = targetOffsetX,
            animationSpec = spring(
                // 0.65f: Tạo độ nảy (overshoot) vừa phải, giúp mèo "lắc" nhẹ khi đến nơi.
                // Nếu muốn lắc mạnh hơn thì giảm xuống 0.5f, muốn ít lắc thì tăng lên 0.8f.
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow // Chuyển động mềm mại, không bị giật cục
            ),
            label = "catOffsetX"
        )

        LaunchedEffect(selectedIndex) {
            if (selectedIndex > previousIndex) {
                facingRight = true
            } else if (selectedIndex < previousIndex) {
                facingRight = false
            }
            previousIndex = selectedIndex
        }

        // Layer 0: Thanh Bottom Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = rowHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.Home,
                        isSelected = currentScreen == NavigationItem.MY_DAY,
                        onClick = { onNavigate(NavigationItem.MY_DAY) }
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.DateRange,
                        isSelected = currentScreen == NavigationItem.CALENDAR,
                        onClick = { onNavigate(NavigationItem.CALENDAR) }
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.List,
                        isSelected = currentScreen == NavigationItem.COLLECTIONS,
                        onClick = { onNavigate(NavigationItem.COLLECTIONS) }
                    )
                }

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.Settings,
                        isSelected = currentScreen == NavigationItem.SETTINGS,
                        onClick = { onNavigate(NavigationItem.SETTINGS) }
                    )
                }
            }
        }

        // Layer 1: Con mèo (Overlay)
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cat))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            modifier = Modifier
                .size(catWidth)
                .offset(x = animatedOffsetX, y = -catWidth + yOffsetAdjustment)
                .graphicsLayer {
                    // Chỉ lật mặt, không xoay (rotation) hay dãn (scale) phức tạp
                    scaleX = if (facingRight) 1f else -1f
                },
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeumorphicColors.darkShadow.copy(0.1f) else NeumorphicColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 4.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val tintColor = if (isSelected) {
                NeumorphicColors.textPrimary.copy(alpha = 0.5f)
            } else {
                NeumorphicColors.textSecondary
            }
            Icon(icon, null, tint = tintColor)
        }
    }
}

@Composable
fun NeumorphicFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .size(64.dp)
            .clickable { onClick() },
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            NeumorphicColors.surface,
                            NeumorphicColors.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                "Add",
                tint = NeumorphicColors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}