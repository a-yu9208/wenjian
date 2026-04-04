package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantSampleData {
    val dashboardStats = listOf(
        StatCard("今日订单", "28", "较昨日上涨 12%"),
        StatCard("今日营业额", "¥3,268", "晚市整体比较稳定"),
        StatCard("制作中", "9", "包含套餐拆分菜品"),
        StatCard("待结账", "4", "顾客正在等待收银")
    )

    val quickEntries = listOf(
        QuickEntry("菜品管理", "管理上下架、库存预警、定价和套餐"),
        QuickEntry("订单处理", "覆盖待处理、烧烤中和待结账流程"),
        QuickEntry("桌台管理", "支持小程序桌码和微信 H5 点餐码"),
        QuickEntry("会员中心", "后续可继续接积分、手机号绑定和到店记录")
    )

    val dishes = listOf(
        DishItem("招牌羊肉串", "烧烤", "¥4.00", 96, 138, "单品"),
        DishItem("蒜香鸡翅", "烧烤", "¥12.00", 32, 46, "单品"),
        DishItem("烤茄子", "蔬菜", "¥18.00", 15, 22, "单品"),
        DishItem("双人套餐", "套餐", "¥128.00", 8, 12, "套餐"),
        DishItem("酸梅汤", "饮品", "¥10.00", 41, 33, "单品"),
        DishItem("夜宵套餐", "套餐", "¥168.00", 5, 9, "套餐")
    )

    val orders = listOf(
        OrderItem("室外 8 号桌", "羊肉串 x12、鸡翅 x2、酸梅汤 x2", "¥86.00", "待处理", "18:42"),
        OrderItem("一楼 3 号桌", "双人套餐 x1、烤茄子 x1", "¥146.00", "烧烤中", "18:39"),
        OrderItem("二楼 2 号桌", "羊肉串 x20、甜品 x2", "¥104.00", "待结账", "18:30"),
        OrderItem("室外 12 号桌", "夜宵套餐 x1", "¥168.00", "待结账", "18:18")
    )

    val tables = listOf(
        TableItem("8 号桌", "室外", "使用中", "微信 H5", "https://order.example.com?tableId=outside-8"),
        TableItem("12 号桌", "室外", "空闲", "微信 H5", "https://order.example.com?tableId=outside-12"),
        TableItem("3 号桌", "一楼", "使用中", "小程序", ""),
        TableItem("2 号桌", "二楼", "待结账", "微信 H5", "https://order.example.com?tableId=second-2")
    )
}
