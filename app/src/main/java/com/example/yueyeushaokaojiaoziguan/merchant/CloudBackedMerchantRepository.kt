package com.example.yueyeushaokaojiaoziguan.merchant

class CloudBackedMerchantRepository(
    private val bridge: MerchantCloudBridge
) : MerchantRepository {
    override suspend fun getDashboardStats(): List<StatCard> = bridge.fetchDashboardStats()

    override suspend fun getQuickEntries(): List<QuickEntry> = MerchantSampleData.quickEntries

    override suspend fun getDishes(): List<DishItem> = bridge.fetchDishes().map { dish ->
        dish.copy(
            category = MerchantUiTextMapper.localizeDishCategory(dish.category),
            type = MerchantUiTextMapper.localizeDishType(dish.type)
        )
    }

    override suspend fun getOrders(): List<OrderItem> = bridge.fetchOrders().map { order ->
        order.copy(status = MerchantUiTextMapper.localizeOrderStatus(order.status))
    }

    override suspend fun getTables(): List<TableItem> = bridge.fetchTables().map { table ->
        table.copy(
            area = MerchantUiTextMapper.localizeArea(table.area),
            status = MerchantUiTextMapper.localizeTableStatus(table.status),
            qrTarget = MerchantUiTextMapper.localizeQrTarget(table.qrTarget)
        )
    }

    override suspend fun generateTableQrCode(request: TableQrRequest): TableQrResponse {
        return bridge.generateTableQrCode(request)
    }

    override suspend fun pushOrderStatus(tableLabel: String, status: String) {
        bridge.pushOrderStatus(tableLabel, status)
    }

    override suspend fun pushDishStock(name: String, stock: Int) {
        bridge.pushDishStock(name, stock)
    }

    override suspend fun createDish(dish: DishItem) {
        bridge.createDish(dish)
    }

    override suspend fun deleteDishes(names: Set<String>) {
        bridge.deleteDishes(names)
    }

    override suspend fun pushTableStatus(label: String, status: String) {
        bridge.pushTableStatus(label, status)
    }

    override suspend fun deleteTable(label: String) {
        bridge.deleteTable(label)
    }

    override suspend fun updateDish(dish: DishItem) {
        bridge.updateDish(dish)
    }

    override suspend fun batchUpdateCategory(names: Set<String>, category: String) {
        bridge.batchUpdateCategory(names, category)
    }
}
