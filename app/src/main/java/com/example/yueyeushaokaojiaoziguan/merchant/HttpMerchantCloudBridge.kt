package com.example.yueyeushaokaojiaoziguan.merchant

class HttpMerchantCloudBridge(
    private val baseUrl: String = MerchantApiConfig.baseApiUrl
) : MerchantCloudBridge {
    private val httpClient = MerchantHttpClient(baseUrl)

    override suspend fun fetchDashboardStats(): List<StatCard> {
        return when (val result = httpClient.get(MerchantApiConfig.dashboardPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseDashboardStats(result.data)
            }
            is MerchantApiResult.Error -> throw Exception("仪表盘加载失败: ${result.message}")
        }
    }

    override suspend fun fetchDishes(): List<DishItem> {
        return when (val result = httpClient.get(MerchantApiConfig.dishesPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseDishes(result.data)
            }
            is MerchantApiResult.Error -> throw Exception("菜品加载失败: ${result.message}")
        }
    }

    override suspend fun fetchOrders(): List<OrderItem> {
        return when (val result = httpClient.get(MerchantApiConfig.ordersPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseOrders(result.data)
            }
            is MerchantApiResult.Error -> throw Exception("订单加载失败: ${result.message}")
        }
    }

    override suspend fun fetchTables(): List<TableItem> {
        return when (val result = httpClient.get(MerchantApiConfig.tablesPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseTables(result.data)
            }
            is MerchantApiResult.Error -> throw Exception("桌台加载失败: ${result.message}")
        }
    }

    override suspend fun generateTableQrCode(request: TableQrRequest): TableQrResponse {
        val requestBody = """
            {
              "section": "${request.section}",
              "number": ${request.number},
              "target": "${request.target}",
              "baseUrl": "${request.baseUrl}"
            }
        """.trimIndent()
        val response = httpClient.post(MerchantApiConfig.tableQrPath, requestBody)
        val targetUrl = if (request.target == "h5") {
            SingleTenantMerchantConfig.buildCustomerOrderUrl(
                section = request.section,
                tableNumber = request.number.toString(),
                baseUrl = request.baseUrl
            )
        } else {
            """{"t":"table","s":"${request.section}","n":"${request.number}"}"""
        }

        return when (response) {
            is MerchantApiResult.Success -> MerchantJsonParsers.parseTableQrResponse(response.data)
                ?: TableQrResponse(
                    fileId = "http-placeholder-file-id",
                    targetUrl = targetUrl
                )
            is MerchantApiResult.Error -> TableQrResponse(
                fileId = "http-placeholder-file-id",
                targetUrl = targetUrl
            )
        }
    }

    override suspend fun pushOrderStatus(tableLabel: String, status: String) {
        val statusMap = mapOf("待处理" to "Pending", "制作中" to "Grilling", "待结账" to "Checkout", "已完成" to "Completed")
        val enStatus = statusMap[status] ?: status
        val orderId = tableLabel.toIntOrNull()
        val requestBody = if (orderId != null) {
            """{"action":"updateStatus","orderId":$orderId,"status":"$enStatus"}"""
        } else {
            """{"action":"updateOrderStatus","tableLabel":"$tableLabel","status":"$enStatus"}"""
        }
        httpClient.post(MerchantApiConfig.orderActionsPath, requestBody)
    }

    override suspend fun pushDishStock(name: String, stock: Int) {
        val requestBody = """
            {
              "action": "updateStock",
              "name": "$name",
              "stock": $stock
            }
        """.trimIndent()
        httpClient.post(MerchantApiConfig.dishActionsPath, requestBody)
    }

    override suspend fun createDish(dish: DishItem) {
        val requestBody = """
            {
              "action": "createDish",
              "name": "${dish.name}",
              "category": "${dish.category}",
              "price": "${dish.price}",
              "stock": ${dish.stock},
              "type": "${dish.type}"
            }
        """.trimIndent()
        httpClient.post(MerchantApiConfig.dishActionsPath, requestBody)
    }

    override suspend fun pushTableStatus(label: String, status: String) {
        val requestBody = """
            {
              "action": "updateTableStatus",
              "label": "$label",
              "status": "$status"
            }
        """.trimIndent()
        httpClient.post(MerchantApiConfig.orderActionsPath, requestBody)
    }
}
