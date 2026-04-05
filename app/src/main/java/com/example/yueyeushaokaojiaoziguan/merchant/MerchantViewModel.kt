package com.example.yueyeushaokaojiaoziguan.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MerchantViewModel(
    private val repository: MerchantRepository = MockMerchantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantUiState())
    val uiState: StateFlow<MerchantUiState> = _uiState.asStateFlow()
    private var sseJob: Job? = null

    init {
        loadMerchantData()
        startSseListener()
    }

    fun loadMerchantData() {
        fetchMerchantData(initialLoad = true)
    }

    fun refreshMerchantData() {
        fetchMerchantData(initialLoad = false)
    }

    private fun startSseListener() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val url = java.net.URL("${MerchantApiConfig.baseApiUrl}/merchant/events")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 0  // 无超时，保持长连接
                    conn.setRequestProperty("Accept", "text/event-stream")
                    conn.inputStream.bufferedReader().use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.startsWith("data:")) {
                                withContext(Dispatchers.Main) {
                                    fetchMerchantData(initialLoad = false)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // 连接断开，等 3 秒重连
                }
                kotlinx.coroutines.delay(3000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }

    fun updateQrDraft(
        section: String? = null,
        tableNumber: String? = null,
        target: String? = null,
        baseUrl: String? = null
    ) {
        val current = _uiState.value.qrDraft
        val next = current.copy(
            section = section ?: current.section,
            tableNumber = tableNumber ?: current.tableNumber,
            target = target ?: current.target,
            baseUrl = baseUrl ?: current.baseUrl
        )

        _uiState.value = _uiState.value.copy(
            qrDraft = next,
            qrPreviewUrl = buildQrPreview(next)
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissNotice() {
        _uiState.value = _uiState.value.copy(noticeMessage = null)
    }

    fun showAddDishDialog() {
        _uiState.value = _uiState.value.copy(showAddDishDialog = true)
    }

    fun uploadDishImage(imageBytes: ByteArray, fileName: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = runCatching {
                (repository as? CloudBackedMerchantRepository)?.let {
                    (it.bridge as? HttpMerchantCloudBridge)?.uploadImage(imageBytes, fileName)
                }
            }.getOrNull()
            withContext(Dispatchers.Main) { onResult(url) }
        }
    }

    fun hideAddDishDialog() {
        _uiState.value = _uiState.value.copy(showAddDishDialog = false)
    }

    fun updateDishStock(name: String, delta: Int) {
        val nextState = _uiState.value.let { state ->
            val updated = state.dishes.map { dish ->
                if (dish.name == name) {
                    val nextStock = (dish.stock + delta).coerceAtLeast(0)
                    dish.copy(stock = nextStock)
                } else {
                    dish
                }
            }
            state.copy(
                dishes = updated,
                noticeMessage = "已更新 $name 的库存"
            )
        }
        _uiState.value = nextState
        val targetDish = nextState.dishes.firstOrNull { it.name == name } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.pushDishStock(name, targetDish.stock) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "库存已本地更新，但同步后端失败：${it.message ?: "请稍后重试"}"
                    )
                }
        }
    }

    fun updateDishDraft(
        name: String? = null,
        category: String? = null,
        price: String? = null,
        stock: String? = null,
        type: String? = null,
        description: String? = null,
        minOrder: String? = null,
        quickServe: Boolean? = null,
        discountEnabled: Boolean? = null,
        discountPrice: String? = null
    ) {
        val current = _uiState.value.dishDraft
        _uiState.value = _uiState.value.copy(
            dishDraft = current.copy(
                name = name ?: current.name,
                category = category ?: current.category,
                price = price ?: current.price,
                stock = stock ?: current.stock,
                type = type ?: current.type,
                description = description ?: current.description,
                minOrder = minOrder ?: current.minOrder,
                quickServe = quickServe ?: current.quickServe,
                discountEnabled = discountEnabled ?: current.discountEnabled,
                discountPrice = discountPrice ?: current.discountPrice
            )
        )
    }

    fun updateDishDraftComboItems(items: List<ComboItem>) {
        _uiState.value = _uiState.value.copy(
            dishDraft = _uiState.value.dishDraft.copy(comboItems = items)
        )
    }

    fun updateCategoryDraft(value: String) {
        _uiState.value = _uiState.value.copy(categoryDraft = value)
    }

    fun addCategoryFromDraft() {
        val draft = _uiState.value.categoryDraft.trim()
        if (draft.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先输入分类名称")
            return
        }

        val exists = _uiState.value.categories.any { it.equals(draft, ignoreCase = true) }
        if (exists) {
            _uiState.value = _uiState.value.copy(
                categoryDraft = "",
                noticeMessage = "分类 $draft 已存在",
                errorMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            categories = (_uiState.value.categories + draft).distinct(),
            categoryDraft = "",
            dishDraft = _uiState.value.dishDraft.copy(category = draft),
            noticeMessage = "已新增分类：$draft",
            errorMessage = null
        )
    }

    fun removeCategory(category: String) {
        val normalized = category.trim()
        if (normalized.isBlank()) return
        val currentCats = _uiState.value.categories
        if (currentCats.size <= 1) {
            _uiState.value = _uiState.value.copy(errorMessage = "至少保留一个分类")
            return
        }

        val nextCategories = currentCats.filterNot { it == normalized }
        val fallback = nextCategories.first()

        val nextDishes = _uiState.value.dishes.map { dish ->
            if (dish.category == normalized) dish.copy(category = fallback) else dish
        }
        val nextDraft = if (_uiState.value.dishDraft.category == normalized) {
            _uiState.value.dishDraft.copy(category = fallback)
        } else {
            _uiState.value.dishDraft
        }

        _uiState.value = _uiState.value.copy(
            categories = nextCategories,
            dishes = nextDishes,
            dishDraft = nextDraft,
            noticeMessage = "已删除分类：$normalized，原分类菜品已归到 $fallback",
            errorMessage = null
        )
    }

    fun moveCategoryUp(category: String) {
        val cats = _uiState.value.categories.toMutableList()
        val idx = cats.indexOf(category)
        if (idx > 0) {
            cats[idx] = cats[idx - 1].also { cats[idx - 1] = cats[idx] }
            _uiState.value = _uiState.value.copy(categories = cats)
            syncCategoryOrder(cats)
        }
    }

    fun moveCategoryDown(category: String) {
        val cats = _uiState.value.categories.toMutableList()
        val idx = cats.indexOf(category)
        if (idx >= 0 && idx < cats.size - 1) {
            cats[idx] = cats[idx + 1].also { cats[idx + 1] = cats[idx] }
            _uiState.value = _uiState.value.copy(categories = cats)
            syncCategoryOrder(cats)
        }
    }

    private fun syncCategoryOrder(cats: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = cats.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" }
            MerchantHttpClient().post("/merchant/categories/order", """{"categories":$json}""")
        }
    }

    fun deleteDishes(names: Set<String>) {
        _uiState.value = _uiState.value.copy(
            dishes = _uiState.value.dishes.filterNot { it.name in names },
            noticeMessage = "已删除 ${names.size} 道菜品"
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.deleteDishes(names) }
        }
    }

    fun updateDish(original: DishItem, updated: DishItem) {
        _uiState.value = _uiState.value.copy(
            dishes = _uiState.value.dishes.map { if (it.name == original.name) updated else it },
            noticeMessage = "已更新 ${updated.name}"
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.updateDish(updated) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorMessage = "同步后端失败：${it.message}")
                }
        }
    }

    fun batchUpdateCategory(names: Set<String>, category: String) {
        _uiState.value = _uiState.value.copy(
            dishes = _uiState.value.dishes.map { if (it.name in names) it.copy(category = category) else it },
            noticeMessage = "已将 ${names.size} 道菜品移至 $category"
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.batchUpdateCategory(names, category) }
        }
    }

    fun addDishFromDraft(): Boolean {
        val draft = _uiState.value.dishDraft
        val priceValue = draft.price.trim()
        val stockValue = draft.stock.toIntOrNull()
        if (draft.name.isBlank() || priceValue.isBlank() || stockValue == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先填写完整的菜品名称、价格和库存")
            return false
        }

        val normalizedPrice = if (priceValue.startsWith("¥")) priceValue else "¥$priceValue"
        val comboDesc = if (draft.type == "套餐" && draft.comboItems.isNotEmpty()) {
            draft.comboItems.joinToString("、") { "${it.name}x${it.quantity}" }
        } else draft.description

        val newDish = DishItem(
            name = draft.name.trim(),
            category = draft.category,
            price = normalizedPrice,
            stock = stockValue,
            soldToday = 0,
            type = draft.type,
            description = comboDesc,
            minOrder = draft.minOrder.toIntOrNull() ?: 1,
            quickServe = draft.quickServe,
            discountEnabled = draft.discountEnabled,
            discountPrice = if (draft.discountEnabled) {
                val dp = draft.discountPrice.trim()
                if (dp.startsWith("¥")) dp else "¥$dp"
            } else "",
            comboItems = if (draft.type == "套餐") draft.comboItems else emptyList()
        )

        _uiState.value = _uiState.value.copy(
            dishes = listOf(newDish) + _uiState.value.dishes.filterNot { it.name == newDish.name },
            categories = mergeCategories(
                dishes = listOf(newDish) + _uiState.value.dishes,
                existing = _uiState.value.categories
            ),
            dishDraft = DishDraft(category = draft.category, type = draft.type),
            noticeMessage = "已新增菜品：${newDish.name}",
            errorMessage = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.createDish(newDish) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "菜品已本地新增，但同步后端失败：${it.message ?: "请稍后重试"}"
                    )
                }
        }
        return true
    }

    fun toggleDishServed(tableLabel: String, time: String, dishName: String) {
        _uiState.value = _uiState.value.let { state ->
            val updatedOrders = state.orders.map { order ->
                if (order.tableLabel == tableLabel && order.time == time) {
                    val updatedDishes = order.dishes.map { dish ->
                        if (dish.subItems.isNotEmpty()) {
                            val updatedSubs = dish.subItems.map { sub ->
                                if (sub.name == dishName) sub.copy(served = !sub.served) else sub
                            }
                            dish.copy(subItems = updatedSubs, served = updatedSubs.all { it.served })
                        } else {
                            if (dish.name == dishName) dish.copy(served = !dish.served) else dish
                        }
                    }
                    val allServed = updatedDishes.all { it.served }
                    val newStatus = if (allServed && order.status in listOf("待处理", "制作中")) "待结账" else order.status
                    order.copy(dishes = updatedDishes, status = newStatus)
                } else order
            }
            val changed = updatedOrders.firstOrNull { it.tableLabel == tableLabel && it.time == time }
            val original = state.orders.firstOrNull { it.tableLabel == tableLabel && it.time == time }
            if (changed != null && changed.id > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    // 同步上菜状态
                    runCatching { repository.pushDishServed(changed.id.toString(), dishName) }
                    // 同步订单状态变更
                    if (original != null && changed.status != original.status) {
                        runCatching { repository.pushOrderStatus(changed.id.toString(), changed.status) }
                    }
                }
            }
            state.copy(orders = updatedOrders)
        }
    }

    fun advanceOrderStatus(tableLabel: String, time: String) {
        val nextState = _uiState.value.let { state ->
            val updated = state.orders.map { order ->
                if (order.tableLabel == tableLabel && order.time == time) {
                    order.copy(status = nextOrderStatus(order.status))
                } else {
                    order
                }
            }
            state.copy(
                orders = updated,
                noticeMessage = "已更新 $tableLabel 的订单状态"
            )
        }
        _uiState.value = nextState
        val targetOrder = nextState.orders.firstOrNull { it.tableLabel == tableLabel && it.time == time } ?: return
        if (targetOrder.id > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repository.pushOrderStatus(targetOrder.id.toString(), targetOrder.status) }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "订单已本地更新，但同步后端失败：${it.message ?: "请稍后重试"}"
                        )
                    }
            }
        }
    }

    fun deleteTable(label: String) {
        val tableId = _uiState.value.tables.firstOrNull { it.label == label }?.id ?: 0
        _uiState.value = _uiState.value.copy(
            tables = _uiState.value.tables.filterNot { it.label == label },
            noticeMessage = "已删除 $label"
        )
        if (tableId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { repository.deleteTable(tableId.toString()) }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "桌台已本地删除，但同步后端失败：${it.message ?: "请稍后重试"}"
                        )
                    }
            }
        }
    }

    fun toggleTableStatus(label: String) {
        val nextState = _uiState.value.let { state ->
            val updated = state.tables.map { table ->
                if (table.label == label) {
                    table.copy(status = nextTableStatus(table.status))
                } else {
                    table
                }
            }
            state.copy(
                tables = updated,
                noticeMessage = "已更新 $label 的桌台状态"
            )
        }
        _uiState.value = nextState
        val targetTable = nextState.tables.firstOrNull { it.label == label } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.pushTableStatus(label, targetTable.status) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "桌台状态已本地更新，但同步后端失败：${it.message ?: "请稍后重试"}"
                    )
                }
        }
    }

    fun generateQrForDraft() {
        val draft = _uiState.value.qrDraft
        val tableNumber = draft.tableNumber.toIntOrNull()
        if (tableNumber == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "桌号格式不正确，请输入数字")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(refreshing = true, errorMessage = null, noticeMessage = null)
            runCatching {
                repository.generateTableQrCode(
                    TableQrRequest(
                        section = draft.section,
                        number = tableNumber,
                        target = draft.target,
                        baseUrl = draft.baseUrl
                    )
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    refreshing = false,
                    qrPreviewUrl = response.targetUrl,
                    qrFileId = response.fileId,
                    noticeMessage = "桌码已生成，可复制给顾客使用"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    refreshing = false,
                    errorMessage = error.message ?: "桌码生成失败，请稍后重试"
                )
            }
        }
    }

    fun applyDraftToTable() {
        val draft = _uiState.value.qrDraft
        val label = "${draft.tableNumber} 号桌"
        val area = MerchantUiTextMapper.localizeArea(draft.section)
        val qrTarget = MerchantUiTextMapper.localizeQrTarget(draft.target)
        val customerLink = if (draft.target == "h5") buildQrPreview(draft) else ""
        val nextTable = TableItem(
            label = label,
            area = area,
            status = "空闲",
            qrTarget = qrTarget,
            customerLink = customerLink
        )

        _uiState.value = _uiState.value.let { state ->
            val existing = state.tables.any { it.label == label }
            val nextTables = if (existing) {
                state.tables.map { table -> if (table.label == label) nextTable else table }
            } else {
                state.tables + nextTable
            }
            state.copy(
                tables = nextTables,
                noticeMessage = if (existing) "已更新 $label 的桌码信息" else "已新增 $label"
            )
        }
    }

    fun updatePointsConfig(earnRate: Int? = null, deductRate: Int? = null) {
        val current = _uiState.value.pointsConfig
        _uiState.value = _uiState.value.copy(
            pointsConfig = current.copy(
                earnRate = earnRate ?: current.earnRate,
                deductRate = deductRate ?: current.deductRate
            ),
            noticeMessage = "积分规则已更新"
        )
    }

    fun queryRevenue(start: String, end: String, onResult: (Double, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.getRevenue(start, end) }.getOrElse { Pair(0.0, 0) }
            withContext(Dispatchers.Main) { onResult(result.first, result.second) }
        }
    }

    fun queryOrdersByDate(start: String, end: String, onResult: (List<OrderItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.getOrdersByDate(start, end) }.getOrElse { emptyList() }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun queryPoints(phone: String, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val httpClient = MerchantHttpClient()
                when (val res = httpClient.get("/merchant/points?phone=$phone")) {
                    is MerchantApiResult.Success -> {
                        val json = org.json.JSONObject(res.data)
                        val data = json.optJSONObject("data")
                        val pts = data?.optInt("points", 0) ?: 0
                        "手机号 $phone 当前积分：$pts"
                    }
                    is MerchantApiResult.Error -> "查询失败：${res.message}"
                }
            }.getOrElse { "查询失败：${it.message}" }
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun addPointsLog(target: String, delta: Int, reason: String) {
        if (target.isBlank() || delta == 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "请填写对象和积分数量")
            return
        }
        val log = PointsLog(
            target = target.trim(),
            delta = delta,
            reason = reason.ifBlank { if (delta > 0) "商家赠送" else "商家扣减" },
            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
        _uiState.value = _uiState.value.copy(
            pointsLogs = listOf(log) + _uiState.value.pointsLogs,
            noticeMessage = "已${if (delta > 0) "增加" else "扣减"} ${target.trim()} ${kotlin.math.abs(delta)} 积分"
        )
    }

    fun updateProfile(shopName: String? = null, h5BaseUrl: String? = null) {
        val current = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            profile = current.copy(
                shopName = shopName ?: current.shopName,
                h5BaseUrl = h5BaseUrl ?: current.h5BaseUrl
            ),
            noticeMessage = "个人信息已更新"
        )
    }

    private fun buildQrPreview(draft: TableQrDraft): String {
        if (draft.target != "h5") {
            return """{"t":"table","s":"${draft.section}","n":"${draft.tableNumber}"}"""
        }

        return SingleTenantMerchantConfig.buildCustomerOrderUrl(
            section = draft.section,
            tableNumber = draft.tableNumber,
            baseUrl = draft.baseUrl
        )
    }

    private fun fetchMerchantData(initialLoad: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                loading = initialLoad,
                refreshing = !initialLoad,
                errorMessage = null
            )

            runCatching {
                withContext(Dispatchers.IO) {
                val dashboard = async { repository.getDashboardStats() }
                val entries = async { repository.getQuickEntries() }
                val dishesDeferred = async { repository.getDishes() }
                val orders = async { repository.getOrders() }
                val tables = async { repository.getTables() }
                val dishes = dishesDeferred.await()

                MerchantUiState(
                    dashboardStats = dashboard.await(),
                    quickEntries = entries.await(),
                    categories = mergeCategories(dishes, currentState.categories),
                    dishes = dishes,
                    orders = orders.await(),
                    tables = tables.await(),
                    dishDraft = currentState.dishDraft.copy(
                        category = currentState.dishDraft.category.takeIf { it in mergeCategories(dishes, currentState.categories) }
                            ?: mergeCategories(dishes, currentState.categories).first()
                    ),
                    categoryDraft = currentState.categoryDraft,
                    qrDraft = currentState.qrDraft,
                    qrPreviewUrl = buildQrPreview(currentState.qrDraft),
                    loading = false,
                    refreshing = false,
                    qrFileId = currentState.qrFileId,
                    pointsConfig = currentState.pointsConfig,
                    pointsLogs = currentState.pointsLogs,
                    profile = currentState.profile
                )
                }
            }.onSuccess { nextState ->
                val allEmpty = nextState.dashboardStats.isEmpty() && nextState.dishes.isEmpty()
                        && nextState.orders.isEmpty() && nextState.tables.isEmpty()
                _uiState.value = if (allEmpty) {
                    nextState.copy(errorMessage = "服务器返回数据为空，请检查网络")
                } else nextState
            }.onFailure { error ->
                _uiState.value = currentState.copy(
                    loading = false,
                    refreshing = false,
                    errorMessage = "加载失败: ${error.message}",
                    qrPreviewUrl = buildQrPreview(currentState.qrDraft)
                )
            }
        }
    }

    private fun nextOrderStatus(status: String): String {
        return when (status) {
            "待处理" -> "制作中"
            "制作中" -> "待结账"
            "待结账" -> "已完成"
            else -> "待处理"
        }
    }

    private fun nextTableStatus(status: String): String {
        return when (status) {
            "空闲" -> "使用中"
            "使用中" -> "待结账"
            "待结账" -> "空闲"
            else -> "空闲"
        }
    }

    private fun mergeCategories(dishes: List<DishItem>, existing: List<String>): List<String> {
        val defaults = listOf("烧烤", "蔬菜", "饮品", "套餐", "未分类")
        return (defaults + existing + dishes.map { it.category.ifBlank { "未分类" } })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
