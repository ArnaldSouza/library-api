# Library API

A REST API for managing a library's book inventory, with JWT authentication and role-based access control.

## Problem

Libraries and small collections need a controlled way to track their inventory: who can browse the catalog, who can modify it, and how to find a specific title among thousands. This API separates those concerns — any authenticated user can search and browse the collection with pagination and filters, while only administrators can add, update, or remove books.

## Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4.1 (Spring Web MVC, Spring Data JPA, Spring Security 7)
- **Database:** PostgreSQL 17
- **Key libraries:** Hibernate 7, HikariCP, JJWT 0.12, Jakarta Validation
- **Testing:** JUnit 5, Mockito, AssertJ, MockMvc
- **Build:** Maven
- **Containerization:** Docker / Docker Compose

## What This Demonstrates

- **Stateless JWT authentication** with a custom security filter, BCrypt password hashing, and no server-side sessions
- **Role-based authorization** enforced at the HTTP method level — `USER` reads, `ADMIN` writes
- **Layered architecture** with strict separation between controller, service, and repository, and DTOs isolating the API contract from the persistence model
- **Dynamic filtering** using JPA Specifications, so optional query parameters combine without a combinatorial explosion of repository methods
- **Centralized exception handling** returning semantically correct status codes (404, 400 with per-field errors, 409, 401) instead of leaking stack traces
- **Security-conscious defaults**: registration cannot self-assign `ADMIN`, login failures return a generic message to prevent user enumeration, credentials live in environment variables, and the container runs as a non-root user
- **29 automated tests** covering business logic in isolation (Mockito) and the full HTTP chain including authentication (MockMvc)

## Architecture

```
                    ┌──────────────────────────┐
   HTTP Request ───►│  JwtAuthenticationFilter │  validates token,
                    │                          │  populates SecurityContext
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │  Authorization Rules     │  ROLE_USER / ROLE_ADMIN
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │  Controller              │  HTTP mapping, @Valid,
                    │  (BookController)        │  status codes
                    └────────────┬─────────────┘
                                 │  DTO
                    ┌────────────▼─────────────┐
                    │  Service                 │  business rules,
                    │  (BookService)           │  DTO ↔ entity mapping
                    └────────────┬─────────────┘
                                 │  Entity
                    ┌────────────▼─────────────┐
                    │  Repository              │  Spring Data JPA
                    │  (BookRepository)        │  + Specifications
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │  PostgreSQL              │
                    └──────────────────────────┘

   Exceptions from any layer are caught by GlobalExceptionHandler
   (@RestControllerAdvice) and converted into structured JSON errors.
```

## How to Run

```bash
git clone https://github.com/ArnaldSouza/library-api.git
cd library-api
cp .env.example .env
docker compose up --build
```

The API is available at `http://localhost:8080`.

Edit `.env` before starting if you want different credentials. The file is git-ignored; `.env.example` is the template.

### Running locally without Docker

Requires JDK 25 and a PostgreSQL instance on `localhost:5432` with a `library` database.

```bash
./mvnw spring-boot:run
```

## Authentication

All endpoints except `/api/v1/auth/**` and `/api/v1/health` require a bearer token.

```bash
# Register (always creates a USER)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"a-strong-password"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"a-strong-password"}'

# Use the returned token
curl http://localhost:8080/api/v1/books \
  -H "Authorization: Bearer <token>"
```

An `ADMIN` account is created on first startup using `ADMIN_USERNAME` and `ADMIN_PASSWORD` from `.env`.

## Roles & Permissions

| Action | Endpoint | USER | ADMIN |
|--------|----------|------|-------|
| Browse and filter books | `GET /api/v1/books` | ✅ | ✅ |
| Get a single book | `GET /api/v1/books/{id}` | ✅ | ✅ |
| Create a book | `POST /api/v1/books` | ❌ | ✅ |
| Update a book | `PUT /api/v1/books/{id}` | ❌ | ✅ |
| Delete a book | `DELETE /api/v1/books/{id}` | ❌ | ✅ |

## API Endpoints

| Method | Route | Description |
|--------|-------|-------------|
| GET | `/api/v1/health` | Service health check (public) |
| POST | `/api/v1/auth/register` | Create a `USER` account, returns a JWT |
| POST | `/api/v1/auth/login` | Authenticate, returns a JWT |
| GET | `/api/v1/books` | Paginated book list with optional filters |
| GET | `/api/v1/books/{id}` | Fetch a single book |
| POST | `/api/v1/books` | Create a book |
| PUT | `/api/v1/books/{id}` | Replace a book |
| DELETE | `/api/v1/books/{id}` | Remove a book |

### Query parameters on `GET /api/v1/books`

| Parameter | Description | Example |
|-----------|-------------|---------|
| `author` | Partial, case-insensitive match | `?author=martin` |
| `genre` | Exact, case-insensitive match | `?genre=software` |
| `page` | Zero-based page index (default `0`) | `?page=2` |
| `size` | Items per page (default `10`) | `?size=25` |
| `sort` | Sort field and direction (default `id`) | `?sort=title,asc` |

Filters combine: `?author=martin&genre=software&page=0&size=5`.

### Error responses

Errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-09-30T18:42:11.204Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fields": {
    "title": "Title is required",
    "availableCopies": "Available copies cannot be negative"
  }
}
```

| Status | When |
|--------|------|
| 400 | Request body fails validation (includes a `fields` map) |
| 401 | Invalid login credentials |
| 403 | Missing token, or role lacks permission for the operation |
| 404 | Book not found |
| 409 | Username already taken |

## Testing

```bash
./mvnw test
```

Integration tests require the PostgreSQL container to be running and a `library_test` database:

```bash
docker compose up -d db
docker exec library-db psql -U library_user -d library -c "CREATE DATABASE library_test;"
```

**Unit tests (20)** run in isolation with Mockito, no database required:

- `BookServiceTest` — CRUD logic, not-found paths, and verification that the service does not touch the repository when a book is missing
- `AuthServiceTest` — passwords are always hashed before persistence, registration always assigns `USER`, duplicate usernames are rejected, and failed authentication never issues a token
- `JwtServiceTest` — claim extraction, plus rejection of tampered, expired, and foreign-signed tokens

**Integration tests (9)** exercise the full HTTP chain against a real PostgreSQL instance:

- `BookControllerIT` — authentication via real JWTs, role enforcement on every verb, pagination, filtering, validation errors, and 404 handling

## Known Limitations

This is a portfolio project. The following are deliberate trade-offs, documented rather than hidden:

- **Schema management uses `ddl-auto: update`.** Hibernate generates the schema from the entities, which is convenient for development but unsafe for production. A real deployment would use versioned migrations (Flyway or Liquibase).
- **The default admin account is seeded at startup.** Credentials come from environment variables rather than being hardcoded, but any known default account is a risk. In production, the first administrator would be provisioned out-of-band.
- **The JWT secret is a static value.** There is no key rotation, and no refresh-token flow — tokens simply expire after one hour.
- **Integration tests depend on a running database.** They target the Compose container rather than spinning up a disposable one, so `docker compose up -d db` is a prerequisite.
- **`Page` is serialized directly.** Spring Data warns that `PageImpl`'s JSON structure is not guaranteed to be stable across versions; `PagedModel` would be the safer contract.
- **DTO ↔ entity mapping is manual.** Explicit and dependency-free, but repetitive as the model grows. MapStruct would generate it.
- **One role per user.** A single enum field covers the two-role requirement; a many-to-many relationship would be needed for users holding multiple roles.

## License

MIT