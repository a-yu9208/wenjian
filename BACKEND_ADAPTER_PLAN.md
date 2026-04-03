# Backend Adapter Plan

This plan keeps your current mini program backend as much as possible and adds only the minimum adapter layer needed for:

- the Android merchant app
- the customer H5 opened inside the WeChat embedded browser

## Core idea

Do not rewrite the current business rules first.

Reuse the existing cloud functions and collections:

- `tables`
- `dishes`
- `categories`
- `orders`
- `users`
- `settings`
- `points_logs`

Then add a thin HTTP adapter layer in front of them.

## Recommended architecture

### Merchant Android app

Merchant app calls HTTP endpoints such as:

- `GET /merchant/tables`
- `GET /merchant/dishes`
- `GET /merchant/orders`
- `POST /merchant/table-qrcode`

### Customer H5

Customer H5 calls HTTP endpoints such as:

- `GET /customer/table`
- `GET /customer/dishes`
- `POST /customer/order`
- `GET /customer/order-status`

### Adapter layer

Each HTTP endpoint does one of these:

1. Query a collection directly.
2. Call an existing cloud function.
3. Format the response for Android or H5.

## Mapping by current backend

### Merchant tables

- source collection: `tables`
- QR generation source: `generateTableQRCode`

Adapter:

- `GET /merchant/tables`
  reads `tables`
- `POST /merchant/table-qrcode`
  calls `generateTableQRCode`

### Merchant dishes

- source collection: `dishes`

Adapter:

- `GET /merchant/dishes`
  reads `dishes`

### Merchant orders

- source collection: `orders`
- order actions source: `manageOrder`

Adapter:

- `GET /merchant/orders`
  reads `orders`
- `POST /merchant/order-actions`
  calls `manageOrder`

### Customer order creation

- source function: `createOrder`

Adapter:

- `POST /customer/order`
  calls `createOrder`

### Customer checkout status

- source collection: `orders`

Adapter:

- `GET /customer/order-status`
  reads `orders` by order id or checkout number

## What should stay unchanged first

To reduce risk, keep these unchanged in the beginning:

- current cloud env id
- current collection names
- current order status meanings
- current QR generation logic

## Best first implementation path

1. Build merchant read-only adapters:
   - `/merchant/tables`
   - `/merchant/dishes`
   - `/merchant/orders`
2. Build merchant QR adapter:
   - `/merchant/table-qrcode`
3. Build customer dish loading:
   - `/customer/table`
   - `/customer/dishes`
4. Build customer order submit:
   - `/customer/order`
5. Build customer order progress and checkout lookup

## Why this is the safest path

- Android merchant app can go live earlier with read-only data first.
- Customer H5 can be tested table by table.
- Existing mini program business logic remains the source of truth.
- You avoid rewriting all order rules at once.
