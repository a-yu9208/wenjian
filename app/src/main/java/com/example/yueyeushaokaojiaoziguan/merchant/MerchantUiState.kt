package com.example.yueyeushaokaojiaoziguan.merchant

data class MerchantUiState(
    val dashboardStats: List<StatCard> = emptyList(),
    val quickEntries: List<QuickEntry> = emptyList(),
    val categories: List<String> = listOf("烧烤", "蔬菜", "饮品", "套餐", "未分类"),
    val dishes: List<DishItem> = emptyList(),
    val orders: List<OrderItem> = emptyList(),
    val tables: List<TableItem> = emptyList(),
    val dishDraft: DishDraft = DishDraft(),
    val categoryDraft: String = "",
    val qrDraft: TableQrDraft = TableQrDraft(),
    val qrPreviewUrl: String = "",
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val qrFileId: String = "",
    val dishManageMode: Boolean = false,
    val showAddDishDialog: Boolean = false,
    val selectedDishes: Set<String> = emptySet(),
    val pointsConfig: PointsConfig = PointsConfig(),
    val pointsLogs: List<PointsLog> = emptyList(),
    val pointsUsers: List<PointsUser> = emptyList(),
    val checkoutAlert: String? = null,
    val profile: MerchantProfile = MerchantProfile()
)
