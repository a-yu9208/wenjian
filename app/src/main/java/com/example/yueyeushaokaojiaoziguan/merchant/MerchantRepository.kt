package com.example.yueyeushaokaojiaoziguan.merchant

interface MerchantRepository {
    fun getDashboardStats(): List<StatCard>
    fun getQuickEntries(): List<QuickEntry>
    fun getDishes(): List<DishItem>
    fun getOrders(): List<OrderItem>
    fun getTables(): List<TableItem>
}
