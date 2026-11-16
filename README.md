# QR Dine – Multi-Restaurant QR Ordering Platform

A full-stack SaaS where each restaurant gets its own QR code. Guests scan, order from their phone and pay after the meal. Owners and kitchen staff manage everything live. Several restaurants share one system, and each sees only its own data.

**Live demo:** https://qrdine-vinod.vercel.app/?restaurantId=1
**Owner dashboard:** https://qrdine-vinod.vercel.app/owner (demo login below)

> The backend runs on a free tier and sleeps when idle, so the first load can take 1–3 minutes.

## Screenshots

| Guest menu | Owner dashboard |
|---|---|
| ![Guest menu](docs/guest-menu.png) | ![Owner dashboard](docs/owner-dashboard.png) |

## Features

**For guests (no app, no sign-up)**
- Scan one QR code, then choose **Dine-in** (pick a free table) or **Takeaway**
- Menu with photos, veg / non-veg markers and an AI assistant
- Live order tracking (Pending → Preparing → Served); takeaway guests get a pickup token
- **Table tab:** order as often as you like and pay once at the end, with an equal split-bill option
- Call-waiter button and "Request bill"

**For owners**
- Menu management, table QR codes (downloadable) and kitchen staff management
- Bill requests, waiter calls and settle-up with cash or UPI
- GST-inclusive printable bills (CGST + SGST)
- Sales analytics: today and weekly sales, top items, dine-in vs takeaway split

**For the kitchen**
- Live order board with sound alerts, PIN login and smart order assignment by chef capacity

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, Axios |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA (Hibernate), WebSocket (STOMP) |
| Database | PostgreSQL (Neon in the cloud, Docker locally) |
| Auth | JWT with restaurant ID in the token (multi-tenant isolation) |
| Hosting | Render (backend, Docker), Vercel (frontend) |

## Architecture highlights

- **Multi-tenancy:** every record belongs to a restaurant; the restaurant ID comes from the logged-in user's JWT, never from the request.
- **Secure guest links:** each guest session gets a random secret key, so guessing session or order numbers returns *403 Forbidden*.
- **Tables free themselves:** unconfirmed holds expire after 30 min and idle tables after 90 min, with no background scheduler.
- **Secrets stay out of Git:** configuration comes from environment variables (Render) or a git-ignored `local.properties`.

## Run it locally

Requirements: Java 21+, Node 18+, Docker.

```bash
# 1. Database
docker run --name restaurant-db -e POSTGRES_PASSWORD=<your-password> -e POSTGRES_DB=restaurant_db -p 5433:5432 -d postgres

# 2. Backend (create local.properties with your own DB password and JWT secret first)
./mvnw spring-boot:run

# 3. Frontend (separate repo: restaurant-frontend)
npm install
npm run dev
```

## Demo login (test data only)

Owner: `test@cloudcafe.com` / `test1234`

## Repositories

- Backend: https://github.com/Vinodkumar9841/restaurant-backend
- Frontend: https://github.com/Vinodkumar9841/restaurant-frontend

## Author

Vinod Kumar – Java Full Stack Developer (Spring Boot, REST APIs, PostgreSQL)
GitHub: https://github.com/Vinodkumar9841
