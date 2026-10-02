🛒 SmartCart — eCommerce Platform

A modern single-vendor eCommerce platform designed for independent sellers, featuring a clean customer storefront and a powerful admin panel for product and order management.

📌 Overview

SmartCart is a full-stack web application that enables customers to browse products, manage carts, and place orders securely.

The system is built with scalable architecture using a Modular Monolith design, making it ready for future microservices extraction.

🧱 Tech Stack Layer Technology Frontend Angular (SPA) Backend Spring Boot Security Spring Security + JWT ORM JPA / Hibernate Database MySQL Language Java 17

🏗 System Design
Order Creation Flow
The following sequence diagram illustrates how an order is created in the system and how services interact to validate inventory, persist orders, and clear the cart.

Order Creation Sequence Diagram

Flow Explanation
Customer sends createOrder(cartId) request.
OrderService fetches the cart from CartService.
For each cart item:
ProductService validates product existence.
InventoryService checks stock availability.
If stock is unavailable → request fails with 409 OutOfStock.
If stock is available:
Inventory is reduced.
Order is persisted via OrderRepo.
Order is created and returned to the customer.
The cart is cleared after successful order creation.
🧰 Tools & Technologies
Backend
Java 17
Spring Boot
Spring Security (JWT)
JPA / Hibernate
MySQL
Frontend
Angular SPA
Angular HttpClient
Architecture & Design
Modular Monolith Architecture
RESTful API Design
Sequence Diagrams
Database Normalization (3NF)
Dev Tools
Git
GitHub
Maven
IntelliJ / VS Code
🎯 Goals & Objectives Business Goals

Launch MVP within 2–3 months

Provide end-to-end purchase flow

Enable complete product control for admins

Technical Goals

Stateless JWT authentication

RESTful API design

Modular Monolith architecture

Scalable microservice-ready structure

Clean Angular component architecture

👥 Target Users 👤 Customer

Customers can:

Browse products

Search products

Add items to cart

Checkout securely

View order history

👨‍💼 Admin

Admins can:

Add products

Update product details

Delete products

Monitor orders

📚 User Stories 🛍 Customer

Browse available products

Search for specific items

Add products to cart

Securely checkout orders

⚙ Admin

Add new products to the store

Update product information

Remove unavailable products

⚙ Functional Requirements 🔐 Authentication Endpoint Access POST /register Public POST /login Public 📦 Product Module Endpoint Access GET /products Public POST /products Admin PUT /products/{id} Admin DELETE /products/{id} Admin 🛒 Cart Module Endpoint Access POST /cart/add Authenticated DELETE /cart/{itemId} Authenticated PATCH /cart/{itemId} Authenticated 💳 Checkout Endpoint Access POST /orders Authenticated

Behavior

Creates order

Clears user cart

⚡ Non-Functional Requirements Performance

Page load < 2 seconds

API response < 500ms

Pagination enforced

Security

BCrypt password hashing

JWT authentication

Input validation

SQL injection protection

Usability

Fully responsive Angular UI

Mobile-first design

Observability

Structured logging

Consistent API error responses

🏗 Architecture Architectural Style

Modular Monolith

A single deployable application divided into bounded internal modules, allowing future migration to microservices.

System Architecture Client Layer │ ▼ Angular SPA │ HTTPS / REST │ ▼ Spring Boot API Layer │ Spring Security (JWT) │ ▼ Business Modules ├── User Module ├── Product Module ├── Cart Module ├── Order Module └── Admin Module │ ▼ Data Layer MySQL Database 🧠 Module Responsibilities User Module

Authentication

Role management (ADMIN / CUSTOMER)

JWT token generation

Product Module

Product CRUD operations

Pagination

Indexed search

Cart Module

Per-user cart

Quantity management

Real-time total calculation

Order Module

Order creation

Order item persistence

Order lifecycle tracking

Admin Module

Product management

Order monitoring

🗄 Database Design Schema Strategy

Fully normalized 3NF schema

Indexed Columns

product.name

product.category

Relationships User 1 : 1 Cart Cart 1 : N CartItem CartItem N : 1 Product User 1 : N Order Order 1 : N OrderItem OrderItem N : 1 Product ⚡ Performance Optimization Pagination

Implemented using:

Spring Pageable Indexing

Indexes applied on:

Product name

Product category

Lazy Loading

JPA relationships loaded on demand

N+1 Query Prevention

Using:

JOIN FETCH 🚀 Scalability Roadmap

Future enhancements include:

Microservices Migration

Possible service extraction:

User Service

Product Service

Order Service

Redis Caching

Cache:

Product catalog

Search results

Payment Gateway

Integration with:

Stripe

PayPal

CDN

Store product images using:

CloudFront

Cloudflare

⚠ Risks & Mitigation Risk Mitigation Stock concurrency Optimistic locking Security misconfiguration Security audit Cart inconsistency Server-side cart Large catalog performance Indexing + pagination 📊 Success Metrics Product Metrics

Order success rate

Registered user growth

Operational Metrics

API response < 500ms

Zero critical admin failures

Zero critical security vulnerabilities

---

## 🔐 Local security setup

Authentication supports **email + password** and **Sign in with Google**. Both end with the backend issuing the same RS256 JWT, which the Angular app sends as `Authorization: Bearer <token>`.

### 1. Generate a JWT key pair (once)

```bash
cd backend/eCommerce
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private_key.pem
openssl pkey -in keys/private_key.pem -pubout -out keys/public_key.pem
```

`keys/` is git-ignored. In other environments, point `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` at the key files (e.g. `file:/run/secrets/jwt_private.pem`).

### 2. Google OAuth2 client

In Google Cloud Console → APIs & Services → Credentials, create an **OAuth client ID (Web application)** with:

- Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`

Put the values in `backend/eCommerce/src/main/resources/application-secrets.properties` (git-ignored):

```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

### 3. Database

Defaults come from `application.properties` and can be overridden with `DB_URL`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
One-off migrations for databases created by older builds (run with the backend stopped):

- `db/001_customer_oauth2_columns.sql` - if the DB predates Google sign-in.
- `db/002_product_image_bytea.sql` - if `product_image.image_data` is still an `oid` (pre-`bytea` builds).

Sample catalogue (24 products with real photos, 5 categories, 5 brands; safe to re-run):

```bash
cd backend/eCommerce
python db/generate_dev_seed.py        # downloads the Unsplash photos once into db/.image-cache (git-ignored)
psql -h localhost -U postgres -d postgres -f db/dev_seed_products.sql -f db/dev_seed_product_images.sql
```

To change products or photos, edit the table in `db/generate_dev_seed.py`; photo credits are in `db/PRODUCT_IMAGE_CREDITS.md`.

### 4. Run

```bash
cd backend/eCommerce && ./mvnw spring-boot:run      # http://localhost:8080  (Swagger: /swagger-ui.html)
cd frontend/eCommerce-client && npm start           # http://localhost:4200
```

### API overview

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/reactivate` | Public (needs email + password) |
| GET | `/oauth2/authorization/google` | Public (browser navigation, starts Google sign-in) |
| GET | `/api/v1/products`, `/api/v1/products/{id}`, `/api/v1/products/{id}/image` | Public |
| GET / PATCH / DELETE | `/api/v1/users/me` | Authenticated |
| POST | `/api/v1/users/me/password` | Authenticated |
| PATCH | `/api/v1/users/me/deactivate` | Authenticated |
| GET / DELETE | `/api/v1/cart` | Authenticated (always the caller's own cart) |
| POST | `/api/v1/cart/items` | Authenticated - `{ productId, quantity }`, adds to an existing line |
| PATCH / DELETE | `/api/v1/cart/items/{productId}` | Authenticated - set quantity (1-10) / remove |

Errors are RFC 9457 `ProblemDetail` JSON with a stable `code` field (e.g. `EMAIL_ALREADY_EXISTS`, `ACCOUNT_DEACTIVATED`).
