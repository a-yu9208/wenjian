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
        DishItem("招牌羊肉串", "烧烤", "¥4.00", 96, 138, "单品", minOrder = 5, quickServe = false),
        DishItem("蒜香鸡翅", "烧烤", "¥12.00", 32, 46, "单品"),
        DishItem("烤茄子", "蔬菜", "¥18.00", 15, 22, "单品"),
        DishItem("双人套餐", "套餐", "¥128.00", 8, 12, "套餐"),
        DishItem("酸梅汤", "饮品", "¥10.00", 41, 33, "单品", quickServe = true),
        DishItem("花生米", "凉菜", "¥8.00", 50, 28, "单品", quickServe = true),
        DishItem("啤酒", "酒水", "¥8.00", 100, 65, "单品", quickServe = true),
        DishItem("夜宵套餐", "套餐", "¥168.00", 5, 9, "套餐"),
        DishItem("烤鸡翅", "烧烤", "¥10.00", 40, 35, "单品", discountEnabled = true, discountPrice = "¥8.00")
    )

    val orders = listOf(
        OrderItem(
            tableLabel = "室外 8 号桌", summary = "羊肉串 x12、鸡翅 x2、酸梅汤 x2", amount = "¥86.00", status = "待处理", time = "18:42",
            area = "室外",
            dishes = listOf(
                OrderDishItem("招牌羊肉串", 12),
                OrderDishItem("蒜香鸡翅", 2),
                OrderDishItem("酸梅汤", 2, quickServe = true)
            )
        ),
        OrderItem(
            tableLabel = "一楼 3 号桌", summary = "双人套餐 x1、烤茄子 x1", amount = "¥146.00", status = "制作中", time = "18:39",
            area = "一楼",
            dishes = listOf(
                OrderDishItem("双人套餐", 1, subItems = listOf(
                    OrderDishItem("招牌羊肉串", 10),
                    OrderDishItem("蒜香鸡翅", 2),
                    OrderDishItem("啤酒", 2, quickServe = true)
                )),
                OrderDishItem("烤茄子", 1)
            )
        ),
        OrderItem(
            tableLabel = "二楼 2 号桌", summary = "羊肉串 x20、花生米 x2", amount = "¥104.00", status = "制作中", time = "18:30",
            area = "二楼",
            dishes = listOf(
                OrderDishItem("招牌羊肉串", 20),
                OrderDishItem("花生米", 2, quickServe = true, served = true)
            )
        ),
        OrderItem(
            tableLabel = "室外 12 号桌", summary = "夜宵套餐 x1", amount = "¥168.00", status = "待结账", time = "18:18",
            area = "室外",
            dishes = listOf(
                OrderDishItem("夜宵套餐", 1, served = true, subItems = listOf(
                    OrderDishItem("招牌羊肉串", 15, served = true),
                    OrderDishItem("蒜香鸡翅", 4, served = true),
                    OrderDishItem("烤茄子", 2, served = true),
                    OrderDishItem("啤酒", 4, quickServe = true, served = true)
                ))
            ),
            utensilCount = 2
        ),
        OrderItem(
            tableLabel = "室外 8 号桌", summary = "啤酒 x3、花生米 x1", amount = "¥32.00", status = "待处理", time = "18:55",
            area = "室外",
            isAppend = true, appendIndex = 1,
            dishes = listOf(
                OrderDishItem("啤酒", 3, quickServe = true),
                OrderDishItem("花生米", 1, quickServe = true)
            )
        )
    )

    val tables = listOf(
        TableItem(id = 1, label = "8 号桌", area = "室外", status = "使用中", qrTarget = "微信 H5", customerLink = "https://api.teselx.cn?section=outside&number=8"),
        TableItem(id = 2, label = "12 号桌", area = "室外", status = "空闲", qrTarget = "微信 H5", customerLink = "https://api.teselx.cn?section=outside&number=12"),
        TableItem(id = 3, label = "3 号桌", area = "一楼", status = "使用中", qrTarget = "小程序", customerLink = ""),
        TableItem(id = 4, label = "2 号桌", area = "二楼", status = "待结账", qrTarget = "微信 H5", customerLink = "https://api.teselx.cn?section=second&number=2")
    )
}
