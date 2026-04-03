package com.example.yueyeushaokaojiaoziguan.merchant

object SingleTenantMerchantConfig {
    const val merchantName = "Yeyue Shaokao"
    const val merchantModeLabel = "Single Merchant Mode"
    const val merchantAccount = "owner"
    const val customerH5BaseUrl = "https://order.example.com"

    fun buildCustomerOrderUrl(
        section: String,
        tableNumber: String,
        baseUrl: String = customerH5BaseUrl
    ): String {
        val safeBaseUrl = baseUrl.trim().trimEnd('/')
        val safeSection = section.trim().ifBlank { "outside" }
        val safeNumber = tableNumber.trim().ifBlank { "8" }
        return "$safeBaseUrl?section=$safeSection&number=$safeNumber"
    }
}
