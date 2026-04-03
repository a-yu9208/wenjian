# HTTP Wrapper Specs

These wrapper specs are designed to sit in front of the existing WeChat cloud functions and collections.

They are intentionally thin so the current backend logic stays reusable.

## 1. GET /merchant/tables

### Purpose

Used by the Android merchant app to display table list and QR target info.

### Backend responsibility

- Query collection `tables`
- Convert current fields into Android-friendly response fields

### Suggested response

```json
{
  "success": true,
  "data": {
    "tables": [
      {
        "id": "table_001",
        "label": "Table 8",
        "area": "Outdoor",
        "status": "Occupied",
        "qrTarget": "WeChat H5",
        "customerLink": "https://order.example.com?section=outside&number=8"
      }
    ]
  },
  "msg": "ok"
}
```

## 2. GET /merchant/dishes

### Purpose

Used by the Android merchant app to show all dishes and stock.

### Backend responsibility

- Query collection `dishes`
- Optionally join category text if needed
- Format current price and stock

### Suggested response

```json
{
  "success": true,
  "data": {
    "dishes": [
      {
        "id": "dish_001",
        "name": "Signature Lamb Skewer",
        "category": "BBQ",
        "price": "¥4.00",
        "stock": 96,
        "soldToday": 138,
        "type": "Single"
      }
    ]
  },
  "msg": "ok"
}
```

## 3. GET /merchant/orders

### Purpose

Used by the Android merchant app to show current order queue and checkout state.

### Backend responsibility

- Query collection `orders`
- Convert `status` and `isPaid` into readable merchant status
- Build item summary string

### Suggested response

```json
{
  "success": true,
  "data": {
    "orders": [
      {
        "id": "order_001",
        "tableLabel": "Outdoor Table 8",
        "summary": "Lamb x12, wings x2, plum drink x2",
        "amount": "¥86.00",
        "status": "Pending",
        "time": "18:42"
      }
    ]
  },
  "msg": "ok"
}
```

## 4. POST /merchant/table-qrcode

### Purpose

Used by the Android merchant app to create a table QR code.

### Backend responsibility

- Receive section, number, target, baseUrl
- Call existing cloud function `generateTableQRCode`
- Return uploaded file id and target URL

### Request example

```json
{
  "section": "outside",
  "number": 8,
  "target": "h5",
  "baseUrl": "https://order.example.com"
}
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "fileId": "cloud://env/qrcode/h5-outside-8.png",
    "targetUrl": "https://order.example.com?section=outside&number=8"
  },
  "msg": "ok"
}
```

## 5. GET /customer/table

### Purpose

Used by the WeChat embedded H5 after customer scans the QR code.

### Backend responsibility

- Resolve table by `section + number` or `tableId`
- Return store name and table info

### Request example

```http
GET /customer/table?section=outside&number=8
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "shopName": "Yeyue Shaokao",
    "table": {
      "id": "table_001",
      "section": "outside",
      "number": 8,
      "status": false
    }
  },
  "msg": "ok"
}
```

## 6. GET /customer/dishes

### Purpose

Used by customer H5 to render categories and dish list.

### Backend responsibility

- Query `categories`
- Query `dishes`
- return only available / on-sale dishes

### Suggested response

```json
{
  "success": true,
  "data": {
    "categories": [],
    "dishes": []
  },
  "msg": "ok"
}
```

## 7. POST /customer/order

### Purpose

Used by customer H5 to submit an order.

### Backend responsibility

- Forward to current `createOrder`
- preserve current stock and set-meal logic

### Request example

```json
{
  "tableId": "table_001",
  "items": [
    {
      "dishId": "dish_001",
      "quantity": 2
    }
  ],
  "anonymousId": "guest_xxx"
}
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "orderId": "order_001",
    "orderNumber": "202604040001",
    "totalAmount": 86
  },
  "msg": "Order created"
}
```

## 8. POST /merchant/order-actions

### Purpose

Used by the Android merchant app for order operations.

### Backend responsibility

- Forward to existing `manageOrder`

### Suggested request example

```json
{
  "action": "updateDishCookingStatus",
  "orderId": "order_001",
  "itemIndex": 0,
  "productionIndex": -1,
  "status": "grilling"
}
```

## Implementation note

You do not need to migrate all logic into HTTP handlers immediately.

The safest first version is:

1. HTTP handler receives request
2. HTTP handler reads DB directly or calls existing cloud function
3. HTTP handler transforms response into Android / H5 friendly JSON
