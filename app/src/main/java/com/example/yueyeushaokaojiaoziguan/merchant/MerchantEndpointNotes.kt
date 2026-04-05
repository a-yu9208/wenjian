package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantEndpointNotes {
    val endpoints = listOf(
        "GET ${MerchantApiConfig.dashboardPath}",
        "GET ${MerchantApiConfig.dishesPath}",
        "GET ${MerchantApiConfig.ordersPath}",
        "GET ${MerchantApiConfig.tablesPath}",
        "POST ${MerchantApiConfig.tableQrPath}",
        "POST ${MerchantApiConfig.orderActionsPath}",
        "POST /merchant/dishes (创建菜品)",
        "POST /merchant/dishes/stock (更新库存)",
        "POST /merchant/dishes/delete (批量删除)",
        "POST /merchant/table-status (桌台状态)"
    )
}
