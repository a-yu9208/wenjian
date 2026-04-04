# UI 重构问题排查报告

## 排查时间：2026-04-05

---

## 问题一：划菜功能无法操作

### 原因
`MainActivity.kt` 第 83-84 行，`onToggleDishServed` 回调是空实现：
```kotlin
onToggleDishServed = { orderTable, dishName ->
    // TODO: implement toggle served
},
```
点击菜品时回调被触发，但什么都没做，所以划线效果不会出现。

### 修复方案
需要在 `MerchantViewModel` 中新增 `toggleDishServed(tableLabel: String, dishName: String)` 方法，
找到对应订单中的菜品，切换其 `served` 状态，然后在 `MainActivity` 中绑定这个方法。

**MerchantViewModel.kt 新增方法：**
```kotlin
fun toggleDishServed(tableLabel: String, dishName: String) {
    _uiState.value = _uiState.value.let { state ->
        val updatedOrders = state.orders.map { order ->
            if (order.tableLabel == tableLabel) {
                order.copy(dishes = order.dishes.map { dish ->
                    if (dish.name == dishName) dish.copy(served = !dish.served) else dish
                })
            } else order
        }
        state.copy(orders = updatedOrders)
    }
}
```

**MainActivity.kt 修改：**
```kotlin
onToggleDishServed = vm::toggleDishServed,
```

---

## 问题二：菜品管理无法添加菜品

### 原因
`addDishFromDraft()` 中创建 `DishItem` 时只传了 6 个参数（name, category, price, stock, soldToday, type），
但 `MerchantModels.kt` 中 `DishItem` 新增了 `imageUri`, `description`, `minOrder`, `quickServe`, `discountEnabled`, `discountPrice` 字段。

虽然这些新字段有默认值不会导致编译错误，但 `AddDishDialog` 中缺少以下输入项：
- 菜品描述
- 最低起点数量（minOrder）
- 可快速上菜开关（quickServe）
- 折扣开关和折扣价

另外 `AddDishDialog` 中分类选择用 `Row` 包裹 `AssistChip`，只取了前5个分类（`.take(5)`），
如果分类超过5个会溢出 Row 宽度，在手机上可能显示不全或布局异常。

### 修复方案
1. `AddDishDialog` 改用 `FlowRow` 替代 `Row` 来展示分类 Chip，去掉 `.take(5)` 限制
2. 添加缺失的输入字段（描述、最低起点、快速上菜开关、折扣）
3. `MerchantViewModel.addDishFromDraft()` 中创建 DishItem 时补上新字段：

```kotlin
val newDish = DishItem(
    name = draft.name.trim(),
    category = draft.category,
    price = normalizedPrice,
    stock = stockValue,
    soldToday = 0,
    type = draft.type,
    description = draft.description,
    minOrder = draft.minOrder.toIntOrNull() ?: 1,
    quickServe = draft.quickServe,
    discountEnabled = draft.discountEnabled,
    discountPrice = if (draft.discountEnabled) {
        val dp = draft.discountPrice.trim()
        if (dp.startsWith("¥")) dp else "¥$dp"
    } else ""
)
```

4. `updateDishDraft()` 方法需要扩展参数以支持新字段：
```kotlin
fun updateDishDraft(
    name: String? = null,
    category: String? = null,
    price: String? = null,
    stock: String? = null,
    type: String? = null,
    description: String? = null,
    minOrder: String? = null,
    quickServe: Boolean? = null,
    discountEnabled: Boolean? = null,
    discountPrice: String? = null
)
```

---

## 问题三：桌台管理看不到二维码

### 原因
`TableManageScreen` 中只显示了 `customerLink`（文字链接），没有生成和展示二维码图片。
`generateQrForDraft()` 方法只返回了 URL 和 fileId，但 UI 上没有渲染二维码图片。

### 修复方案
有两种方式：

**方案A（推荐，纯本地）：** 添加 QR 码生成库，在本地根据 URL 生成二维码图片
- `build.gradle.kts` 添加依赖：`implementation("com.google.zxing:core:3.5.2")`
- 写一个工具函数将 URL 转为 Bitmap
- 在桌台列表每项中展示二维码图片，支持点击放大查看

**方案B（简单）：** 用第三方 API 生成二维码图片 URL，通过 Coil/Glide 加载显示

建议用方案A，示例代码：
```kotlin
// 工具函数
fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
```

在 `TableManageScreen` 桌台列表中，每个桌台卡片增加"查看二维码"按钮，
点击后弹窗显示二维码图片。

---

## 问题四：FunctionsScreen 的 rememberSaveable 可能崩溃

### 原因
```kotlin
var activePage by rememberSaveable { mutableStateOf<FunctionEntry?>(null) }
```
`FunctionEntry` 是自定义 enum，`rememberSaveable` 要求值可以被序列化保存。
enum 默认支持 `Saver`，但如果 Activity 被系统回收后恢复，可能出现反序列化问题。

### 修复方案
改用 `remember` 或者用 enum 的 name 来存储：
```kotlin
var activePageName by rememberSaveable { mutableStateOf<String?>(null) }
val activePage = activePageName?.let { name -> FunctionEntry.entries.find { it.name == name } }
```

---

## 问题五：SampleData 中订单状态不匹配

### 原因
`MerchantSampleData.kt` 中的订单使用了 `"制作中"` 状态（已更新），
但 `advanceOrderStatus()` 是通过 `tableLabel` 匹配的，
如果同一桌有多条订单（首次+追加），`advanceOrderStatus` 会把所有同桌号的订单状态一起改。

### 修复方案
`advanceOrderStatus` 需要用更精确的标识来匹配订单，比如 `tableLabel + time` 组合：
```kotlin
fun advanceOrderStatus(tableLabel: String, time: String) {
    val nextState = _uiState.value.let { state ->
        val updated = state.orders.map { order ->
            if (order.tableLabel == tableLabel && order.time == time) {
                order.copy(status = nextOrderStatus(order.status))
            } else order
        }
        state.copy(orders = updated)
    }
    _uiState.value = nextState
}
```
对应 UI 层也需要传递 time 参数。

---

## 问题六：H5 页面无法访问

### 原因
H5 文件放在 `/root/wenjian/h5/` 目录下，这只是静态文件，
需要一个 HTTP 服务器来提供访问。直接用 `file://` 协议在微信内置浏览器中无法打开。

### 修复方案
1. **本地测试：** 在服务器上启动一个简单的 HTTP 服务：
   ```bash
   cd /root/wenjian/h5 && python3 -m http.server 8080
   ```
   然后通过 `http://服务器IP:8080?section=outside&number=8` 访问

2. **正式部署：** 将 h5 目录部署到你的域名下（比如 api.teselx.cn），
   或者用 Nginx 配置一个静态文件服务

3. **微信环境：** 微信内置浏览器访问需要 HTTPS，
   所以正式环境需要配置 SSL 证书

---

## 问题七：菜品管理删除功能未实现

### 原因
`DishManageScreen.kt` 中删除按钮的 onClick 是空实现：
```kotlin
onClick = {
    // TODO: implement batch delete
    selected = emptySet()
    manageMode = false
}
```

### 修复方案
在 `MerchantViewModel` 中新增批量删除方法：
```kotlin
fun deleteDishes(names: Set<String>) {
    _uiState.value = _uiState.value.copy(
        dishes = _uiState.value.dishes.filterNot { it.name in names },
        noticeMessage = "已删除 ${names.size} 道菜品"
    )
}
```
然后在 `DishManageScreen` 中调用 `vm.deleteDishes(selected)`。

---

## 修复优先级

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | 划菜功能空实现 | 核心功能不可用 |
| P0 | 菜品添加字段不完整 | 新增菜品缺少关键信息 |
| P0 | H5 页面无法访问 | 顾客端完全不可用 |
| P1 | 二维码不显示 | 桌台管理核心功能缺失 |
| P1 | 菜品删除未实现 | 管理功能不完整 |
| P1 | 订单状态匹配不精确 | 追加订单状态会被误改 |
| P2 | FunctionsScreen saveable 问题 | 低概率崩溃 |
| P2 | 分类 Chip 布局溢出 | 分类多时显示异常 |

---

## 明天交互建议

1. 先修 P0 的三个问题（划菜、添加菜品、H5 部署）
2. 再加二维码生成功能
3. 最后完善删除和订单匹配逻辑
