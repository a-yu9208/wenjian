package com.example.yueyeushaokaojiaoziguan.merchant

class MockMerchantRepository : MerchantRepository {
    override suspend fun getDashboardStats(): List<StatCard> = MerchantSampleData.dashboardStats

    override suspend fun getQuickEntries(): List<QuickEntry> = MerchantSampleData.quickEntries

    override suspend fun getDishes(): List<DishItem> = MerchantSampleData.dishes

    override suspend fun getOrders(): List<OrderItem> = MerchantSampleData.orders

    override suspend fun getTables(): List<TableItem> = MerchantSampleData.tables

    override suspend fun generateTableQrCode(request: TableQrRequest): TableQrResponse {
        val targetUrl = if (request.target == "h5") {
            SingleTenantMerchantConfig.buildCustomerOrderUrl(
                section = request.section,
                tableNumber = request.number.toString(),
                baseUrl = request.baseUrl
            )
        } else {
            """{"t":"table","s":"${request.section}","n":"${request.number}"}"""
        }

        return TableQrResponse(
            fileId = "mock-file-id",
            targetUrl = targetUrl
        )
    }

    override suspend fun pushOrderStatus(tableLabel: String, status: String) = Unit

    override suspend fun pushDishStock(name: String, stock: Int) = Unit

    override suspend fun createDish(dish: DishItem) = Unit
    override suspend fun deleteDishes(names: Set<String>) = Unit
    override suspend fun pushTableStatus(label: String, status: String) = Unit
    override suspend fun deleteTable(label: String) = Unit
    override suspend fun updateDish(dish: DishItem) = Unit
    override suspend fun batchUpdateCategory(names: Set<String>, category: String) = Unit
}
