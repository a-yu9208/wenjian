package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantUiTextMapper {
    fun localizeArea(area: String): String {
        return when (area.trim()) {
            "Outdoor", "outside", "outdoor", "室外" -> "室外"
            "First Floor", "first", "一楼" -> "一楼"
            "Second Floor", "second", "二楼" -> "二楼"
            "Hall", "hall", "大厅" -> "大厅"
            else -> area.ifBlank { "未分区" }
        }
    }

    fun localizeOrderStatus(status: String): String {
        return when (status.trim()) {
            "Pending", "pending", "待处理" -> "待处理"
            "Grilling", "grilling", "In Progress", "制作中", "烧烤中" -> "制作中"
            "Checkout", "Pending Bill", "pending_bill", "待结账" -> "待结账"
            "Paid", "Completed", "已完成", "completed" -> "已完成"
            else -> status.ifBlank { "待处理" }
        }
    }

    fun localizeTableStatus(status: String): String {
        return when (status.trim()) {
            "Occupied", "occupied", "使用中" -> "使用中"
            "Idle", "idle", "空闲" -> "空闲"
            "Pending Bill", "pending_bill", "待结账" -> "待结账"
            else -> status.ifBlank { "空闲" }
        }
    }

    fun localizeQrTarget(target: String): String {
        return when (target.trim()) {
            "WeChat H5", "h5", "微信 H5" -> "微信 H5"
            "Mini Program", "miniProgram", "小程序" -> "小程序"
            else -> target.ifBlank { "微信 H5" }
        }
    }

    fun localizeDishCategory(category: String): String {
        return when (category.trim()) {
            "BBQ" -> "烧烤"
            "Vegetable" -> "蔬菜"
            "Drink" -> "饮品"
            "Combo" -> "套餐"
            else -> category.ifBlank { "未分类" }
        }
    }

    fun localizeDishType(type: String): String {
        return when (type.trim()) {
            "Single" -> "单品"
            "Set Meal", "Combo" -> "套餐"
            else -> type.ifBlank { "单品" }
        }
    }
}
