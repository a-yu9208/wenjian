package com.example.yueyeushaokaojiaoziguan.merchant

data class TableQrRequest(
    val section: String,
    val number: Int,
    val target: String,
    val baseUrl: String = ""
)

data class TableQrResponse(
    val fileId: String,
    val targetUrl: String
)

interface MerchantCloudBridge {
    suspend fun fetchDashboardStats(): List<StatCard>
    suspend fun fetchDishes(): List<DishItem>
    suspend fun fetchOrders(): List<OrderItem>
    suspend fun fetchTables(): List<TableItem>
    suspend fun generateTableQrCode(request: TableQrRequest): TableQrResponse
    suspend fun pushOrderStatus(tableLabel: String, status: String)
    suspend fun pushDishStock(name: String, stock: Int)
    suspend fun createDish(dish: DishItem)
    suspend fun deleteDishes(names: Set<String>)
    suspend fun pushTableStatus(label: String, status: String)
}
