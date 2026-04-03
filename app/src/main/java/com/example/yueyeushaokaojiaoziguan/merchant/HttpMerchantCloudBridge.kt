package com.example.yueyeushaokaojiaoziguan.merchant

import java.io.BufferedReader

class HttpMerchantCloudBridge(
    private val baseUrl: String = MerchantApiConfig.baseApiUrl
) : MerchantCloudBridge {
    private val httpClient = MerchantHttpClient(baseUrl)

    override suspend fun fetchDashboardStats(): List<StatCard> {
        return when (val result = httpClient.get(MerchantApiConfig.dashboardPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseDashboardStats(result.data).ifEmpty {
                    MerchantSampleData.dashboardStats
                }
            }
            is MerchantApiResult.Error -> MerchantSampleData.dashboardStats
        }
    }

    override suspend fun fetchDishes(): List<DishItem> {
        return when (val result = httpClient.get(MerchantApiConfig.dishesPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseDishes(result.data).ifEmpty {
                    MerchantSampleData.dishes
                }
            }
            is MerchantApiResult.Error -> MerchantSampleData.dishes
        }
    }

    override suspend fun fetchOrders(): List<OrderItem> {
        return when (val result = httpClient.get(MerchantApiConfig.ordersPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseOrders(result.data).ifEmpty {
                    MerchantSampleData.orders
                }
            }
            is MerchantApiResult.Error -> MerchantSampleData.orders
        }
    }

    override suspend fun fetchTables(): List<TableItem> {
        return when (val result = httpClient.get(MerchantApiConfig.tablesPath)) {
            is MerchantApiResult.Success -> {
                MerchantJsonParsers.parseTables(result.data).ifEmpty {
                    MerchantSampleData.tables
                }
            }
            is MerchantApiResult.Error -> MerchantSampleData.tables
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
        httpClient.post(MerchantApiConfig.tableQrPath, requestBody)
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
            fileId = "http-placeholder-file-id",
            targetUrl = targetUrl
        )
    }
}
