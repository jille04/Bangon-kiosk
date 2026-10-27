# Bangon Kiosk — Java Spring Boot / MySQL

This project extends the original single-file coffee kiosk. It preserves the layout and five menu categories while using server-managed product data, pricing, orders, authentication, payment status, and loyalty.

## Team and work breakdown

| Member | Responsibilities |
|---|---|
| _Name 1_ | _e.g. Backend (Spring Boot, JPA, security)_ |
| _Name 2_ | _e.g. Frontend and UI_ |
| _Name 3_ | _e.g. Database, testing, documentation_ |

Version control history and meeting logs are provided as screenshots in the submission.

## Requirements

- Java JDK 21
- Maven 3.9+ (`mvn` must be available on PATH)
- MySQL 8+

## Setup

1. Run `database/create-database.sql` as a MySQL administrator. Set a real password for `bangon_app` (replace `CHANGE_THIS_PASSWORD`).
2. Set your own local environment variables `DB_USER=bangon_app` and `DB_PASSWORD=<your chosen password>`. Optional: `DB_URL=jdbc:mysql://localhost:3306/bangon_kiosk?useSSL=false&serverTimezone=Asia/Manila`.
3. Optional for demo counter payment confirmations: set **both** `ADMIN_EMAIL` and a unique `ADMIN_PASSWORD` of at least 12 characters **before the first startup**. This creates an administrator account; it will not silently elevate an existing user.
4. Run:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

5. Open `http://localhost:8080/` in a browser. **Do not open the HTML as `file://`**: it requires same-origin API and session cookies. The frontend is the single file `backend/src/main/resources/static/index.html`, served by Spring Boot.
6. Run the Java unit tests with `cd backend && mvn test`.

## Login and checkout

- On the landing screen, click **Login** and enter credentials. Cancel the email prompt to access the registration prompts. Customers can also use **Start Order** for guest purchases.
- Product menu, customizable pricing, and orders are served by Java REST APIs and persisted with Spring Data JPA.
- Cash orders are **awaiting payment at the counter**. Card and e-wallet are also **PENDING**; no real banking/wallet gateway is present.
- To simulate a **staff-verified** payment for the school demo, sign in using the admin account; obtain an order's numeric `id` from the `POST /api/orders` response, then send `POST /api/admin/orders/{id}/confirm-paid` with the administrator session cookie and CSRF header. This records payment, updates loyalty points transactionally, and prevents duplicate rewards on repeated confirmations.
- Only orders with authenticated customers earn and redeem loyalty points. Redeem 100 points for ₱20, earn `floor(amount / 50)` points after staff-confirmed payment.

## REST API summary

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/auth/csrf` | Get the CSRF token for mutating requests |
| POST | `/api/auth/register` | Register `{name,email,password}` |
| POST | `/api/auth/login` | Sign in `{email,password}`; session cookie |
| POST | `/api/auth/logout` | End session |
| GET | `/api/categories` | List categories |
| GET | `/api/products?category=Coffee&search=latte` | List/filter/search |
| GET | `/api/products/{id}` | Product details |
| GET | `/api/products/{id}/options` | Available options |
| POST | `/api/quote` | Server price/nutrition estimate for `Line` |
| GET/PUT | `/api/customers/me` | Profile and settings (login required) |
| GET | `/api/customers/me/orders` | Customer history |
| POST | `/api/orders` | Place pending order, including idempotency key |
| GET | `/api/orders/{id}` | Own saved order (login required) |
| POST | `/api/admin/orders/{id}/confirm-paid` | Mark payment verified (admin only) |

For unsafe requests, first GET `/api/auth/csrf`, then include `X-XSRF-TOKEN` header with the returned `token`, and preserve the same browser session cookie. All checkout prices are independently recalculated on the backend. A sample checkout body:

```json
{
  "items": [
    {"productId": 1, "quantity": 1, "choices": {"size":"large", "milk":"oat", "sweetness":50,"extraShot":true,"caramel":false,"cream":false}}
  ],
  "paymentMethod": "Cash",
  "redeemPoints": false,
  "idempotencyKey": "unique-client-generated-uuid-per-order"
}
```

## Notes and limitations

- **Tested:** `mvn test` passes (9 unit tests for pricing and order logic), and the app was run against a local MySQL 8 database with a manual browser checkout and order history. Automated database integration and browser tests are not included.
- JPA `ddl-auto=update` is convenient for local demonstration, **not production**. Use Flyway/Liquibase migrations and restricted DB permissions in deployment.
- Original nutritional figures are estimates; allergens are not backed by verified ingredient records. Daily nutrition consumption history is not fully implemented and must not be represented as an accurate health tracker.
- A real payment gateway and webhook verification (and refunds/chargebacks) are required for financial production use. The staff payment-confirmation route is a **demo workflow**.
- Price and customization rules are currently centralized in Java code, not configured per product in the database; edit code to change options.
- Source images still come from external URLs; a few original URLs may not resolve, and no local photos are bundled.
- Registration uses browser prompts to minimize visual modifications; production deployments should use accessible form-based dialogs and stronger rate limiting.
- Guest order receipts are returned on creation, but lookup by guest URL is intentionally disallowed without a secure claim token.
- Browser test suite, full integration tests, and database-specific concurrency testing remain to be built.
