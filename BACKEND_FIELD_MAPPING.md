# Backend Field Mapping

This file maps the Android merchant app contract to the current WeChat mini program backend.

## 1. Tables

### Source

- collection: `tables`
- mini program page: `miniprogram/pages/merchant/table/index.js`

### Existing fields in current project

- `_id`
- `section`
- `number`
- `qrCode`
- `qrTarget`
- `baseUrl`
- `targetUrl`
- `status`
- `createTime`
- `updateTime`

### Android table model mapping

- `label` <- `"Table ${number}"`
- `area` <- `section`
- `status` <- `status`
- `qrTarget` <- `qrTarget`
- `customerLink` <- `targetUrl`

## 2. Dishes

### Source

- collection: `dishes`
- mini program page: `miniprogram/pages/merchant/dish/index.js`

### Existing fields in current project

- `_id`
- `name`
- `categoryId`
- `category`
- `originalPrice`
- `currentPrice`
- `isDiscount`
- `description`
- `imageUrl`
- `stock`
- `minOrderQuantity`
- `bundleItems`
- `status`
- `soldToday`
- `type`
- `isSoldOut`

### Android dish model mapping

- `name` <- `name`
- `category` <- `category`
- `price` <- `currentPrice`
- `stock` <- `stock`
- `soldToday` <- `soldToday`
- `type` <- `type`

## 3. Orders

### Source

- collection: `orders`
- mini program page: `miniprogram/pages/merchant/order/index.js`
- cloudfunctions:
  - `createOrder`
  - `manageOrder`
  - `checkout`

### Existing fields in current project

- `_id`
- `orderNumber`
- `tableId`
- `tableNumber`
- `section`
- `items`
- `originalAmount`
- `finalAmount`
- `status`
- `isPaid`
- `checkoutNumber`
- `checkoutAmount`
- `usePoints`
- `pointsDeduction`
- `paymentMethod`
- `createTime`
- `updateTime`

### Android order model mapping

- `tableLabel` <- `"${section} Table ${tableNumber}"`
- `summary` <- join `items`
- `amount` <- `finalAmount`
- `status` <- derive from `status` and `isPaid`
- `time` <- `createTime`

## 4. Table QR generation

### Source

- cloud function: `generateTableQRCode`
- file: `cloudfunctions/generateTableQRCode/index.js`

### Request mapping

- `section` -> same
- `number` -> same
- `target` -> same
- `baseUrl` -> same

### Response mapping

- `fileId` <- `fileID`
- `targetUrl` <- `targetUrl`

## 5. Suggested HTTP wrapper endpoints

If you expose the current cloud logic through HTTP, these are the simplest wrappers:

- `GET /merchant/tables` -> query `tables`
- `GET /merchant/dishes` -> query `dishes`
- `GET /merchant/orders` -> query `orders`
- `POST /merchant/table-qrcode` -> call `generateTableQRCode`

## 6. Customer H5 follow-up

The customer H5 will still need separate endpoints for:

- table lookup
- dish list
- order creation
- checkout status
