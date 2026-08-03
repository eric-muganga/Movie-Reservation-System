# Movie Reservation System

A Spring Boot application that allows users to browse movies and showtimes, reserve seats, and manage reservations and payments. It includes both a public API and an admin dashboard for managing showtimes, reservations, and revenue.

## Table of Contents

- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running locally with Maven](#running-locally-with-maven)
  - [Running with Docker Compose](#running-with-docker-compose)
- [Database & Migrations](#database--migrations)
- [Reservation & Seat Locking Flow](#reservation--seat-locking-flow)
- [REST API](#rest-api)
  - [Reservation endpoints](#reservation-endpoints)
- [Admin Dashboard](#admin-dashboard)
- [Tests](#tests)
- [Continuous Integration](#continuous-integration)
- [Future Work](#future-work)
- [License](#license)

---

## Architecture

The application follows a layered architecture:

- **Web layer** – Controllers and Thymeleaf views (`/admin/**`, `/api/**`).
- **Service layer** – Business logic for reservations, seat locks, reporting, and movie browsing.
- **Persistence layer** – Spring Data JPA repositories backed by PostgreSQL.
- **Infrastructure** – Flyway migrations, Testcontainers-based integration tests, and Docker Compose for local environments.

## Features

- Browse movies, showtimes, and auditorium seating.
- Reserve seats for a given showtime.
- Seat locking to prevent double booking and handle concurrent users.
- Reservation lifecycle:
  - `PENDING` → `CONFIRMED` → `CANCELLED`
- Payment lifecycle:
  - `PENDING` → `PAID` → `FAILED`
- Admin dashboard:
  - View reservations and details (including seat labels).
  - Cancel reservations from allowed statuses.
  - Daily showtime performance and revenue reporting.

## Tech Stack

- **Language:** Java (Spring Boot)
- **Build:** Maven
- **Database:** PostgreSQL
- **ORM & migrations:** Spring Data JPA, Flyway
- **Frontend:** Thymeleaf + HTML/CSS for admin views
- **Testing:** JUnit, Mockito, Spring Boot Test, Testcontainers
- **CI:** GitHub Actions (Java + Maven workflow)

## Getting Started

### Prerequisites

- Java 17 or 21 (match your `pom.xml` / `setup-java` version).
- Maven 3.8+ (or use the included `mvnw` wrapper).
- Docker & Docker Compose (for running Postgres and the app together).

### Running locally with Maven

1. Clone the repository:

   ```bash
   git clone https://github.com/eric-muganga/movie-reservation-system.git
   cd movie-reservation-system
   ```

2. Start a local Postgres instance (via Docker Compose or your own setup) and ensure the application’s `application.yml` points to it.

3. Run the application:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Access:

   - Public/API: `http://localhost:8080/api/...`
   - Admin dashboard: `http://localhost:8080/admin/...`

### Running with Docker Compose

The project includes a `docker-compose.yaml` for local development.

1. Build and start services:

   ```bash
   docker compose up --build
   ```

2. This will start:

   - `cinema-app`: the Spring Boot application.
   - `cinema-db`: PostgreSQL with the Flyway migrations applied on startup.

3. Access the app at `http://localhost:8080`.

To stop:

```bash
docker compose down
```

## Database & Migrations

The schema is managed by **Flyway** migrations under `src/main/resources/db/migration`.

Key tables:

- `users`, `roles`, `user_roles`
- `movies`, `genres`, `movie_genres`
- `auditoriums`, `seats`
- `showtimes`
- `reservations`, `reservation_seats`
- `seat_locks`

Recent migrations add:

- Payment-related columns to `reservations` (e.g. `payment_status`, `payment_reference`, `paid_at`).
- Indexes on timestamps and foreign keys to support reporting and admin queries.

On startup, Flyway automatically brings the database up to the latest version.

## Reservation & Seat Locking Flow

The core reservation flow is implemented in `ReservationServiceImpl` and related services.

High-level steps for `/api/reservations` (start checkout):

1. Validate the user (`auth0Sub`) and showtime.
2. Ensure the showtime is in the future and all requested seats belong to the showtime’s auditorium.
3. Expire any stale `seat_locks`.
4. Check for:
   - Seats already reserved (`reservation_seats`) → return `409 Conflict` (`"already reserved"`).
   - Seats locked by other users (`seat_locks`) → return `409 Conflict` (`"locked"`).
5. Create `SeatLock` entries for the current user and requested seats.
6. Create a `Reservation` with:
   - `status = CONFIRMED`
   - `paymentStatus = PAID` (or `PENDING` if you adopt a multi-step payment flow).
7. Create corresponding `ReservationSeat` entries for each seat.
8. Mark the locks as `CONSUMED`.

Cancellation and payment updates:

- `DELETE /api/reservations/{reservationId}` cancels a reservation for the current user.
- `POST /api/reservations/{reservationId}/confirm-payment` marks the reservation as paid.
- `POST /api/reservations/{reservationId}/fail-payment` marks payment as failed and cancels the reservation.

## REST API

### Reservation endpoints

All reservation endpoints are exposed via `ReservationController` under `/api/reservations`.

#### Start checkout / reserve seats

```http
POST /api/reservations
Headers:
  X-Debug-User: <auth0Sub for development>

Body:
{
  "showtimeId": 3,
  "seatIds":[1][2][3]
}
```

Response (success `200 OK`):

```json
{
  "reservationId": 123,
  "status": "CONFIRMED",
  "showtimeId": 3,
  "createdAt": "2026-07-30T15:00:00Z",
  "totalAmount": 37.50,
  "seats": [
    { "seatId": 1, "rowLabel": "A", "seatNumber": 1, "price": 12.50 },
    { "seatId": 2, "rowLabel": "A", "seatNumber": 2, "price": 12.50 },
    { "seatId": 3, "rowLabel": "A", "seatNumber": 3, "price": 12.50 }
  ]
}
```

Example conflict response (`409 Conflict`):

```json
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Some seats are already reserved (BOOKED) for this showtime: ",[2][1]
  "path": "/api/reservations"
}
```

#### Cancel reservation (user-facing)

```http
DELETE /api/reservations/{reservationId}
Headers:
  X-Debug-User: <auth0Sub>

Response: 204 No Content
```

#### Confirm payment

```http
POST /api/reservations/{reservationId}/confirm-payment
Body:
{
  "paymentReference": "PAYMENT-12345"
}
```

#### Fail payment

```http
POST /api/reservations/{reservationId}/fail-payment
Body (optional):
{
  "reason": "PAYMENT_FAILED"
}
```

## Admin Dashboard

Admin functionality lives under `/admin/**` and uses Thymeleaf templates:

- `/admin/reservations` – Paginated reservation list with status filter.
- `/admin/reservations/{reservationId}` – Reservation detail view:
  - Customer email
  - Movie and showtime info
  - Seats (e.g. `A1`, `A2`, `A3`)
  - Created/updated timestamps
  - Cancel action where allowed.
- `/admin/showtimes/daily` – Daily showtime performance reports.
- `/admin/revenue` – Revenue and reserved seat counts per showtime for a given date.
- `/admin/movies` – Movies with showtimes for the selected business date.

These views are backed by services such as `AdminReservationQueryServiceImpl` and `AdminReservationCommandServiceImpl`.

## Tests

The project includes both unit and integration tests.

### Unit tests

- `ReservationServiceImplTest`:
  - Happy-path reservation creation.
  - Already-reserved seat conflicts.
  - Seats locked by other users.
  - Missing user handling.

### Integration tests

Using Spring Boot Test + Testcontainers:

- `ReservationFlowIntegrationTest`:
  - `POST /api/reservations` happy path and conflicts.
  - Cancelling a reservation and verifying seating.
  - Handling locks and expiry.

- `AdminReservationFlowIntegrationTest`:
  - Admin reservation list and detail pages.
  - Cancel reservation from the admin UI.

To run tests locally:

```bash
./mvnw test
```

Testcontainers will start an ephemeral PostgreSQL instance for integration tests.

## Continuous Integration

The repository uses GitHub Actions to run `mvn test` on each push and pull request. A typical workflow (stored in `.github/workflows/ci-maven-test.yml`) looks like:

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main", "develop" ]
  pull_request:
    branches: [ "main", "develop" ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Maven tests
        run: mvn -B test --file pom.xml
```

This ensures all unit and integration tests run automatically on every change.

## Future Work

Some ideas for future enhancements:

- Authentication and authorisation integration (e.g. with Auth0).
- Customer-facing UI for browsing movies and managing reservations.
- More advanced pricing (e.g. seat categories, dynamic pricing).
- Exportable reports (CSV/PDF) from the admin dashboard.
- API documentation (OpenAPI/Swagger).
