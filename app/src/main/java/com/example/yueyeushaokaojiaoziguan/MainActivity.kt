package com.example.yueyeushaokaojiaoziguan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantAppContainer
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantTab
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModelFactory
import com.example.yueyeushaokaojiaoziguan.screens.FunctionsScreen
import com.example.yueyeushaokaojiaoziguan.screens.HomeWorkbenchScreen
import com.example.yueyeushaokaojiaoziguan.screens.OrderHistoryScreen
import com.example.yueyeushaokaojiaoziguan.ui.theme.YueyeushaokaojiaoziguanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YueyeushaokaojiaoziguanTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShaokaoMerchantApp()
                }
            }
        }
    }
}

private val tabIcons: Map<MerchantTab, ImageVector> = mapOf(
    MerchantTab.Home to Icons.Default.Home,
    MerchantTab.Orders to Icons.Default.List,
    MerchantTab.Functions to Icons.Default.Settings
)

@Composable
private fun ShaokaoMerchantApp() {
    var currentTab by rememberSaveable { mutableStateOf(MerchantTab.Home) }
    val vm: MerchantViewModel = viewModel(
        factory = MerchantViewModelFactory(MerchantAppContainer.repository)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                MerchantTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tabIcons[tab] ?: Icons.Default.Home,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MerchantTab.Home -> HomeWorkbenchScreen(
                uiState = uiState,
                onAdvanceOrder = vm::advanceOrderStatus,
                onToggleDishServed = vm::toggleDishServed,
                modifier = Modifier.padding(innerPadding)
            )
            MerchantTab.Orders -> OrderHistoryScreen(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding)
            )
            MerchantTab.Functions -> FunctionsScreen(
                uiState = uiState,
                vm = vm,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
