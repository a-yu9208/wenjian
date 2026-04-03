package com.example.yueyeushaokaojiaoziguan.merchant

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MerchantViewModel(
    private val repository: MerchantRepository = MockMerchantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantUiState())
    val uiState: StateFlow<MerchantUiState> = _uiState.asStateFlow()

    init {
        loadMerchantData()
    }

    fun loadMerchantData() {
        _uiState.value = MerchantUiState(
            dashboardStats = repository.getDashboardStats(),
            quickEntries = repository.getQuickEntries(),
            dishes = repository.getDishes(),
            orders = repository.getOrders(),
            tables = repository.getTables(),
            qrPreviewUrl = buildQrPreview(TableQrDraft()),
            loading = false
        )
    }

    fun updateQrDraft(
        section: String? = null,
        tableNumber: String? = null,
        target: String? = null,
        baseUrl: String? = null
    ) {
        val current = _uiState.value.qrDraft
        val next = current.copy(
            section = section ?: current.section,
            tableNumber = tableNumber ?: current.tableNumber,
            target = target ?: current.target,
            baseUrl = baseUrl ?: current.baseUrl
        )

        _uiState.value = _uiState.value.copy(
            qrDraft = next,
            qrPreviewUrl = buildQrPreview(next)
        )
    }

    private fun buildQrPreview(draft: TableQrDraft): String {
        if (draft.target != "h5") {
            return """{"t":"table","s":"${draft.section}","n":"${draft.tableNumber}"}"""
        }

        return SingleTenantMerchantConfig.buildCustomerOrderUrl(
            section = draft.section,
            tableNumber = draft.tableNumber,
            baseUrl = draft.baseUrl
        )
    }
}
