package com.example.myapplication.ui.components

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
import androidx.compose.ui.unit.Dp
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
fun NeumorphicNavCard(containerColor: androidx.compose.ui.graphics.Color = NeumorphicColors.surface, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
        ,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        content = { content() }
    )
}

@Composable
fun NeumorphicBottomNav(currentScreen: NavigationItem, onNavigate: (NavigationItem) -> Unit) {
    Box(modifier = Modifier.padding(24.dp)) {
        NeumorphicNavCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Filled.Home,
                    isSelected = currentScreen == NavigationItem.MY_DAY,
                    onClick = { onNavigate(NavigationItem.MY_DAY) },
                    contentDescription = "My Day"
                )

                NavItem(
                    icon = Icons.Filled.DateRange,
                    isSelected = currentScreen == NavigationItem.CALENDAR,
                    onClick = { onNavigate(NavigationItem.CALENDAR) },
                    contentDescription = "Calendar"
                )

                NavItem(
                    icon = Icons.Filled.List,
                    isSelected = currentScreen == NavigationItem.COLLECTIONS,
                    onClick = { onNavigate(NavigationItem.COLLECTIONS) },
                    contentDescription = "Collections"
                )

                NavItem(
                    icon = Icons.Filled.Settings,
                    isSelected = currentScreen == NavigationItem.SETTINGS,
                    onClick = { onNavigate(NavigationItem.SETTINGS) },
                    contentDescription = "Settings"
                )
            }
        }
    }
}


data class MascotNavState(
    val currentScreen: NavigationItem,
    val isMenuOpen: Boolean,
    val dynamicSlotIcon: ImageVector?
)

data class MascotNavActions(
    val onMenuClick: () -> Unit,
    val onDynamicSlotClick: () -> Unit,
    val onNavigate: (NavigationItem) -> Unit
)

@Composable
fun MascotBottomNav(
    state: MascotNavState,
    actions: MascotNavActions
) {
    MascotNavLayout(
        state = state,
        actions = actions
    )
}

@Composable
private fun MascotNavLayout(
    state: MascotNavState,
    actions: MascotNavActions
) {
    val tabCount = 5
    val catWidth = 64.dp
    val rowHorizontalPadding = 5.dp
    val yOffsetAdjustment = 27.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val maxWidth = maxWidth
        val rowWidth = maxWidth - rowHorizontalPadding * 2
        val sectionWidth = rowWidth / tabCount

        // Determine target index
        val selectedIndex = if (state.isMenuOpen) 4 else when (state.currentScreen) {
            NavigationItem.CALENDAR -> 0
            NavigationItem.COLLECTIONS -> 1
            NavigationItem.MY_DAY -> 2
            else -> 3
        }

        // Animation State
        val targetCenter = rowHorizontalPadding + sectionWidth * selectedIndex + sectionWidth / 2
        val targetOffsetX = targetCenter - catWidth / 2
        
        val animatedOffsetX = rememberMascotAnimation(targetOffsetX)
        val facingRight = rememberMascotDirection(selectedIndex)

        // Layer 0: Static Tab Row
        MascotTabRow(
            state = state,
            actions = actions,
            rowHorizontalPadding = rowHorizontalPadding
        )

        // Layer 1: Animated Mascot Overlay
        MascotOverlay(
            animatedOffsetX = animatedOffsetX,
            facingRight = facingRight,
            catWidth = catWidth,
            yOffsetAdjustment = yOffsetAdjustment
        )
    }
}

@Composable
private fun rememberMascotAnimation(targetOffsetX: Dp): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val animatedOffsetX = remember { 
        androidx.compose.animation.core.Animatable(targetOffsetX.value) 
    }

    LaunchedEffect(targetOffsetX) {
        val distance = kotlin.math.abs(targetOffsetX.value - animatedOffsetX.value)
        if (distance > 600f) {
            animatedOffsetX.snapTo(targetOffsetX.value)
        } else {
            animatedOffsetX.animateTo(
                targetValue = targetOffsetX.value,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.75f,
                    stiffness = 50f
                )
            )
        }
    }
    
    return animatedOffsetX
}

@Composable
private fun rememberMascotDirection(selectedIndex: Int): Boolean {
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    var facingRight by remember { mutableStateOf(true) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex > previousIndex) facingRight = true
        else if (selectedIndex < previousIndex) facingRight = false
        previousIndex = selectedIndex
    }
    return facingRight
}

@Composable
private fun MascotTabRow(
    state: MascotNavState,
    actions: MascotNavActions,
    rowHorizontalPadding: Dp
) {
    NeumorphicNavCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = rowHorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FIXED: Using single component for standard nav items to reduce duplication
            StandardNavItemSlot(
                SlotConfig(NavigationItem.CALENDAR, Icons.Filled.DateRange, "Calendar"),
                state, actions.onNavigate
            )
            StandardNavItemSlot(
                SlotConfig(NavigationItem.COLLECTIONS, Icons.Filled.List, "Collections"),
                state, actions.onNavigate
            )
            StandardNavItemSlot(
                SlotConfig(NavigationItem.MY_DAY, Icons.Filled.Home, "My Day"),
                state, actions.onNavigate
            )

            // Dynamic Slot
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val isDynamicSelected = !state.isMenuOpen && 
                                     state.currentScreen !in setOf(NavigationItem.CALENDAR, NavigationItem.COLLECTIONS, NavigationItem.MY_DAY)

                if (state.dynamicSlotIcon != null) {
                    NavItem(icon = state.dynamicSlotIcon, isSelected = isDynamicSelected, onClick = actions.onDynamicSlotClick, contentDescription = "Dynamic Slot")
                } else {
                    NavItem(icon = Icons.Filled.Add, isSelected = false, onClick = actions.onDynamicSlotClick, contentDescription = "Add")
                }
            }

            // Menu Trigger
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavItem(icon = Icons.Filled.Menu, isSelected = state.isMenuOpen, onClick = actions.onMenuClick, contentDescription = "Menu")
            }
        }
    }
}

private data class SlotConfig(
    val item: NavigationItem,
    val icon: ImageVector,
    val description: String
)

@Composable
private fun RowScope.StandardNavItemSlot(
    config: SlotConfig,
    state: MascotNavState,
    onNavigate: (NavigationItem) -> Unit
) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        NavItem(
            icon = config.icon,
            isSelected = !state.isMenuOpen && state.currentScreen == config.item,
            onClick = { onNavigate(config.item) },
            contentDescription = config.description
        )
    }
}

@Composable
private fun MascotOverlay(
    animatedOffsetX: androidx.compose.animation.core.Animatable<Float, *>,
    facingRight: Boolean,
    catWidth: Dp,
    yOffsetAdjustment: Dp
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cat))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .size(catWidth)
            .offset { 
                androidx.compose.ui.unit.IntOffset(
                    x = animatedOffsetX.value.dp.roundToPx(), 
                    y = (yOffsetAdjustment - catWidth).roundToPx()
                ) 
            }
            .graphicsLayer {
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

@Composable
fun NavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, contentDescription: String) {
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
            Icon(icon, contentDescription, tint = tintColor)
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