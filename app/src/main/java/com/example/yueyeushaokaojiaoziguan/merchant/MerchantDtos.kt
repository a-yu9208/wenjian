package com.example.yueyeushaokaojiaoziguan.merchant

data class DashboardResponse(
    val stats: List<StatCard> = emptyList()
)

data class DishesResponse(
    val dishes: List<DishItem> = emptyList()
)

data class OrdersResponse(
    val orders: List<OrderItem> = emptyList()
)

data class TablesResponse(
    val tables: List<TableItem> = emptyList()
)
