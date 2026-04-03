package com.example.yueyeushaokaojiaoziguan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantAppContainer
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantEndpointNotes
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantIntegrationConfig
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantTab
import com.example.yueyeushaokaojiaoziguan.merchant.QuickEntry
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantUiState
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModel
import com.example.yueyeushaokaojiaoziguan.merchant.MerchantViewModelFactory
import com.example.yueyeushaokaojiaoziguan.merchant.SingleTenantMerchantConfig
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShaokaoMerchantApp() {
    var currentTab by rememberSaveable { mutableStateOf(MerchantTab.Home) }
    val merchantViewModel: MerchantViewModel = viewModel(
        factory = MerchantViewModelFactory(MerchantAppContainer.repository)
    )
    val uiState by merchantViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = SingleTenantMerchantConfig.merchantName,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${SingleTenantMerchantConfig.merchantModeLabel} · Android shell migrated from the mini program",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MerchantTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentTab == tab) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.shortLabel,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTab == tab) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MerchantTab.Home -> HomeScreen(uiState, Modifier.padding(innerPadding))
            MerchantTab.Dishes -> DishesScreen(uiState, Modifier.padding(innerPadding))
            MerchantTab.Orders -> OrdersScreen(uiState, Modifier.padding(innerPadding))
            MerchantTab.Tables -> TablesScreen(
                uiState = uiState,
                onDraftChange = merchantViewModel::updateQrDraft,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(
    uiState: MerchantUiState,
    modifier: Modifier = Modifier
) {
    val totalRevenue = uiState.orders.sumOf { order ->
        order.amount.replace("¥", "").toDoubleOrNull() ?: 0.0
    }
    val checkoutCount = uiState.orders.count { it.status == "Checkout" }
    val occupiedTables = uiState.tables.count { it.status == "Occupied" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C2D12), Color(0xFFC2410C), Color(0xFFF59E0B))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = SingleTenantMerchantConfig.merchantName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Text(
                    text = "One owner account manages the shop here. Customers do not log in to the app and instead scan table QR codes into the WeChat embedded browser.",
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 22.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Single merchant", "WeChat H5 ordering", "API wiring next").forEach { item ->
                        AssistChip(
                            onClick = {},
                            label = { Text(item) }
                        )
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Business Snapshot",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricMiniCard("Orders", uiState.orders.size.toString())
                    MetricMiniCard("Occupied", occupiedTables.toString())
                    MetricMiniCard("Checkout", checkoutCount.toString())
                }
                Text(
                    text = "Estimated live revenue: ¥${"%.2f".format(totalRevenue)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.dashboardStats.forEach { stat ->
                Card(
                    modifier = Modifier.width(164.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stat.title,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stat.value,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stat.note,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        SectionTitle("Migrated Merchant Modules")
        uiState.quickEntries.forEach { entry ->
            FeatureCard(entry)
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Integration Contract",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Cloud env: ${MerchantIntegrationConfig.cloudEnvId}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Merchant account: ${SingleTenantMerchantConfig.merchantAccount}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Customer H5 base: ${SingleTenantMerchantConfig.customerH5BaseUrl}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Text(
                    text = "Collections: ${MerchantIntegrationConfig.collections.joinToString()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Text(
                    text = "Functions: ${MerchantIntegrationConfig.cloudFunctions.joinToString()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Text(
                    text = "API endpoints: ${MerchantEndpointNotes.endpoints.joinToString()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(entry: QuickEntry) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                text = entry.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun DishesScreen(
    uiState: MerchantUiState,
    modifier: Modifier = Modifier
) {
    var keyword by remember { mutableStateOf("") }
    val totalDishes = uiState.dishes.size
    val lowStockCount = uiState.dishes.count { it.stock in 1..10 }
    val comboCount = uiState.dishes.count { it.type.contains("Set", ignoreCase = true) || it.type.contains("Combo", ignoreCase = true) }
    val filtered = remember(keyword, uiState.dishes) {
        uiState.dishes.filter {
            keyword.isBlank() ||
                it.name.contains(keyword, ignoreCase = true) ||
                it.category.contains(keyword, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Dish Management")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricMiniCard("Total", totalDishes.toString())
                MetricMiniCard("Low Stock", lowStockCount.toString())
                MetricMiniCard("Combos", comboCount.toString())
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search dishes or categories") },
                singleLine = true
            )
        }

        items(filtered) { dish ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dish.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${dish.category} · ${dish.type}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = if (dish.stock > 10) "In Stock" else "Low Stock",
                            accent = if (dish.stock > 10) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricMiniCard("Price", dish.price)
                        MetricMiniCard("Stock", "${dish.stock} pcs")
                        MetricMiniCard("Sold Today", "${dish.soldToday}")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersScreen(
    uiState: MerchantUiState,
    modifier: Modifier = Modifier
) {
    var tablewareCount by rememberSaveable { mutableStateOf("2") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var orderKeyword by rememberSaveable { mutableStateOf("") }
    val statusOptions = listOf("All", "Pending", "Grilling", "Checkout")
    val filteredOrders = remember(selectedStatus, orderKeyword, uiState.orders) {
        uiState.orders.filter { order ->
            val statusMatch = selectedStatus == "All" || order.status == selectedStatus
            val keywordMatch = orderKeyword.isBlank() ||
                order.tableLabel.contains(orderKeyword, ignoreCase = true) ||
                order.summary.contains(orderKeyword, ignoreCase = true)
            statusMatch && keywordMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Order Processing")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "This screen maps the mini program flows for pending items, grilling items, and pending checkout.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    AssistChip(
                        onClick = { selectedStatus = status },
                        label = {
                            val count = if (status == "All") uiState.orders.size else uiState.orders.count { it.status == status }
                            Text("$status ($count)")
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = orderKeyword,
                onValueChange = { orderKeyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by table or item") },
                singleLine = true
            )
        }

        items(filteredOrders) { order ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.tableLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        StatusPill(
                            text = order.status,
                            accent = when (order.status) {
                                "Pending" -> Color(0xFF2563EB)
                                "Grilling" -> Color(0xFFEA580C)
                                else -> Color(0xFF15803D)
                            }
                        )
                    }
                    Text(
                        text = order.summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricMiniCard("Amount", order.amount)
                        MetricMiniCard("Created", order.time)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cashier Parameters",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        value = tablewareCount,
                        onValueChange = { tablewareCount = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tableware Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(
                        text = "Later we can connect payment method, tableware fee, and points deduction here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TablesScreen(
    uiState: MerchantUiState,
    onDraftChange: (String?, String?, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedTableStatus by rememberSaveable { mutableStateOf("All") }
    var selectedArea by rememberSaveable { mutableStateOf("All") }
    val tableStatusOptions = listOf("All", "Occupied", "Idle", "Pending Bill")
    val areaOptions = listOf("All") + uiState.tables.map { it.area }.distinct()
    val filteredTables = remember(selectedTableStatus, selectedArea, uiState.tables) {
        uiState.tables.filter { table ->
            val statusMatch = selectedTableStatus == "All" || table.status == selectedTableStatus
            val areaMatch = selectedArea == "All" || table.area == selectedArea
            statusMatch && areaMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Table Management")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "This maps the new table QR logic from the mini program and keeps both mini program and WeChat H5 QR modes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "QR Draft",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("outside", "first", "second").forEachIndexed { index, section ->
                            SegmentedButton(
                                selected = uiState.qrDraft.section == section,
                                onClick = { onDraftChange(section, null, null, null) },
                                shape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                                    2 -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            ) {
                                Text(section)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.qrDraft.tableNumber,
                        onValueChange = { onDraftChange(null, it.filter(Char::isDigit), null, null) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Table Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("h5", "miniProgram").forEachIndexed { index, target ->
                            SegmentedButton(
                                selected = uiState.qrDraft.target == target,
                                onClick = { onDraftChange(null, null, target, null) },
                                shape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                                    1 -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            ) {
                                Text(target)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.qrDraft.baseUrl,
                        onValueChange = { onDraftChange(null, null, null, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Base URL") },
                        singleLine = true,
                        enabled = uiState.qrDraft.target == "h5",
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Text(
                        text = "Preview",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.qrPreviewUrl,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 20.sp
                    )
                    HorizontalDivider()
                    Text(
                        text = "Customer Flow",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Customer scans the table QR with WeChat and opens the ordering page inside the WeChat embedded browser.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    if (uiState.qrPreviewUrl.isNotBlank()) {
                        AssistChip(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.qrPreviewUrl))
                            },
                            label = { Text("Copy Preview Link") }
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tableStatusOptions.forEach { status ->
                    AssistChip(
                        onClick = { selectedTableStatus = status },
                        label = {
                            val count = if (status == "All") uiState.tables.size else uiState.tables.count { it.status == status }
                            Text("$status ($count)")
                        }
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                areaOptions.forEach { area ->
                    AssistChip(
                        onClick = { selectedArea = area },
                        label = {
                            val count = if (area == "All") uiState.tables.size else uiState.tables.count { it.area == area }
                            Text("$area ($count)")
                        }
                    )
                }
            }
        }

        items(filteredTables) { table ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = table.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = table.area,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = table.status,
                            accent = when (table.status) {
                                "Idle" -> Color(0xFF15803D)
                                "Pending Bill" -> Color(0xFFB45309)
                                else -> Color(0xFFDC2626)
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricMiniCard("QR Target", table.qrTarget)
                    }

                    if (table.customerLink.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = "Customer Link",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = table.customerLink,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 20.sp
                        )
                        AssistChip(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(table.customerLink))
                            },
                            label = { Text("Copy Link") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    )
}

@Composable
private fun StatusPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetricMiniCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ShaokaoMerchantAppPreview() {
    YueyeushaokaojiaoziguanTheme(dynamicColor = false) {
        ShaokaoMerchantApp()
    }
}
