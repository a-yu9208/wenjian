# API Migration Checklist

This project is now designed for:

- one merchant user using the Android app
- customers scanning table QR codes
- customers ordering inside the WeChat embedded browser

## Priority order

### Phase 1

- `GET /merchant/tables`
- `GET /merchant/dishes`
- `GET /merchant/orders`

These three are enough to turn the Android merchant shell into a live read-only dashboard.

### Phase 2

- `POST /merchant/table-qrcode`
- `POST /merchant/dishes`
- `PATCH /merchant/dishes/{id}`
- `POST /merchant/order-actions`

These cover table QR generation, dish editing, and order processing.

### Phase 3

- customer H5 dish loading
- customer H5 order submission
- checkout confirmation
- points and member lookup

## Mini program source mapping

- `miniprogram/pages/index/index.js` -> merchant dashboard
- `miniprogram/pages/merchant/dish/index.js` -> dish management
- `miniprogram/pages/merchant/order/index.js` -> order processing
- `miniprogram/pages/merchant/table/index.js` -> table management and QR logic
- `cloudfunctions/generateTableQRCode/index.js` -> table QR generation
- `cloudfunctions/manageOrder/index.js` -> merchant order actions
- `cloudfunctions/createOrder/index.js` -> customer order submission
