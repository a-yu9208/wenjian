package com.example.yueyeushaokaojiaoziguan.merchant

object MerchantSampleData {
    val dashboardStats = listOf(
        StatCard("Today Orders", "28", "Up 12% from yesterday"),
        StatCard("Today Revenue", "¥3,268", "Dinner shift is stable"),
        StatCard("In Production", "9", "Includes set meal split items"),
        StatCard("Pending Checkout", "4", "Waiting at cashier")
    )

    val quickEntries = listOf(
        QuickEntry("Dish Management", "Shelf status, stock warnings, pricing, set meals"),
        QuickEntry("Order Processing", "Pending, grilling, and checkout flows"),
        QuickEntry("Table Management", "Mini program QR or WeChat H5 ordering QR"),
        QuickEntry("Member Center", "Points, phone binding, and visit history later")
    )

    val dishes = listOf(
        DishItem("Signature Lamb Skewer", "BBQ", "¥4.00", 96, 138, "Single"),
        DishItem("Garlic Chicken Wings", "BBQ", "¥12.00", 32, 46, "Single"),
        DishItem("Roasted Eggplant", "Vegetable", "¥18.00", 15, 22, "Single"),
        DishItem("Couple Combo", "Combo", "¥128.00", 8, 12, "Set Meal"),
        DishItem("Sour Plum Drink", "Drink", "¥10.00", 41, 33, "Single"),
        DishItem("Late Night Combo", "Combo", "¥168.00", 5, 9, "Set Meal")
    )

    val orders = listOf(
        OrderItem("Outdoor Table 8", "Lamb x12, wings x2, plum drink x2", "¥86.00", "Pending", "18:42"),
        OrderItem("First Floor Table 3", "Couple combo x1, eggplant x1", "¥146.00", "Grilling", "18:39"),
        OrderItem("Second Floor Table 2", "Lamb x20, dessert x2", "¥104.00", "Checkout", "18:30"),
        OrderItem("Outdoor Table 12", "Late night combo x1", "¥168.00", "Checkout", "18:18")
    )

    val tables = listOf(
        TableItem("Table 8", "Outdoor", "Occupied", "WeChat H5", "https://order.example.com?tableId=outside-8"),
        TableItem("Table 12", "Outdoor", "Idle", "WeChat H5", "https://order.example.com?tableId=outside-12"),
        TableItem("Table 3", "First Floor", "Occupied", "Mini Program", ""),
        TableItem("Table 2", "Second Floor", "Pending Bill", "WeChat H5", "https://order.example.com?tableId=second-2")
    )
}
