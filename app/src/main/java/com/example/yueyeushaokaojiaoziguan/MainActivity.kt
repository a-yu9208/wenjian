package com.example.yueyeushaokaojiaoziguan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    val tabs = MerchantTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val vm: MerchantViewModel = viewModel(
        factory = MerchantViewModelFactory(MerchantAppContainer.repository)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.noticeMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.noticeMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.dismissError()
            vm.dismissNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            when (tabs[page]) {
                MerchantTab.Home -> HomeWorkbenchScreen(
                    uiState = uiState,
                    onAdvanceOrder = vm::advanceOrderStatus,
                    onToggleDishServed = vm::toggleDishServed,
                    onRefresh = vm::refreshMerchantData
                )
                MerchantTab.Orders -> OrderHistoryScreen(
                    uiState = uiState,
                    vm = vm
                )
                MerchantTab.Functions -> FunctionsScreen(
                    uiState = uiState,
                    vm = vm
                )
            }
        }
    }
}
