package com.example.yueyeushaokaojiaoziguan.merchant

enum class MerchantTab(val label: String, val shortLabel: String) {
    Home("首页", "首"),
    Dishes("菜品", "菜"),
    Orders("订单", "单"),
    Tables("桌台", "桌")
}

data class StatCard(
    val title: String,
    val value: String,
    val note: String
)

data class QuickEntry(
    val title: String,
    val description: String
)

data class DishItem(
    val name: String,
    val category: String,
    val price: String,
    val stock: Int,
    val soldToday: Int,
    val type: String
)

data class OrderItem(
    val tableLabel: String,
    val summary: String,
    val amount: String,
    val status: String,
    val time: String
)

data class TableItem(
    val label: String,
    val area: String,
    val status: String,
    val qrTarget: String,
    val customerLink: String
)

data class TableQrDraft(
    val section: String = "outside",
    val tableNumber: String = "8",
    val target: String = "h5",
    val baseUrl: String = "https://order.example.com"
)

data class DishDraft(
    val name: String = "",
    val category: String = "烧烤",
    val price: String = "",
    val stock: String = "10",
    val type: String = "单品"
)
