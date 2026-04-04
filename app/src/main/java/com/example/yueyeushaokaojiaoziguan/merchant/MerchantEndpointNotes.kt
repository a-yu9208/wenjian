package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantEndpointNotes {
    val endpoints = listOf(
        "GET ${MerchantApiConfig.dashboardPath}",
        "GET ${MerchantApiConfig.dishesPath}",
        "GET ${MerchantApiConfig.ordersPath}",
        "GET ${MerchantApiConfig.tablesPath}",
        "POST ${MerchantApiConfig.tableQrPath}",
        "POST ${MerchantApiConfig.orderActionsPath}",
        "POST ${MerchantApiConfig.dishActionsPath}"
    )
}
