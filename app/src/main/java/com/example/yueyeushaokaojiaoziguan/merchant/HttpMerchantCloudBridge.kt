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
        val comboJson = if (dish.comboItems.isNotEmpty()) {
            dish.comboItems.joinToString(",", prefix = "[", postfix = "]") {
                """{"name":"${it.name}","quantity":${it.quantity}}"""
            }
        } else "[]"
        val requestBody = """{"name":"${dish.name}","category":"${dish.category}","price":$price,"stock":${dish.stock},"type":"${dish.type}","description":"${dish.description}","minOrder":${dish.minOrder},"quickServe":${dish.quickServe},"comboItems":$comboJson,"imageUrl":"${dish.imageUri}"}"""
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
        val requestBody = """{"name":"${dish.name}","category":"${dish.category}","price":$price,"stock":${dish.stock},"type":"${dish.type}","description":"${dish.description}","minOrder":${dish.minOrder},"quickServe":${dish.quickServe},"imageUrl":"${dish.imageUri}"}"""
        httpClient.post("/merchant/dishes/update", requestBody)
    }

    override suspend fun batchUpdateCategory(names: Set<String>, category: String) {
        val namesJson = names.joinToString(",") { "\"$it\"" }
        httpClient.post("/merchant/dishes/batch-category", """{"names":[$namesJson],"category":"$category"}""")
    }

    override suspend fun pushDishServed(orderId: String, dishName: String) {
        httpClient.post(MerchantApiConfig.orderActionsPath, """{"action":"toggleServed","orderId":$orderId,"dishName":"$dishName"}""")
    }

    override suspend fun getRevenue(start: String, end: String): Pair<Double, Int> {
        return when (val res = httpClient.get("/merchant/revenue?start=$start&end=$end")) {
            is MerchantApiResult.Success -> {
                val json = org.json.JSONObject(res.data)
                val data = json.optJSONObject("data")
                Pair(data?.optDouble("revenue", 0.0) ?: 0.0, data?.optInt("orderCount", 0) ?: 0)
            }
            is MerchantApiResult.Error -> Pair(0.0, 0)
        }
    }

    override suspend fun getOrdersByDate(start: String, end: String): List<OrderItem> {
        return when (val res = httpClient.get("${MerchantApiConfig.ordersPath}?start=$start&end=$end")) {
            is MerchantApiResult.Success -> MerchantJsonParsers.parseOrders(res.data)
            is MerchantApiResult.Error -> emptyList()
        }
    }

    fun uploadImage(imageBytes: ByteArray, fileName: String): String? {
        val boundary = "----FormBoundary${System.currentTimeMillis()}"
        val url = java.net.URL("${baseUrl.trimEnd('/')}/merchant/upload")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        conn.outputStream.use { os ->
            val writer = os.bufferedWriter()
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            writer.write("Content-Type: image/jpeg\r\n\r\n")
            writer.flush()
            os.write(imageBytes)
            os.flush()
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
        conn.disconnect()
        if (code !in 200..299) return null
        return try {
            val json = org.json.JSONObject(text)
            json.optJSONObject("data")?.optString("url")
        } catch (_: Exception) { null }
    }
}
