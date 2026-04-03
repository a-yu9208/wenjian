package com.example.yueyeushaokaojiaoziguan.merchant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MerchantViewModelFactory(
    private val repository: MerchantRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MerchantViewModel::class.java)) {
            return MerchantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
