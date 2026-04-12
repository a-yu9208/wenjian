package com.example.yueyeushaokaojiaoziguan

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.yueyeushaokaojiaoziguan.merchant.*
import com.example.yueyeushaokaojiaoziguan.screens.*
import com.example.yueyeushaokaojiaoziguan.ui.theme.GradientOrange
import com.example.yueyeushaokaojiaoziguan.ui.theme.YueyeushaokaojiaoziguanTheme

class MainActivity : ComponentActivity() {

    private var showPermissionDialog = mutableStateOf(false)
    private var initialTab = mutableStateOf<String?>(null)

    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) showPermissionDialog.value = true
        startSseService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TtsManager.init(this)
        initialTab.value = intent?.getStringExtra("navigate_tab")
        requestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            YueyeushaokaojiaoziguanTheme(dynamicColor = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(showPermissionDialog, initialTab)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("navigate_tab")?.let { initialTab.value = it }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startSseService()
    }

    private fun startSseService() {
        val intent = Intent(this, SseService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}

// ========== Prefs helper ==========
private const val PREFS_NAME = "shaokao_prefs"
private const val KEY_ROLE = "user_role"

private fun saveRole(context: Context, role: UserRole) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_ROLE, role.name).apply()
}

private fun loadRole(context: Context): UserRole? {
    val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ROLE, null)
    return name?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
}

private fun clearRole(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_ROLE).apply()
}

// ========== Root composable ==========
@Composable
private fun AppRoot(showPermissionDialog: MutableState<Boolean>, initialTab: MutableState<String?>) {
    val context = LocalContext.current
    var role by remember { mutableStateOf(loadRole(context)) }

    if (role == null) {
        RoleSelectionScreen { selected ->
            role = selected
            saveRole(context, selected)
        }
    } else {
        ShaokaoMerchantApp(
            role = role!!,
            showPermissionDialog = showPermissionDialog,
            initialTab = initialTab,
            onSwitchRole = { clearRole(context); role = null }
        )
    }
}

// ========== Tab definitions ==========
private data class TabInfo(val filled: ImageVector, val outlined: ImageVector, val label: String)

// Boss tabs
private val bossTabInfoMap = mapOf(
    MerchantTab.Home to TabInfo(Icons.Filled.Home, Icons.Outlined.Home, "首页"),
    MerchantTab.Orders to TabInfo(Icons.Filled.List, Icons.Outlined.List, "订单"),
    MerchantTab.Functions to TabInfo(Icons.Filled.Settings, Icons.Outlined.Settings, "功能")
)

// Staff tabs: Home + Info
private enum class StaffTab { Home, Info }
private val staffTabInfoMap = mapOf(
    StaffTab.Home to TabInfo(Icons.Filled.Home, Icons.Outlined.Home, "工作台"),
    StaffTab.Info to TabInfo(Icons.Filled.Info, Icons.Outlined.Info, "信息")
)

// ========== Main app ==========
@Composable
private fun ShaokaoMerchantApp(
    role: UserRole,
    showPermissionDialog: MutableState<Boolean> = mutableStateOf(false),
    initialTab: MutableState<String?> = mutableStateOf(null),
    onSwitchRole: () -> Unit = {}
) {
    val vm: MerchantViewModel = viewModel(factory = MerchantViewModelFactory(MerchantAppContainer.repository))
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var funcResetTrigger by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Permission dialog
    if (showPermissionDialog.value) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog.value = false },
            title = { Text("需要通知权限", fontWeight = FontWeight.Bold) },
            text = { Text("开启通知权限后，APP在后台也能语音播报新订单和结账提醒。\n\n请前往：设置 → 应用 → 月月烧烤 → 通知 → 开启") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog.value = false
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }) { Text("去设置") }
            },
            dismissButton = { TextButton(onClick = { showPermissionDialog.value = false }) { Text("稍后再说") } }
        )
    }

    // SSE broadcast receiver
    DisposableEffect(vm) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.getStringExtra("alert")?.let { vm.onSseAlert(it) }
            }
        }
        val filter = IntentFilter("com.shaokao.SSE_EVENT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Check update on launch
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

    // Checkout dialog
    uiState.checkoutDialogOrder?.let { order ->
        CheckoutDialog(
            order = order,
            paymentQr = uiState.paymentQr,
            onDismiss = { vm.dismissCheckoutDialog() },
            onNotPaid = { vm.dismissCheckoutDialog() },
            onPaid = { utensilSets -> vm.advanceOrderStatus(order.id, utensilSets) }
        )
    }

    if (role == UserRole.Boss) {
        BossScaffold(uiState, vm, snackbarHostState, initialTab, funcResetTrigger, { funcResetTrigger++ }, onSwitchRole)
    } else {
        StaffScaffold(uiState, vm, snackbarHostState, onSwitchRole)
    }

    // Update dialog
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

// ========== Boss layout (3 tabs: 首页/订单/功能) ==========
@Composable
private fun BossScaffold(
    uiState: MerchantUiState, vm: MerchantViewModel, snackbarHostState: SnackbarHostState,
    initialTab: MutableState<String?>, funcResetTrigger: Int, onFuncReset: () -> Unit, onSwitchRole: () -> Unit
) {
    var currentTabName by rememberSaveable { mutableStateOf(MerchantTab.Home.name) }
    val currentTab = MerchantTab.entries.find { it.name == currentTabName } ?: MerchantTab.Home

    LaunchedEffect(initialTab.value) {
        initialTab.value?.let { tab ->
            MerchantTab.entries.find { it.name == tab }?.let { currentTabName = it.name }
            initialTab.value = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.shadow(8.dp)) {
                MerchantTab.entries.forEach { tab ->
                    val info = bossTabInfoMap[tab]!!
                    val selected = currentTab == tab
                    val iconScale by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = 0.6f), label = "s")
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab == MerchantTab.Functions && currentTab == MerchantTab.Functions) onFuncReset()
                            currentTabName = tab.name
                        },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(GradientOrange), alpha = 0.15f))
                                Icon(if (selected) info.filled else info.outlined, info.label, Modifier.size((24 * iconScale).dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
                    MerchantTab.Home -> HomeWorkbenchScreen(uiState, vm::advanceOrderStatus, vm::toggleDishServed, vm::refreshMerchantData, Modifier.padding(innerPadding), onCheckout = { vm.showCheckoutDialog(it) })
                    MerchantTab.Orders -> OrderHistoryScreen(uiState, vm, Modifier.padding(innerPadding))
                    MerchantTab.Functions -> FunctionsScreen(uiState, vm, Modifier.padding(innerPadding), funcResetTrigger, onSwitchRole = onSwitchRole)
                }
            }
            // Checkout alert banner
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
                            Text("🔔", fontSize = 20.sp); Spacer(Modifier.width(10.dp))
                            Text(msg, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ========== Staff layout (2 tabs: 工作台/信息) ==========
@Composable
private fun StaffScaffold(
    uiState: MerchantUiState, vm: MerchantViewModel, snackbarHostState: SnackbarHostState, onSwitchRole: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(StaffTab.Home.name) }
    val tab = StaffTab.entries.find { it.name == currentTab } ?: StaffTab.Home

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.shadow(8.dp)) {
                StaffTab.entries.forEach { t ->
                    val info = staffTabInfoMap[t]!!
                    val selected = tab == t
                    val iconScale by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = 0.6f), label = "s")
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = t.name },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(GradientOrange), alpha = 0.15f))
                                Icon(if (selected) info.filled else info.outlined, info.label, Modifier.size((24 * iconScale).dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
            when (tab) {
                StaffTab.Home -> StaffWorkbenchScreen(uiState, vm, Modifier.padding(innerPadding))
                StaffTab.Info -> StaffInfoScreen(uiState, vm, Modifier.padding(innerPadding), onSwitchRole)
            }
            // Alert banner
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
                            Text("🔔", fontSize = 20.sp); Spacer(Modifier.width(10.dp))
                            Text(msg, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
