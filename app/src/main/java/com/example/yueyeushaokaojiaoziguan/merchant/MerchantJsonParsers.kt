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
                        type = item.optString("type"),
                        quickServe = item.optBoolean("quickServe", false),
                        description = item.optString("description"),
                        minOrder = item.optInt("minOrder", 1)
                    )
                )
            }
        }
    }

    fun parseOrders(json: String): List<OrderItem> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val ordersArray = root.optJSONObject("data")?.optJSONArray("orders")
            ?: root.optJSONArray("orders")
            ?: JSONArray()

        return buildList {
            for (index in 0 until ordersArray.length()) {
                val item = ordersArray.optJSONObject(index) ?: continue
                var dishes = parseOrderDishes(item.optJSONArray("dishes"))
                // 后端可能不返回 dishes，从 summary 解析
                if (dishes.isEmpty()) {
                    dishes = parseSummaryToDishes(item.optString("summary"))
                }
                add(
                    OrderItem(
                        id = item.optInt("id"),
                        tableLabel = item.optString("tableLabel"),
                        summary = item.optString("summary"),
                        amount = item.optString("amount"),
                        status = item.optString("status"),
                        time = item.optString("time"),
                        area = item.optString("area"),
                        isAppend = item.optBoolean("isAppend", false),
                        appendIndex = item.optInt("appendIndex", 0),
                        dishes = dishes,
                        utensilCount = item.optInt("utensilCount", 0)
                    )
                )
            }
        }
    }

    /** 从 "花生米 x1、酸梅汤 x2" 格式的 summary 解析出菜品列表 */
    private fun parseSummaryToDishes(summary: String): List<OrderDishItem> {
        if (summary.isBlank()) return emptyList()
        return summary.split("、").mapNotNull { part ->
            val match = Regex("(.+?)\\s*[xX×]\\s*(\\d+)").find(part.trim()) ?: return@mapNotNull null
            OrderDishItem(
                name = match.groupValues[1].trim(),
                quantity = match.groupValues[2].toIntOrNull() ?: 1
            )
        }
    }

    private fun parseOrderDishes(arr: JSONArray?): List<OrderDishItem> {
        if (arr == null || arr.length() == 0) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                add(
                    OrderDishItem(
                        name = d.optString("name"),
                        quantity = d.optInt("quantity", d.optInt("qty", 1)),
                        note = d.optString("note"),
                        served = d.optBoolean("served", false),
                        quickServe = d.optBoolean("quickServe", false),
                        subItems = parseOrderDishes(d.optJSONArray("subItems"))
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
                        id = item.optInt("id"),
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
