package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantAppContainer {
    private val bridge: MerchantCloudBridge = if (MerchantApiConfig.useMockData) {
        MockMerchantCloudBridge()
    } else {
        HttpMerchantCloudBridge()
    }

    val repository: MerchantRepository by lazy {
        CloudBackedMerchantRepository(bridge)
    }
}
