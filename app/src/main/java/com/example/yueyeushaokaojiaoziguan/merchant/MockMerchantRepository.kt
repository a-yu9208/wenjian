package com.example.yueyeushaokaojiaoziguan.merchant

class MockMerchantRepository : MerchantRepository {
    override fun getDashboardStats(): List<StatCard> = MerchantSampleData.dashboardStats

    override fun getQuickEntries(): List<QuickEntry> = MerchantSampleData.quickEntries

    override fun getDishes(): List<DishItem> = MerchantSampleData.dishes

    override fun getOrders(): List<OrderItem> = MerchantSampleData.orders

    override fun getTables(): List<TableItem> = MerchantSampleData.tables
}
