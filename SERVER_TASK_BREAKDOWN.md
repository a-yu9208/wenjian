# Server Task Breakdown

## Read-only merchant endpoints first

1. `GET /merchant/tables`
2. `GET /merchant/dishes`
3. `GET /merchant/orders`

These let the Android app show live data first.

## QR and actions second

4. `POST /merchant/table-qrcode`
5. `POST /merchant/order-actions`

These let the merchant app do useful operations.

## Customer H5 third

6. `GET /customer/table`
7. `GET /customer/dishes`
8. `POST /customer/order`
9. `GET /customer/order-status`

These complete the customer ordering flow.

## Nice-to-have later

- points lookup
- member profile lookup
- payment confirmation wrapper
- sales dashboard aggregation endpoint
