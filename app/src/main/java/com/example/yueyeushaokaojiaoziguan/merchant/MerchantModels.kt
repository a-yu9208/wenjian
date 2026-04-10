package com.example.yueyeushaokaojiaoziguan.merchant

enum class MerchantTab(val label: String) {
    Home("首页"),
    Orders("订单"),
    Functions("功能")
}

enum class HomeSubTab(val label: String) {
    Pending("待处理"),
    QuickServe("快速上菜"),
    Cooking("制作中"),
    AwaitingPayment("待结账")
}

enum class FunctionEntry(val label: String, val icon: String, val desc: String) {
    Revenue("营业额统计", "📊", "查看经营数据"),
    DishManage("菜品管理", "🍖", "增删改菜品"),
    CategorySetting("分类设置", "📂", "管理菜品分类"),
    TableManage("桌台管理", "🪑", "桌码与状态"),
    PointsActivity("积分活动", "🎁", "积分规则与流水"),
    AiAssistant("烤烤助理", "🧚", "智能小助手对话"),
    Profile("个人信息", "👤", "店铺与账号")
}

data class StatCard(
    val title: String,
    val value: String,
    val note: String
)

data class QuickEntry(
    val title: String,
    val description: String
)

data class DishItem(
    val name: String,
    val category: String,
    val price: String,
    val stock: Int,
    val soldToday: Int,
    val type: String,
    val imageUri: String = "",
    val description: String = "",
    val minOrder: Int = 1,
    val quickServe: Boolean = false,
    val discountEnabled: Boolean = false,
    val discountPrice: String = "",
    val comboItems: List<ComboItem> = emptyList()
)

data class OrderDishItem(
    val name: String,
    val quantity: Int,
    val note: String = "",
    val served: Boolean = false,
    val quickServe: Boolean = false,
    val subItems: List<OrderDishItem> = emptyList()
)

data class OrderItem(
    val id: Int = 0,
    val tableLabel: String,
    val summary: String,
    val amount: String,
    val status: String,
    val time: String,
    val area: String = "",
    val isAppend: Boolean = false,
    val appendIndex: Int = 0,
    val dishes: List<OrderDishItem> = emptyList(),
    val utensilCount: Int = 0
)

data class TableItem(
    val id: Int = 0,
    val label: String,
    val area: String,
    val status: String,
    val qrTarget: String,
    val customerLink: String
)

data class TableQrDraft(
    val section: String = "outside",
    val tableNumber: String = "8",
    val target: String = "h5",
    val baseUrl: String = "https://api.teselx.cn"
)

data class ComboItem(
    val name: String,
    val quantity: Int = 1
)

data class DishDraft(
    val name: String = "",
    val category: String = "烧烤",
    val price: String = "",
    val stock: String = "10",
    val type: String = "单品",
    val description: String = "",
    val minOrder: String = "1",
    val quickServe: Boolean = false,
    val discountEnabled: Boolean = false,
    val discountPrice: String = "",
    val comboItems: List<ComboItem> = emptyList(),
    val imageUri: String = ""
)

data class PointsConfig(
    val earnRate: Int = 1,       // 消费1元得N积分
    val deductRate: Int = 10,    // N积分抵1元
    val maxDeductPercent: Int = 50  // 每单最多抵扣订单金额的百分比
)

data class PointsLog(
    val target: String,          // 桌号或手机号
    val delta: Int,              // +增 -减
    val reason: String,
    val time: String
)

data class MerchantProfile(
    val shopName: String = "月月烧烤",
    val account: String = "店主账号",
    val h5BaseUrl: String = "https://api.teselx.cn",
    val mode: String = "单店模式"
)

data class PointsUser(
    val phone: String,
    val points: Int
)
