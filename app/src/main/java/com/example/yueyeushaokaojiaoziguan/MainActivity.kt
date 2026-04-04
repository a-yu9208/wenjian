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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TextButton
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
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            text = SingleTenantMerchantConfig.merchantName,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "单店商家端",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = merchantViewModel::refreshMerchantData,
                        enabled = !uiState.loading && !uiState.refreshing
                    ) {
                        Text(if (uiState.refreshing) "刷新中..." else "刷新")
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
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentTab == tab) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
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
            MerchantTab.Dishes -> DishesScreen(
                uiState = uiState,
                onAdjustStock = merchantViewModel::updateDishStock,
                onDraftChange = merchantViewModel::updateDishDraft,
                onCategoryDraftChange = merchantViewModel::updateCategoryDraft,
                onAddCategory = merchantViewModel::addCategoryFromDraft,
                onRemoveCategory = merchantViewModel::removeCategory,
                onAddDish = merchantViewModel::addDishFromDraft,
                modifier = Modifier.padding(innerPadding)
            )
            MerchantTab.Orders -> OrdersScreen(
                uiState = uiState,
                onAdvanceOrder = merchantViewModel::advanceOrderStatus,
                modifier = Modifier.padding(innerPadding)
            )
            MerchantTab.Tables -> TablesScreen(
                uiState = uiState,
                onDraftChange = merchantViewModel::updateQrDraft,
                onGenerateQr = merchantViewModel::generateQrForDraft,
                onApplyDraft = merchantViewModel::applyDraftToTable,
                onToggleTableStatus = merchantViewModel::toggleTableStatus,
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
        order.amount.replace("¥", "").replace(",", "").toDoubleOrNull() ?: 0.0
    }
    val activeOrders = uiState.orders.count { it.status == "待处理" || it.status == "烧烤中" }
    val occupiedTables = uiState.tables.count { it.status == "使用中" }
    val lowStockCount = uiState.dishes.count { it.stock in 1..10 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MerchantStatusSection(uiState)

        HeroCard(
            title = "今晚经营概览",
            subtitle = "顾客扫码后直接进入微信内置浏览器点餐，店主只在这里看单、管桌、调库存。",
            badges = listOf("单店模式", "微信 H5 点餐", "后端已接入")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard("进行中的订单", activeOrders.toString(), "当前最优先处理")
            SummaryCard("在用桌台", occupiedTables.toString(), "关注翻台节奏")
            SummaryCard("低库存菜品", lowStockCount.toString(), "及时补货或下架")
            SummaryCard("预计营业额", "¥${"%.0f".format(totalRevenue)}", "按当前订单汇总")
        }

        SectionCard(title = "今日重点") {
            PriorityRow("待处理订单", "${uiState.orders.count { it.status == "待处理" }} 单")
            PriorityRow("烧烤中订单", "${uiState.orders.count { it.status == "烧烤中" }} 单")
            PriorityRow("待结账桌台", "${uiState.tables.count { it.status == "待结账" }} 桌")
        }

        SectionCard(title = "常用模块") {
            uiState.quickEntries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }
                FeatureCard(entry)
            }
        }

        SectionCard(title = "接入信息") {
            InfoLine("云环境", MerchantIntegrationConfig.cloudEnvId)
            InfoLine("商家账号", SingleTenantMerchantConfig.merchantAccount)
            InfoLine("顾客地址", SingleTenantMerchantConfig.customerH5BaseUrl)
            InfoLine("接口", MerchantEndpointNotes.endpoints.joinToString(" / "))
        }
    }
}

@Composable
private fun FeatureCard(entry: QuickEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = entry.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
        Text(
            text = entry.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeroCard(
    title: String,
    subtitle: String,
    badges: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6B2E16), Color(0xFFB45309), Color(0xFFF4B740))
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.92f),
                lineHeight = 22.sp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    AssistChip(onClick = {}, label = { Text(badge) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, note: String) {
    Card(
        modifier = Modifier.width(165.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = note,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            content()
        }
    }
}

@Composable
private fun PriorityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Text(text = value, lineHeight = 20.sp)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DishesScreen(
    uiState: MerchantUiState,
    onAdjustStock: (String, Int) -> Unit,
    onDraftChange: (String?, String?, String?, String?, String?) -> Unit,
    onCategoryDraftChange: (String) -> Unit,
    onAddCategory: () -> Unit,
    onRemoveCategory: (String) -> Unit,
    onAddDish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("全部") }
    val categoryOptions = remember(uiState.categories) { listOf("全部") + uiState.categories }
    val filtered = remember(keyword, selectedCategory, uiState.dishes) {
        uiState.dishes.filter { dish ->
            val keywordMatch = keyword.isBlank() ||
                dish.name.contains(keyword, ignoreCase = true) ||
                dish.category.contains(keyword, ignoreCase = true)
            val categoryMatch = selectedCategory == "全部" || dish.category == selectedCategory
            keywordMatch && categoryMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MerchantStatusSection(uiState)
            PageIntro(
                title = "菜品管理",
                subtitle = "先看库存和售卖情况，再决定补货、上新或做套餐。"
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard("菜品总数", uiState.dishes.size.toString(), "当前上架数量")
                SummaryCard("分类数", uiState.categories.count { it != "未分类" }.toString(), "菜单分组更清晰")
                SummaryCard("低库存", uiState.dishes.count { it.stock in 1..10 }.toString(), "尽快处理")
                SummaryCard("套餐", uiState.dishes.count { it.type.contains("套餐") }.toString(), "适合做主推")
            }
        }

        item {
            SectionCard(title = "分类管理") {
                Text(
                    text = "分类现在可以直接增删。删掉分类后，原有菜品会自动归到未分类，不会丢。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.categoryDraft,
                    onValueChange = onCategoryDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新增分类名称") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onAddCategory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加分类")
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { category ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column {
                                    Text(category, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${uiState.dishes.count { it.category == category }} 道菜",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (category != "未分类") {
                                    TextButton(onClick = { onRemoveCategory(category) }) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "新增菜品") {
                OutlinedTextField(
                    value = uiState.dishDraft.name,
                    onValueChange = { onDraftChange(it, null, null, null, null) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("菜品名称") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "选择分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.forEach { category ->
                        AssistChip(
                            onClick = { onDraftChange(null, category, null, null, null) },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(text = "当前分类：${uiState.dishDraft.category}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(text = "菜品类型", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("单品", "套餐", "招牌", "饮品").forEach { type ->
                        AssistChip(
                            onClick = { onDraftChange(null, null, null, null, type) },
                            label = { Text(type) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(text = "当前类型：${uiState.dishDraft.type}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.dishDraft.price,
                        onValueChange = { onDraftChange(null, null, it.filter { ch -> ch.isDigit() || ch == '.' }, null, null) },
                        modifier = Modifier.weight(1f),
                        label = { Text("价格") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.dishDraft.stock,
                        onValueChange = { onDraftChange(null, null, null, it.filter(Char::isDigit), null) },
                        modifier = Modifier.weight(1f),
                        label = { Text("库存") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onAddDish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存并上架")
                }
            }
        }

        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索菜品或分类") },
                singleLine = true
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryOptions.forEach { category ->
                    AssistChip(
                        onClick = { selectedCategory = category },
                        label = {
                            val count = if (category == "全部") uiState.dishes.size else uiState.dishes.count { it.category == category }
                            Text("$category ($count)")
                        }
                    )
                }
            }
        }

        items(filtered) { dish ->
            ListCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dish.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${dish.category} · ${dish.type}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = if (dish.stock > 10) "库存充足" else "库存偏低",
                        accent = if (dish.stock > 10) Color(0xFF1D7A44) else Color(0xFFB45309)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniInfo("价格", dish.price)
                    MiniInfo("库存", "${dish.stock} 份")
                    MiniInfo("今日已售", "${dish.soldToday}")
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAdjustStock(dish.name, -1) }) {
                        Text("库存 -1")
                    }
                    TextButton(onClick = { onAdjustStock(dish.name, 1) }) {
                        Text("库存 +1")
                    }
                }
            }
        }

        if (filtered.isEmpty() && !uiState.loading && !uiState.refreshing) {
            item {
                EmptyStateCard("当前没有符合条件的菜品")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun OrdersScreen(
    uiState: MerchantUiState,
    onAdvanceOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by rememberSaveable { mutableStateOf("全部") }
    var keyword by rememberSaveable { mutableStateOf("") }
    val statusOptions = listOf("全部", "待处理", "烧烤中", "待结账", "已完成")
    val filteredOrders = remember(selectedStatus, keyword, uiState.orders) {
        uiState.orders.filter { order ->
            val statusMatch = selectedStatus == "全部" || order.status == selectedStatus
            val keywordMatch = keyword.isBlank() ||
                order.tableLabel.contains(keyword, ignoreCase = true) ||
                order.summary.contains(keyword, ignoreCase = true)
            statusMatch && keywordMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MerchantStatusSection(uiState)
            PageIntro(
                title = "订单处理",
                subtitle = "把待处理订单尽快推进到出餐和结账，老板只关心当前该做什么。"
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    AssistChip(
                        onClick = { selectedStatus = status },
                        label = {
                            val count = if (status == "全部") uiState.orders.size else uiState.orders.count { it.status == status }
                            Text("$status ($count)")
                        }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("按桌号或菜品搜索") },
                singleLine = true
            )
        }

        items(filteredOrders) { order ->
            ListCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.tableLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = order.summary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                    StatusPill(
                        text = order.status,
                        accent = when (order.status) {
                            "待处理" -> Color(0xFF2563EB)
                            "烧烤中" -> Color(0xFFEA580C)
                            "待结账" -> Color(0xFFB45309)
                            else -> Color(0xFF1D7A44)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniInfo("金额", order.amount)
                    MiniInfo("时间", order.time)
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { onAdvanceOrder(order.tableLabel) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (order.status) {
                            "待处理" -> "开始制作"
                            "烧烤中" -> "转为待结账"
                            "待结账" -> "完成结账"
                            else -> "重新开始"
                        }
                    )
                }
            }
        }

        if (filteredOrders.isEmpty() && !uiState.loading && !uiState.refreshing) {
            item {
                EmptyStateCard("当前没有符合条件的订单")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TablesScreen(
    uiState: MerchantUiState,
    onDraftChange: (String?, String?, String?, String?) -> Unit,
    onGenerateQr: () -> Unit,
    onApplyDraft: () -> Unit,
    onToggleTableStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedTableStatus by rememberSaveable { mutableStateOf("全部") }
    var selectedArea by rememberSaveable { mutableStateOf("全部") }
    val tableStatusOptions = listOf("全部", "使用中", "空闲", "待结账")
    val areaOptions = listOf("全部") + uiState.tables.map { it.area }.distinct()
    val filteredTables = remember(selectedTableStatus, selectedArea, uiState.tables) {
        uiState.tables.filter { table ->
            val statusMatch = selectedTableStatus == "全部" || table.status == selectedTableStatus
            val areaMatch = selectedArea == "全部" || table.area == selectedArea
            statusMatch && areaMatch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MerchantStatusSection(uiState)
            PageIntro(
                title = "桌台管理",
                subtitle = "这里负责桌码、桌态和顾客入口。老板最常用的动作都收在一张卡里。"
            )
        }

        item {
            SectionCard(title = "桌码生成") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("室外" to "outside", "一楼" to "first", "二楼" to "second").forEachIndexed { index, section ->
                        SegmentedButton(
                            selected = uiState.qrDraft.section == section.second,
                            onClick = { onDraftChange(section.second, null, null, null) },
                            shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                                2 -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        ) {
                            Text(section.first)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.qrDraft.tableNumber,
                        onValueChange = { onDraftChange(null, it.filter(Char::isDigit), null, null) },
                        modifier = Modifier.weight(1f),
                        label = { Text("桌号") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.qrDraft.baseUrl,
                        onValueChange = { onDraftChange(null, null, null, it) },
                        modifier = Modifier.weight(1.4f),
                        label = { Text("H5 域名") },
                        singleLine = true,
                        enabled = uiState.qrDraft.target == "h5",
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("微信 H5" to "h5", "小程序" to "miniProgram").forEachIndexed { index, target ->
                        SegmentedButton(
                            selected = uiState.qrDraft.target == target.second,
                            onClick = { onDraftChange(null, null, target.second, null) },
                            shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
                                1 -> RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        ) {
                            Text(target.first)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(text = "预览链接", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.qrPreviewUrl,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 20.sp
                )
                if (uiState.qrFileId.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "桌码文件：${uiState.qrFileId}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onGenerateQr) {
                        Text("生成桌码")
                    }
                    TextButton(onClick = onApplyDraft) {
                        Text("应用到桌台")
                    }
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(uiState.qrPreviewUrl))
                        }
                    ) {
                        Text("复制链接")
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tableStatusOptions.forEach { status ->
                    AssistChip(
                        onClick = { selectedTableStatus = status },
                        label = {
                            val count = if (status == "全部") uiState.tables.size else uiState.tables.count { it.status == status }
                            Text("$status ($count)")
                        }
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                areaOptions.forEach { area ->
                    AssistChip(
                        onClick = { selectedArea = area },
                        label = {
                            val count = if (area == "全部") uiState.tables.size else uiState.tables.count { it.area == area }
                            Text("$area ($count)")
                        }
                    )
                }
            }
        }

        items(filteredTables) { table ->
            ListCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = table.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${table.area} · ${table.qrTarget}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = table.status,
                        accent = when (table.status) {
                            "空闲" -> Color(0xFF1D7A44)
                            "待结账" -> Color(0xFFB45309)
                            else -> Color(0xFFDC2626)
                        }
                    )
                }
                if (table.customerLink.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = table.customerLink,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 20.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onToggleTableStatus(table.label) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when (table.status) {
                                "空闲" -> "设为使用中"
                                "使用中" -> "转为待结账"
                                "待结账" -> "恢复空闲"
                                else -> "更新状态"
                            }
                        )
                    }
                    if (table.customerLink.isNotBlank()) {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(table.customerLink))
                            }
                        ) {
                            Text("复制顾客链接")
                        }
                    }
                }
            }
        }

        if (filteredTables.isEmpty() && !uiState.loading && !uiState.refreshing) {
            item {
                EmptyStateCard("当前没有符合条件的桌台")
            }
        }
    }
}

@Composable
private fun PageIntro(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ListCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun MerchantStatusSection(uiState: MerchantUiState) {
    when {
        uiState.loading -> {
            LoadingStatusCard("正在加载数据，请稍候...")
        }

        uiState.refreshing -> {
            LoadingStatusCard("正在刷新最新数据...")
        }

        !uiState.errorMessage.isNullOrBlank() -> {
            MessageStatusCard(uiState.errorMessage ?: "数据加载失败")
        }

        !uiState.noticeMessage.isNullOrBlank() -> {
            MessageStatusCard(uiState.noticeMessage ?: "")
        }
    }
}

@Composable
private fun LoadingStatusCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageStatusCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3D5B5))
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
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

@Composable
private fun MiniInfo(label: String, value: String) {
    MetricMiniCard(label = label, value = value)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun ShaokaoMerchantAppPreview() {
    YueyeushaokaojiaoziguanTheme(dynamicColor = false) {
        ShaokaoMerchantApp()
    }
}


