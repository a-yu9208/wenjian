package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.FunctionEntry
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.screens.functions.*
import com.example.yueyeushaokaojiaoziguan.ui.theme.*

private val entryGradients = mapOf(
    FunctionEntry.Revenue to listOf(Color(0xFFFF6B35), Color(0xFFFF8A65)),
    FunctionEntry.DishManage to listOf(Color(0xFFE91E63), Color(0xFFF48FB1)),
    FunctionEntry.CategorySetting to listOf(Color(0xFF7C4DFF), Color(0xFFB388FF)),
    FunctionEntry.TableManage to listOf(Color(0xFF00BCD4), Color(0xFF80DEEA)),
    FunctionEntry.PointsActivity to listOf(Color(0xFFFF9800), Color(0xFFFFCC02)),
    FunctionEntry.Profile to listOf(Color(0xFF607D8B), Color(0xFF90A4AE))
)

@Composable
fun FunctionsScreen(uiState: MerchantUiState, vm: MerchantViewModel, modifier: Modifier = Modifier) {
    var activePageName by rememberSaveable { mutableStateOf<String?>(null) }
    val activePage = activePageName?.let { name -> FunctionEntry.entries.find { it.name == name } }

    if (activePage != null) {
        FunctionSubPage(activePage, uiState, vm, { activePageName = null }, modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(FunctionEntry.entries.toList()) { index, entry ->
                val visible = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible.value = true }
                AnimatedVisibility(
                    visible.value,
                    enter = scaleIn(tween(350, delayMillis = index * 60), initialScale = 0.7f) + fadeIn(tween(350, delayMillis = index * 60))
                ) {
                    FunctionCard(entry) { activePageName = entry.name }
                }
            }
        }
    }
}

@Composable
private fun FunctionCard(entry: FunctionEntry, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.6f), label = "cs")
    val gradient = entryGradients[entry] ?: GradientOrange

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) { Text(entry.icon, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(entry.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(entry.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionSubPage(entry: FunctionEntry, uiState: MerchantUiState, vm: MerchantViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(entry.label, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (entry == FunctionEntry.DishManage) {
                FloatingActionButton(
                    onClick = { vm.showAddDishDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) { Text("+", fontSize = 24.sp, color = Color.White) }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (entry) {
                FunctionEntry.Revenue -> RevenueScreen(uiState, vm)
                FunctionEntry.DishManage -> DishManageScreen(uiState, vm)
                FunctionEntry.CategorySetting -> CategorySettingScreen(uiState, vm)
                FunctionEntry.TableManage -> TableManageScreen(uiState, vm)
                FunctionEntry.PointsActivity -> PointsActivityScreen(uiState, vm)
                FunctionEntry.Profile -> ProfileScreen(uiState, vm)
            }
        }
    }
}
