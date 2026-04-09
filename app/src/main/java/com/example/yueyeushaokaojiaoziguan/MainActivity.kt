package com.example.yueyeushaokaojiaoziguan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yueyeushaokaojiaoziguan.merchant.*
import com.example.yueyeushaokaojiaoziguan.screens.*
import com.example.yueyeushaokaojiaoziguan.ui.theme.GradientOrange
import com.example.yueyeushaokaojiaoziguan.ui.theme.YueyeushaokaojiaoziguanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TtsManager.init(this)
        enableEdgeToEdge()
        setContent {
            YueyeushaokaojiaoziguanTheme(dynamicColor = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShaokaoMerchantApp()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.shutdown()
    }
}

private data class TabInfo(val filled: ImageVector, val outlined: ImageVector, val label: String)

private val tabInfoMap = mapOf(
    MerchantTab.Home to TabInfo(Icons.Filled.Home, Icons.Outlined.Home, "首页"),
    MerchantTab.Orders to TabInfo(Icons.Filled.List, Icons.Outlined.List, "订单"),
    MerchantTab.Functions to TabInfo(Icons.Filled.Settings, Icons.Outlined.Settings, "功能")
)

@Composable
private fun ShaokaoMerchantApp() {
    var currentTabName by rememberSaveable { mutableStateOf(MerchantTab.Home.name) }
    val currentTab = MerchantTab.entries.find { it.name == currentTabName } ?: MerchantTab.Home
    val vm: MerchantViewModel = viewModel(factory = MerchantViewModelFactory(MerchantAppContainer.repository))
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var funcResetTrigger by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // 启动时检查更新
    LaunchedEffect(Unit) {
        val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        vm.checkForUpdate(currentVersionCode)
    }

    LaunchedEffect(uiState.noticeMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.noticeMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.dismissError(); vm.dismissNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.shadow(8.dp)
            ) {
                MerchantTab.entries.forEach { tab ->
                    val info = tabInfoMap[tab]!!
                    val selected = currentTab == tab
                    val iconScale by animateFloatAsState(
                        if (selected) 1.15f else 1f,
                        spring(dampingRatio = 0.6f), label = "s"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { 
                            if (tab == MerchantTab.Functions && currentTab == MerchantTab.Functions) funcResetTrigger++
                            currentTabName = tab.name
                        },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(16.dp))
                                        .background(Brush.horizontalGradient(GradientOrange), alpha = 0.15f))
                                }
                                Icon(
                                    if (selected) info.filled else info.outlined, info.label,
                                    Modifier.size((24 * iconScale).dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = { Text(info.label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn(tween(250)) + scaleIn(initialScale = 0.96f, animationSpec = tween(250)) togetherWith fadeOut(tween(150)) },
                label = "page"
            ) { tab ->
                when (tab) {
                    MerchantTab.Home -> HomeWorkbenchScreen(uiState, vm::advanceOrderStatus, vm::toggleDishServed, vm::refreshMerchantData, Modifier.padding(innerPadding))
                    MerchantTab.Orders -> OrderHistoryScreen(uiState, vm, Modifier.padding(innerPadding))
                    MerchantTab.Functions -> FunctionsScreen(uiState, vm, Modifier.padding(innerPadding), funcResetTrigger)
                }
            }

            // 全局结账通知横幅
            AnimatedVisibility(
                visible = uiState.checkoutAlert != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = innerPadding.calculateTopPadding())
            ) {
                uiState.checkoutAlert?.let { msg ->
                    LaunchedEffect(msg) { kotlinx.coroutines.delay(10000L); vm.dismissCheckoutAlert() }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35)),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { vm.dismissCheckoutAlert() }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔔", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(msg, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 更新弹窗
    uiState.pendingUpdate?.let { info ->
        val downloadDone = downloading && downloadProgress >= 100
        AlertDialog(
            onDismissRequest = { if (!downloading) vm.dismissUpdate() },
            title = { Text("发现新版本 v${info.versionName}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(info.changelog, fontSize = 14.sp)
                    if (downloading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(if (downloadDone) "下载完成" else "下载中 $downloadProgress%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                if (downloadDone) {
                    TextButton(onClick = { AppUpdater.installDownloaded(context) }) { Text("安装") }
                } else {
                    TextButton(onClick = { downloading = true; AppUpdater.downloadAndInstall(context, info) { downloadProgress = it } }, enabled = !downloading) {
                        Text(if (downloading) "下载中..." else "立即更新")
                    }
                }
            },
            dismissButton = { if (!downloading) TextButton(onClick = { vm.dismissUpdate() }) { Text("稍后再说") } }
        )
    }
}
