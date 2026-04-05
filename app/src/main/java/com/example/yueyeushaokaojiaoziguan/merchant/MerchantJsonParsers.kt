package com.example.yueyeushaokaojiaoziguan.merchant

import org.json.JSONArray
import org.json.JSONObject

object MerchantJsonParsers {
    fun parseDashboardStats(json: String): List<StatCard> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val statsArray = root.optJSONObject("data")?.optJSONArray("stats")
            ?: root.optJSONArray("stats")
            ?: JSONArray()

        return buildList {
            for (index in 0 until statsArray.length()) {
                val item = statsArray.optJSONObject(index) ?: continue
                add(
                    StatCard(
                        title = item.optString("title"),
                        value = item.optString("value"),
                        note = item.optString("note")
                    )
                )
            }
        }
    }

    fun parseDishes(json: String): List<DishItem> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val dishesArray = root.optJSONObject("data")?.optJSONArray("dishes")
            ?: root.optJSONArray("dishes")
            ?: JSONArray()

        return buildList {
            for (index in 0 until dishesArray.length()) {
                val item = dishesArray.optJSONObject(index) ?: continue
                add(
                    DishItem(
                        name = item.optString("name"),
                        category = item.optString("category"),
                        price = item.optString("price"),
                        stock = item.optInt("stock"),
                        soldToday = item.optInt("soldToday"),
                        type = item.optString("type")
                    )
                )
            }
        }
    }

    private val statusToCn = mapOf("Pending" to "待处理", "Grilling" to "制作中", "Checkout" to "待结账", "Completed" to "已完成")
    private val statusToEn = mapOf("待处理" to "Pending", "制作中" to "Grilling", "待结账" to "Checkout", "已完成" to "Completed")

    fun parseOrders(json: String): List<OrderItem> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val ordersArray = root.optJSONObject("data")?.optJSONArray("orders")
            ?: root.optJSONArray("orders")
            ?: JSONArray()

        return buildList {
            for (index in 0 until ordersArray.length()) {
                val item = ordersArray.optJSONObject(index) ?: continue
                add(
                    OrderItem(
                        id = item.optInt("id"),
                        tableLabel = item.optString("tableLabel"),
                        summary = item.optString("summary"),
                        amount = item.optString("amount"),
                        status = statusToCn[item.optString("status")] ?: item.optString("status"),
                        time = item.optString("time")
                    )
                )
            }
        }
    }

    fun parseTables(json: String): List<TableItem> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val tablesArray = root.optJSONObject("data")?.optJSONArray("tables")
            ?: root.optJSONArray("tables")
            ?: JSONArray()

        return buildList {
            for (index in 0 until tablesArray.length()) {
                val item = tablesArray.optJSONObject(index) ?: continue
                add(
                    TableItem(
                        label = item.optString("label"),
                        area = item.optString("area"),
                        status = item.optString("status"),
                        qrTarget = item.optString("qrTarget"),
                        customerLink = item.optString("customerLink")
                    )
                )
            }
        }
    }

    fun parseTableQrResponse(json: String): TableQrResponse? {
        if (json.isBlank()) return null
        val root = JSONObject(json)
        val data = root.optJSONObject("data") ?: root
        val fileId = data.optString("fileId")
        val targetUrl = data.optString("targetUrl")
        if (fileId.isBlank() && targetUrl.isBlank()) return null
        return TableQrResponse(
            fileId = fileId,
            targetUrl = targetUrl
        )
    }
}
