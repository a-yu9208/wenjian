# API Contract Draft

This draft matches the current Android merchant app structure and the single-merchant product mode.

## General response envelope

All endpoints should preferably return:

```json
{
  "success": true,
  "data": {},
  "msg": "ok",
  "error": ""
}
```

## 1. Merchant dashboard

### Request

```http
GET /merchant/dashboard
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "stats": [
      { "title": "Today Orders", "value": "28", "note": "Up 12% from yesterday" },
      { "title": "Today Revenue", "value": "¥3268", "note": "Dinner shift is stable" }
    ]
  }
}
```

## 2. Merchant dishes

### Request

```http
GET /merchant/dishes
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "dishes": [
      {
        "name": "Signature Lamb Skewer",
        "category": "BBQ",
        "price": "¥4.00",
        "stock": 96,
        "soldToday": 138,
        "type": "Single"
      }
    ]
  }
}
```

## 3. Merchant orders

### Request

```http
GET /merchant/orders
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "orders": [
      {
        "tableLabel": "Outdoor Table 8",
        "summary": "Lamb x12, wings x2, plum drink x2",
        "amount": "¥86.00",
        "status": "Pending",
        "time": "18:42"
      }
    ]
  }
}
```

## 4. Merchant tables

### Request

```http
GET /merchant/tables
```

### Suggested response

```json
{
  "success": true,
  "data": {
    "tables": [
      {
        "label": "Table 8",
        "area": "Outdoor",
        "status": "Occupied",
        "qrTarget": "WeChat H5",
        "customerLink": "https://order.example.com?section=outside&number=8"
      }
    ]
  }
}
```

## 5. Table QR generation

### Request

```http
POST /merchant/table-qrcode
Content-Type: application/json
```

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
    "fileId": "cloud://xxx/qrcode/table-8.png",
    "targetUrl": "https://order.example.com?section=outside&number=8"
  }
}
```

## 6. Customer H5 entry

Customer QR flow:

1. Merchant app creates a table QR code.
2. Customer scans in WeChat.
3. WeChat embedded browser opens:

```text
https://order.example.com?section=outside&number=8
```

4. Customer H5 loads dishes and submits orders to the backend.

## Notes

- This project does not need multi-merchant fields right now.
- If you later add multi-merchant capability, the cleanest extension is adding `merchantId` to request context rather than rewriting the current models.
