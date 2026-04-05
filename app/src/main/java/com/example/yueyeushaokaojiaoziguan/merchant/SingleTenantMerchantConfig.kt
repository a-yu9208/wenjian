package com.example.yueyeushaokaojiaoziguan.merchant

object SingleTenantMerchantConfig {
    const val merchantName = "月月烧烤"
    const val merchantModeLabel = "单店模式"
    const val merchantAccount = "店主账号"
    const val customerH5BaseUrl = "https://api.teselx.cn"

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
