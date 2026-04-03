package com.example.yueyeushaokaojiaoziguan.merchant

sealed class MerchantApiResult<out T> {
    data class Success<T>(val data: T) : MerchantApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : MerchantApiResult<Nothing>()
}

data class MerchantApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val msg: String = "",
    val error: String = ""
)
