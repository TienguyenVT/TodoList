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
import androidx.compose.material.icons.filled.Menu
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
fun MascotBottomNav(
    currentScreen: NavigationItem,
    isMenuOpen: Boolean = false,
    dynamicSlotIcon: ImageVector? = null,
    onMenuClick: () -> Unit,
    onDynamicSlotClick: () -> Unit,
    onNavigate: (NavigationItem) -> Unit
) {
    val tabCount = 5
    val catWidth = 64.dp
    val rowHorizontalPadding = 5.dp

    // Giữ nguyên mức điều chỉnh độ cao (40.dp)
    val yOffsetAdjustment = 27.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val maxWidth = maxWidth

        // Layout: [0:Calendar] [1:Collection] [2:Home] [3:Dynamic] [4:Menu]
        val selectedIndex = if (isMenuOpen) 4 else when (currentScreen) {
            NavigationItem.CALENDAR -> 0
            NavigationItem.COLLECTIONS -> 1
            NavigationItem.MY_DAY -> 2
            else -> 3
        }

        var previousIndex by remember { mutableIntStateOf(selectedIndex) }
        var facingRight by remember { mutableStateOf(true) }

        // --- Tính toán tọa độ ---
        val rowWidth = maxWidth - rowHorizontalPadding * 2
        val sectionWidth = rowWidth / tabCount
        val targetCenter = rowHorizontalPadding + sectionWidth * selectedIndex + sectionWidth / 2
        val targetOffsetX = targetCenter - catWidth / 2
        
        // === ADVANCED ANIMATION: Velocity-Preserving Spring ===
        // Sử dụng Animatable với Spring để bảo toàn vận tốc khi target thay đổi (spam click)
        val animatedOffsetX = remember { 
            androidx.compose.animation.core.Animatable(targetOffsetX.value) 
        }
        
        // Coroutine-based animation
        LaunchedEffect(targetOffsetX) {
            // Log target change
            // android.util.Log.d("PerfDebug", "🎯 TARGET: ${targetOffsetX.value}dp")
            
            // Tính khoảng cách
            val distance = kotlin.math.abs(targetOffsetX.value - animatedOffsetX.value)
            
            // Chỉ Snap nếu khoảng cách CỰC KỲ xa (> 1.5 lần chiều rộng màn hình - hiếm khi xảy ra)
            // Việc snap ở khoảng cách ngắn (như 150dp) gây cảm giác giật cục
            if (distance > 600f) {
                animatedOffsetX.snapTo(targetOffsetX.value)
            } else {
                // Sử dụng Spring để có chuyển động tự nhiên và bảo toàn quán tính
                // TUNED: 
                // - Stiffness 400f: Giảm tốc độ (~15% chậm hơn so với 500-700f), tạo cảm giác "lướt"
                // - Damping 0.75f: Nảy nhẹ (soft bounce) ở đích, không quá cứng nhưng không quá lỏng lẻo
                animatedOffsetX.animateTo(
                    targetValue = targetOffsetX.value,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.75f,
                        stiffness = 50f
                    )
                )
            }
        }
        
        // Convert Animatable value thành Dp
        val currentOffsetDp = animatedOffsetX.value.dp
        
        // === OPTIMIZED LOGGING ===
        // Chỉ log khi thực sự có issue để giảm overhead cho UI Thread
        var lastLogTime by remember { mutableStateOf(0L) }
        androidx.compose.runtime.SideEffect {
            val now = System.currentTimeMillis()
            if (lastLogTime > 0) {
                val delta = now - lastLogTime
                // Chỉ warn nếu frame gap > 32ms (dropped > 2 frames)
                if (delta > 32) { 
                     // Dùng String builder đơn giản hoặc log ngắn gọn nhất
                     android.util.Log.d("PerfDebug", "⚠️ DROP: ${delta}ms")
                }
            }
            lastLogTime = now
        }

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
                // 0: Calendar
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.DateRange,
                        isSelected = !isMenuOpen && currentScreen == NavigationItem.CALENDAR,
                        onClick = { onNavigate(NavigationItem.CALENDAR) }
                    )
                }

                // 1: Collections
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.List,
                        isSelected = !isMenuOpen && currentScreen == NavigationItem.COLLECTIONS,
                        onClick = { onNavigate(NavigationItem.COLLECTIONS) }
                    )
                }

                // 2: Home (My Day)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.Home,
                        isSelected = !isMenuOpen && currentScreen == NavigationItem.MY_DAY,
                        onClick = { onNavigate(NavigationItem.MY_DAY) }
                    )
                }

                // 3: Dynamic Slot
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                     val isDynamicSelected = !isMenuOpen && 
                                          currentScreen != NavigationItem.CALENDAR && 
                                          currentScreen != NavigationItem.COLLECTIONS && 
                                          currentScreen != NavigationItem.MY_DAY

                    if (dynamicSlotIcon != null) {
                        NavItem(
                            icon = dynamicSlotIcon,
                            isSelected = isDynamicSelected,
                            onClick = { onDynamicSlotClick() }
                        )
                    } else {
                        // Empty placeholder
                        Box(
                             modifier = Modifier
                                 .size(48.dp)
                                 .clickable { onDynamicSlotClick() }, 
                             contentAlignment = Alignment.Center
                        ) {}
                    }
                }

                // 4: Menu Trigger
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavItem(
                        icon = Icons.Filled.Menu,
                        isSelected = isMenuOpen,
                        onClick = { onMenuClick() }
                    )
                }
            }
        }

        // Layer 1: Con mèo (Overlay) - Hardware Accelerated
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cat))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            modifier = Modifier
                .size(catWidth)
                .offset { 
                    // OPTIMIZATION: Use lambda offset to skip Composition phase, running only in Layout phase
                    // This is crucial for avoiding 60fps recomposition
                    androidx.compose.ui.unit.IntOffset(
                        x = animatedOffsetX.value.dp.roundToPx(), 
                        y = (-catWidth + yOffsetAdjustment).roundToPx()
                    ) 
                }
                .graphicsLayer {
                    // Hardware layer để animation chạy riêng biệt, không bị block bởi UI thread
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                    // Lật mặt theo hướng di chuyển
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