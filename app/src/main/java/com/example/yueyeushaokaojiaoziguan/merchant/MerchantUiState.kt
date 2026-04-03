package com.example.yueyeushaokaojiaoziguan.merchant

data class MerchantUiState(
    val dashboardStats: List<StatCard> = emptyList(),
    val quickEntries: List<QuickEntry> = emptyList(),
    val dishes: List<DishItem> = emptyList(),
    val orders: List<OrderItem> = emptyList(),
    val tables: List<TableItem> = emptyList(),
    val qrDraft: TableQrDraft = TableQrDraft(),
    val qrPreviewUrl: String = "",
    val loading: Boolean = true
)
