# Koinonia Backend — Slice 1: Auth Foundation

Spring Boot 3.3 · Java 21 · PostgreSQL · JWT

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| PostgreSQL | any recent (port 5433) |
| Maven | not required — use `mvnw` |

Create the database once:

```sql
CREATE DATABASE koinonia_db;
```

---

## Setup

`application.properties` is **not committed** to the repo — copy the example template and fill in your own values:

```powershell
# PowerShell
Copy-Item src/main/resources/application.properties.example src/main/resources/application.properties
```

```bash
# macOS / Linux
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Then edit `src/main/resources/application.properties` and replace:

- `YOUR_DB_PASSWORD_HERE` → your PostgreSQL password
- `YOUR_JWT_SECRET_HERE` → a secure Base64-encoded secret (see below)

### Generating a JWT secret

```bash
# macOS / Linux
openssl rand -base64 32
```

```powershell
# PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

### Setting DB_PASSWORD via environment variable (alternative)

Instead of hardcoding the password in `application.properties`, you can use `${DB_PASSWORD}` in the file and set the variable at runtime:

```powershell
$env:DB_PASSWORD = "your_password_here"
.\mvnw.cmd spring-boot:run
```

---

## Running

```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

Flyway runs `V1__create_users_table.sql` automatically on startup.
Server starts on <http://localhost:8080>.

---

## Testing the API

### 1. Register

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "paul",
    "email": "paul@koinonia.dev",
    "password": "Grace123",
    "displayName": "Paul"
  }' | jq .
```

### 2. Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "paul@koinonia.dev",
    "password": "Grace123"
  }' | jq .
```

Copy the `token` value from the response.

### 3. Get my profile (protected)

```bash
TOKEN="<paste token here>"

curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Running tests

```bash
./mvnw test
```

> Tests require PostgreSQL on localhost:5433. Each test is `@Transactional` and rolls back automatically.

---

## What's next — Slice 2: Posts CRUD

- `posts` table (Flyway migration `V2`)
- `Post` entity with `author_id` FK → `users.id`
- `PostsController`: create, read (paginated feed), update, delete
- Ownership check: only the post author can edit/delete
- `UserResponse` enriched with post count
