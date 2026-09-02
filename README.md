# PhotoVault Backend

PhotoVault is a REST API for a personal photo library. It provides account management, email verification and password recovery, JWT authentication with refresh-token cookies, photo uploads backed by ImageKit, albums, library management, and ImageKit-powered AI image transformations.

## Technology

- Java 21 and Spring Boot 4.1
- PostgreSQL with Spring Data JPA
- Redis for refresh-token storage
- Spring Security, JWT, and BCrypt password hashing
- ImageKit for image storage and transformations
- SMTP and Thymeleaf email templates
- Maven Wrapper (Maven 3.9.16)

## Prerequisites

- JDK 21
- PostgreSQL 14+ running locally or accessible over the network
- Redis 6+ running locally or accessible over the network
- An ImageKit account and keys
- SMTP credentials for verification and password-reset emails

## Configure the application

The application reads its configuration from `src/main/resources/application.yml`. Set environment variables before starting it; the values below override the development defaults in that file.

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_USERNAME` | PostgreSQL username | `photovault_user` |
| `DB_PASSWORD` | PostgreSQL password | `photovault_pass` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password, if enabled | empty |
| `SMTP_HOST` / `SMTP_PORT` | SMTP server address and port | Gmail / `587` |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | SMTP account credentials | configured file defaults |
| `JWT_SECRET` | Base64-encoded JWT signing secret | configured file default |
| `FRONTEND_URL` | Frontend URL used in email links | `http://localhost:5173` |
| `COOKIE_SECURE` | Send refresh cookie over HTTPS only | `false` |
| `IMAGEKIT_PUBLIC_KEY` | ImageKit public key | configured file default |
| `IMAGEKIT_PRIVATE_KEY` | ImageKit private key | configured file default |
| `IMAGEKIT_URL_ENDPOINT` | ImageKit delivery URL endpoint | configured file default |

Create the database and a user before the first run:

```sql
CREATE USER photovault_user WITH PASSWORD 'photovault_pass';
CREATE DATABASE photovault OWNER photovault_user;
```

For local development, export your secrets in the shell rather than committing them:

```bash
export DB_USERNAME=photovault_user
export DB_PASSWORD=replace-me
export JWT_SECRET="$(openssl rand -base64 64)"
export SMTP_USERNAME=you@example.com
export SMTP_PASSWORD=your-smtp-app-password
export IMAGEKIT_PUBLIC_KEY=public_...
export IMAGEKIT_PRIVATE_KEY=private_...
export IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your_endpoint
```

## Run locally

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`.

To build and run tests:S

```bash
./mvnw clean verify
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

## Authentication

1. Register with `POST /api/auth/register`.
2. Verify the email using the token sent by email.
3. Log in with `POST /api/auth/login`. The response contains an access token and the server sets an HTTP-only refresh-token cookie.
4. Send the access token on protected endpoints:

```http
Authorization: Bearer <access-token>
```

5. Renew an expired access token with `POST /api/auth/refresh`; the refresh-token cookie is rotated automatically.

The configured CORS origin is currently `http://localhost:5173`. Update `SecurityConfig` when deploying a frontend at another origin.

## API overview

All routes except `/api/auth/**` require a bearer access token.

| Area | Endpoints |
| --- | --- |
| Authentication | `POST /api/auth/register`, `/login`, `/refresh`, `/forgot-password`, `/reset-password`, `/verify-email`, `/resend-verification` |
| User sessions | `PATCH /api/users/me/change-password`; `POST /api/users/me/logout`, `/me/logout-all` |
| Photos | `GET /api/photos`, `GET /api/photos/{id}`, `POST /api/photos/upload`, `POST /api/photos`; `POST /api/photos/archive`, `/trash`, `/restore`, `/delete-permanent`; `DELETE /api/photos/{id}` |
| Albums | `GET`, `POST /api/albums`; `GET`, `PATCH`, `DELETE /api/albums/{id}`; `GET`, `POST /api/albums/{id}/photos`; `DELETE /api/albums/{id}/photos/{photoId}` |
| Library | `GET /api/library/storage`, `/imagekit-assets`; `POST /api/library/import` |
| AI transforms | `POST /api/photos/{photoId}/ai/preview`, `/apply` |

Photo listing accepts `status` (`ACTIVE`, `ARCHIVE`, or `TRASH`), `page`, and `size`. Album photo listing accepts `page` and `size`; both default to page `0` with 24 items.

### Example: register, log in, and upload

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"strong-password","displayName":"Your Name"}'

curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"strong-password"}'

curl -X POST http://localhost:8080/api/photos/upload \
  -H 'Authorization: Bearer <access-token>' \
  -F 'file=@/absolute/path/to/photo.jpg'
```

Uploads accept multipart form data under the `file` field. The maximum file size is 25 MB and the maximum request size is 100 MB.

### Example: AI transformation

```bash
curl -X POST http://localhost:8080/api/photos/<photo-id>/ai/preview \
  -H 'Authorization: Bearer <access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"type":"REMOVE_BACKGROUND"}'
```

Available transform types are `REMOVE_BACKGROUND`, `BACKGROUND_AND_SHADOW`, `CHANGE_BACKGROUND`, `GENERATIVE_FILL`, `SMART_CROP`, `OBJECT_CROP`, `RETOUCH`, `UPSCALE`, and `AI_EDIT`.

## Storage and AI limits

By default, PhotoVault limits each user to 300 MB of active-photo storage and 15 applied AI transformations per calendar month. Adjust these application settings if your ImageKit plan and expected usage allow it:

```yaml
app:
  storage:
    quota-bytes-per-user: 314572800
  ai:
    max-transforms-per-user-per-month: 15
```

## Project layout

```text
src/main/java/com/saumik/photovault/
├── config/        # security, Redis, ImageKit, and async configuration
├── controller/    # REST endpoints
├── dto/           # request and response records
├── entity/        # JPA entities
├── event/         # domain events
├── exception/     # API exception handling
├── listener/      # email-related event listeners
├── mail/          # SMTP delivery and templates
├── repository/    # persistence layer
├── security/      # JWT, cookies, and user details
└── service/       # application business logic
```

