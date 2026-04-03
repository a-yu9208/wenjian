package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantIntegrationConfig {
    const val cloudEnvId = "cloud1-1gn7fwfsa4552d34"

    val collections = listOf(
        "orders",
        "tables",
        "dishes",
        "categories",
        "users",
        "settings",
        "points_logs"
    )

    val cloudFunctions = listOf(
        "login",
        "createOrder",
        "manageOrder",
        "checkout",
        "generateTableQRCode",
        "getPhoneNumber"
    )
}
