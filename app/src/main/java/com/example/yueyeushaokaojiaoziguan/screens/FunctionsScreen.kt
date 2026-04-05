package com.example.yueyeushaokaojiaoziguan.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.FunctionEntry
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.screens.functions.*

@Composable
fun FunctionsScreen(
    uiState: MerchantUiState,
    vm: MerchantViewModel,
    modifier: Modifier = Modifier
) {
    var activePageName by rememberSaveable { mutableStateOf<String?>(null) }
    val activePage = activePageName?.let { name -> FunctionEntry.entries.find { it.name == name } }

    if (activePage != null) {
        FunctionSubPage(
            entry = activePage,
            uiState = uiState,
            vm = vm,
            onBack = { activePageName = null },
            modifier = modifier
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(FunctionEntry.entries.toList()) { entry ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { activePageName = entry.name },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(entry.icon, fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(entry.label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionSubPage(
    entry: FunctionEntry,
    uiState: MerchantUiState,
    vm: MerchantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(entry.label) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        },
        floatingActionButton = {
            if (entry == FunctionEntry.DishManage) {
                FloatingActionButton(
                    onClick = { vm.showAddDishDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("+", fontSize = 24.sp, color = Color.White)
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (entry) {
                FunctionEntry.Revenue -> RevenueScreen(uiState)
                FunctionEntry.DishManage -> DishManageScreen(uiState, vm)
                FunctionEntry.CategorySetting -> CategorySettingScreen(uiState, vm)
                FunctionEntry.TableManage -> TableManageScreen(uiState, vm)
                FunctionEntry.PointsActivity -> PointsActivityScreen(uiState, vm)
                FunctionEntry.Profile -> ProfileScreen(uiState, vm)
            }
        }
    }
}
