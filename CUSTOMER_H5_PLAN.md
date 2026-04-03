# Customer H5 Plan

Customers do not use the Android app.

They scan a table QR code and open the ordering page inside the WeChat embedded browser.

## URL pattern

Suggested URL:

```text
https://order.example.com?section=outside&number=8
```

## Required H5 screens

### 1. Table landing page

- confirm table info
- show shop name
- allow customer to start ordering

### 2. Dish list

- categories
- dishes
- search
- set meals
- stock state

### 3. Cart and submit

- quantity adjust
- submit order
- optional member info later

### 4. Order result / status

- order created
- waiting
- grilling
- paid / unpaid state

## Required backend endpoints

- `GET /customer/table`
- `GET /customer/dishes`
- `POST /customer/order`
- `GET /customer/order-status`

## Data source mapping

- table info -> `tables`
- dish list -> `dishes` + `categories`
- order submit -> `createOrder`
- order progress -> `orders`

## Product constraints

- no multi-merchant logic needed
- no customer app needed
- all customer flows should work inside WeChat browser first
