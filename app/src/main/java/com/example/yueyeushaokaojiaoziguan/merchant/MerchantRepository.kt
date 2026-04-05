package com.example.yueyeushaokaojiaoziguan.merchant

interface MerchantRepository {
    suspend fun getDashboardStats(): List<StatCard>
    suspend fun getQuickEntries(): List<QuickEntry>
    suspend fun getDishes(): List<DishItem>
    suspend fun getOrders(): List<OrderItem>
    suspend fun getTables(): List<TableItem>
    suspend fun generateTableQrCode(request: TableQrRequest): TableQrResponse
    suspend fun pushOrderStatus(tableLabel: String, status: String)
    suspend fun pushDishStock(name: String, stock: Int)
    suspend fun createDish(dish: DishItem)
    suspend fun deleteDishes(names: Set<String>)
    suspend fun pushTableStatus(label: String, status: String)
}
