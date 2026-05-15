# ConnectHub

ConnectHub is a Spring Boot microservices backend for a real-time chat application. It includes:

- `discovery-server` for Eureka service discovery
- `api-gateway` for a single entry point and WebSocket routing
- `auth-service` for JWT auth, user registration/login, and OAuth2 login support
- `room-service` for creating and joining rooms
- `message-service` for room message history and live WebSocket chat
- `notification-service` for unread message notifications
- `common-lib` for shared JWT utilities and DTOs

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Spring Cloud 2023.0.x
- Spring Security + JWT
- OAuth2 Login (Google/GitHub style providers via Spring Security)
- Spring WebSocket
- Spring Data JPA
- MySQL
- OpenFeign
- Eureka
- Spring Cloud Gateway

## Project Layout

```text
connecthub/
  common-lib/
  discovery-server/
  api-gateway/
  auth-service/
  room-service/
  message-service/
  docker-compose.yml
```

## How To Run

### 0. Create your `.env`

This repo uses environment variables for passwords/secrets. Copy the example file and edit it:

```powershell
Copy-Item .env.example .env
```

At minimum, set `MYSQL_ROOT_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` in `.env`.

### 1. Start MySQL

```powershell
docker compose up -d
```

This creates three schemas in one MySQL container:

- `auth_db`
- `room_db`
- `message_db`
- `notification_db`

### 2. Optional: configure OAuth

If you want Google OAuth login, set these values (either in `.env` or as environment variables) and enable the `oauth` profile:

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:SPRING_PROFILES_ACTIVE="oauth"
```

If you skip these, the normal JWT register/login flow still works.

### 3. Start services

Start them in this order using the Maven wrapper:

```powershell
.\mvnw.cmd -pl discovery-server spring-boot:run
.\mvnw.cmd -pl auth-service spring-boot:run
.\mvnw.cmd -pl room-service spring-boot:run
.\mvnw.cmd -pl message-service spring-boot:run
.\mvnw.cmd -pl notification-service spring-boot:run
.\mvnw.cmd -pl api-gateway spring-boot:run
```

### 4. Main ports

- Eureka: `http://localhost:8761`
- Gateway: `http://localhost:8080`
- Auth service: `http://localhost:8081`
- Room service: `http://localhost:8082`
- Message service: `http://localhost:8083`
- Notification service: `http://localhost:8084`

## Swagger / OpenAPI

Once services are running, Swagger UI is available at:

- Gateway (aggregated): `http://localhost:8080/swagger-ui/index.html`
- Auth service: `http://localhost:8081/swagger-ui/index.html`
- Room service: `http://localhost:8082/swagger-ui/index.html`
- Message service: `http://localhost:8083/swagger-ui/index.html`
- Notification service: `http://localhost:8084/swagger-ui/index.html`

Raw OpenAPI JSON for each service:

- Auth: `http://localhost:8081/v3/api-docs` (or via gateway: `http://localhost:8080/v3/api-docs/auth`)
- Room: `http://localhost:8082/v3/api-docs` (or via gateway: `http://localhost:8080/v3/api-docs/room`)
- Message: `http://localhost:8083/v3/api-docs` (or via gateway: `http://localhost:8080/v3/api-docs/message`)
- Notification: `http://localhost:8084/v3/api-docs` (or via gateway: `http://localhost:8080/v3/api-docs/notification`)

## API Overview

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /oauth2/authorization/google`

#### OAuth2 (Google) from a SPA (React/Vite)

If you start Google OAuth from the browser using `fetch`/`axios`, the request will follow the backend `302` redirect to `https://accounts.google.com/...` and then fail with a CORS error (Google does not allow cross-origin XHR/fetch for the OAuth authorize endpoint).

Use a top-level navigation instead (or an `<a>` link), so the browser can follow redirects normally:

```js
// Example (React click handler)
window.location.assign("http://localhost:8080/oauth2/authorization/google");
```

After a successful login, the `auth-service` redirects to `app.oauth2.redirect-url` (default: `http://localhost:5173/oauth-success`) with `token` and `email` query params.

### Rooms

- `POST /api/rooms`
- `GET /api/rooms`
- `GET /api/rooms/{roomCode}`
- `POST /api/rooms/{roomCode}/join`
- `GET /api/rooms/{roomCode}/members`

### Messages

- `GET /api/messages/rooms/{roomCode}`
- `POST /api/messages/rooms/{roomCode}`
- WebSocket: `ws://localhost:8080/ws/chat?roomCode={roomCode}&token={jwt}`

### Notifications

- `GET /api/notifications/unread`
- `GET /api/notifications/unread/count`
- `POST /api/notifications/rooms/{roomCode}/read`

## Example Flow

### Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "Password@123"
}
```

### Create room

```http
POST /api/rooms
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "name": "General Discussion",
  "description": "Open team chat"
}
```

### Join room

```http
POST /api/rooms/{roomCode}/join
Authorization: Bearer <jwt>
```

### Send message over WebSocket

```json
{
  "content": "Hello everyone!"
}
```

## Notes

- All microservices register with Eureka and discover each other by service ID.
- `message-service` verifies room existence through `room-service`.
- The JWT secret defaults to a local development value but should be overridden in production.
- OAuth login is wired for Spring Security providers and becomes active once provider credentials are configured.

### React DevTools

For React development, install React DevTools in your browser (Chrome/Edge extension or Firefox add-on). React’s official link: `https://react.dev/link/react-devtools`.
