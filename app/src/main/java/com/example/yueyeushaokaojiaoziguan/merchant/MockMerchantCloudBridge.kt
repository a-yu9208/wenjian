package com.example.yueyeushaokaojiaoziguan.merchant

class MockMerchantCloudBridge : MerchantCloudBridge {
    override suspend fun fetchDashboardStats(): List<StatCard> = MerchantSampleData.dashboardStats

    override suspend fun fetchDishes(): List<DishItem> = MerchantSampleData.dishes

    override suspend fun fetchOrders(): List<OrderItem> = MerchantSampleData.orders

    override suspend fun fetchTables(): List<TableItem> = MerchantSampleData.tables

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
}
