package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantFieldMappers {
    fun mapTableArea(section: String): String {
        return when (section) {
            "outside" -> "Outdoor"
            "first" -> "First Floor"
            "second" -> "Second Floor"
            "hall" -> "Hall"
            else -> section.ifBlank { "Unknown" }
        }
    }

    fun mapTableLabel(number: String): String {
        val safeNumber = number.trim().ifBlank { "-" }
        return "Table $safeNumber"
    }

    fun mapOrderStatus(status: Int, isPaid: Boolean): String {
        return when {
            !isPaid -> "Checkout"
            status == 0 -> "Pending"
            status == 1 -> "Paid"
            status == 2 -> "Completed"
            else -> "Pending"
        }
    }
}
