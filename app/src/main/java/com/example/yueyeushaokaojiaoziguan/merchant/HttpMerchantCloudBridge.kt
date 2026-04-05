package com.example.yueyeushaokaojiaoziguan.merchant

class HttpMerchantCloudBridge(
    private val baseUrl: String = MerchantApiConfig.baseApiUrl
) : MerchantCloudBridge {
    private val httpClient = MerchantHttpClient(baseUrl)

    override suspend fun fetchDashboardStats(): List<StatCard> {
        return when (val result = httpClient.get(MerchantApiConfig.dashboardPath)) {
            is MerchantApiResult.Success -> {
                val parsed = MerchantJsonParsers.parseDashboardStats(result.data)
                if (parsed.isEmpty()) {
                    android.util.Log.e("SHAOKAO", "dashboard raw: ${result.data.take(200)}")
                }
                parsed
            }
            is MerchantApiResult.Error -> {
                android.util.Log.e("SHAOKAO", "dashboard error: ${result.message}")
                emptyList()
            }
        }
    }

    override suspend fun fetchDishes(): List<DishItem> {
        return when (val result = httpClient.get(MerchantApiConfig.dishesPath)) {
            is MerchantApiResult.Success -> {
                val parsed = MerchantJsonParsers.parseDishes(result.data)
                if (parsed.isEmpty()) {
                    android.util.Log.e("SHAOKAO", "dishes raw: ${result.data.take(200)}")
                }
                parsed
            }
            is MerchantApiResult.Error -> {
                android.util.Log.e("SHAOKAO", "dishes error: ${result.message}")
                emptyList()
            }
        }
    }

    override suspend fun fetchOrders(): List<OrderItem> {
        return when (val result = httpClient.get(MerchantApiConfig.ordersPath)) {
            is MerchantApiResult.Success -> {
                val parsed = MerchantJsonParsers.parseOrders(result.data)
                if (parsed.isEmpty()) {
                    android.util.Log.e("SHAOKAO", "orders raw: ${result.data.take(200)}")
                }
                parsed
            }
            is MerchantApiResult.Error -> {
                android.util.Log.e("SHAOKAO", "orders error: ${result.message}")
                emptyList()
            }
        }
    }

    override suspend fun fetchTables(): List<TableItem> {
        return when (val result = httpClient.get(MerchantApiConfig.tablesPath)) {
            is MerchantApiResult.Success -> {
                val parsed = MerchantJsonParsers.parseTables(result.data)
                if (parsed.isEmpty()) {
                    android.util.Log.e("SHAOKAO", "tables raw: ${result.data.take(200)}")
                }
                parsed
            }
            is MerchantApiResult.Error -> {
                android.util.Log.e("SHAOKAO", "tables error: ${result.message}")
                emptyList()
            }
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
        val requestBody = """{"action":"updateStatus","orderId":${orderId ?: 0},"status":"$enStatus"}"""
        httpClient.post(MerchantApiConfig.orderActionsPath, requestBody)
    }

    override suspend fun pushDishStock(name: String, stock: Int) {
        val requestBody = """{"name":"$name","stock":$stock}"""
        httpClient.post("/merchant/dishes/stock", requestBody)
    }

    override suspend fun createDish(dish: DishItem) {
        val price = dish.price.replace("¥", "").toDoubleOrNull() ?: 0.0
        val requestBody = """{"name":"${dish.name}","category":"${dish.category}","price":$price,"stock":${dish.stock},"type":"${dish.type}","description":"${dish.description}","minOrder":${dish.minOrder},"quickServe":${dish.quickServe}}"""
        httpClient.post("/merchant/dishes", requestBody)
    }

    override suspend fun deleteDishes(names: Set<String>) {
        val namesJson = names.joinToString(",") { "\"$it\"" }
        httpClient.post("/merchant/dishes/delete", """{"names":[$namesJson]}""")
    }

    override suspend fun pushTableStatus(label: String, status: String) {
        val requestBody = """{"label":"$label","status":"$status"}"""
        httpClient.post("/merchant/table-status", requestBody)
    }

    override suspend fun deleteTable(label: String) {
        httpClient.delete("/merchant/tables/$label")
    }

    override suspend fun updateDish(dish: DishItem) {
        val price = dish.price.replace("¥", "").toDoubleOrNull() ?: 0.0
        val requestBody = """{"name":"${dish.name}","category":"${dish.category}","price":$price,"stock":${dish.stock},"type":"${dish.type}","description":"${dish.description}","minOrder":${dish.minOrder},"quickServe":${dish.quickServe}}"""
        httpClient.post("/merchant/dishes/update", requestBody)
    }

    override suspend fun batchUpdateCategory(names: Set<String>, category: String) {
        val namesJson = names.joinToString(",") { "\"$it\"" }
        httpClient.post("/merchant/dishes/batch-category", """{"names":[$namesJson],"category":"$category"}""")
    }
}
