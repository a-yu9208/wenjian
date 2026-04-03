# Android Migration Notes

This Android Studio project now contains a first merchant-side migration shell from the WeChat mini program.

## Product assumption

- There is only one merchant user.
- This Android app is only for that merchant.
- Customers do not use the Android app.
- Customers scan table QR codes and open the ordering page inside the WeChat embedded browser.
- Because of that, the architecture can stay single-tenant and does not need a multi-merchant account system.

## Included so far

- Compose merchant dashboard
- Dish management screen shell
- Order processing screen shell
- Table management screen shell
- Stable Gradle and AndroidX configuration
- Merchant repository interface and mock data source
- Mini program cloud contract notes inside the Android project
- Cloud bridge interface for later API wiring
- App container and ViewModel factory for swappable data sources
- HTTP bridge placeholder with switchable mock/real mode
- Shared HTTP client and API result models
- Backend field mapping notes for tables, dishes, orders, and QR generation
- Backend adapter plan and customer H5 plan
- HTTP wrapper specs and server task breakdown

## Local build notes

- Gradle is pinned to `E:\java` because this machine already has JDK 17 installed there.
- The build also needs Android environment variables to avoid conflict:
  - keep `ANDROID_USER_HOME=D:\Android\.android`
  - remove or unset `ANDROID_SDK_HOME`

## Recommended next steps

1. Add a repository layer that maps to the mini program cloud data model.
2. Replace sample data with API-backed ViewModels.
3. Add login and merchant settings.
4. Connect table QR generation and order actions to real endpoints.
5. Implement `MerchantCloudBridge` with your chosen backend or CloudBase Web/API adapter.
6. Swap `MerchantAppContainer.repository` from the mock-backed implementation to the real one.

## Current data-source switch

- `MerchantApiConfig.useMockData = true`
  This keeps the app stable and compile-ready while the real API is not wired.
- When your backend is ready:
  1. set `useMockData` to `false`
  2. replace placeholder parsing inside `HttpMerchantCloudBridge`
  3. point `baseApiUrl` to your actual backend

## Fallback strategy

- If HTTP succeeds and parsing succeeds:
  use live backend data
- If HTTP fails:
  keep the merchant app usable with mock data
- If HTTP succeeds but payload shape is still incomplete:
  fall back to mock data until the backend contract is aligned

## Mini program mapping reference

- Dashboard source: `miniprogram/pages/index/index.js`
- Dishes source: `miniprogram/pages/merchant/dish/index.js`
- Orders source: `miniprogram/pages/merchant/order/index.js`
- Tables source: `miniprogram/pages/merchant/table/index.js`
- Cloud env: `cloud1-1gn7fwfsa4552d34`
- Customer entry: WeChat QR code -> customer H5 ordering page
