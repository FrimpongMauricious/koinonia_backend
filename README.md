# Koinonia Backend

Koinonia is a Twitter-style social platform focused on Christian knowledge-sharing. This repository contains the REST API backend that powers the React Native mobile client. Users can post content, like, comment, repost, favorite, and follow one another. The backend is stateless, JWT-authenticated, and designed to deploy on Render with a Neon PostgreSQL database.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security 6 + JWT (jjwt 0.12.6, HS256) |
| Database | PostgreSQL |
| Migrations | Flyway (Hibernate validates only — never modifies schema) |
| Build | Maven (Maven Wrapper included) |
| Rate limiting | Bucket4j (in-memory token bucket) |

---

## Local Setup

### Prerequisites
- Java 21
- PostgreSQL running on port 5433 (or update `application.properties`)

### Steps

```bash
# 1. Clone
git clone https://github.com/FrimpongMauricious/koinonia_backend.git
cd koinonia_backend

# 2. Create the database
psql -U postgres -c "CREATE DATABASE koinonia_db;"

# 3. Copy the example config
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit the file and fill in your values

# 4. Set the DB password environment variable
export DB_PASSWORD=your_postgres_password       # macOS / Linux
$env:DB_PASSWORD = "your_postgres_password"     # Windows PowerShell

# 5. Run
./mvnw spring-boot:run          # macOS / Linux
# Windows — use the cached Maven wrapper:
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```

The server starts at `http://localhost:8080`. Flyway applies all migrations automatically on startup.

---

## API Overview

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | — | Register a new account |
| POST | `/api/v1/auth/login` | — | Login, receive JWT |

> Auth endpoints are rate-limited: login 5 req/min per IP, register 3 req/min per IP.

### Users
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/users/me` | Required | Get own profile |
| PUT | `/api/v1/users/me` | Required | Update profile (displayName, bio, profilePictureUrl) |
| DELETE | `/api/v1/users/me` | Required | Delete own account (send `{"password":"..."}` to confirm) |

### Posts
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/posts` | Optional | Paginated feed |
| POST | `/api/v1/posts` | Required | Create a post |
| GET | `/api/v1/posts/{id}` | Optional | Get single post |
| PUT | `/api/v1/posts/{id}` | Required | Update own post |
| DELETE | `/api/v1/posts/{id}` | Required | Delete own post |
| GET | `/api/v1/users/{userId}/posts` | Optional | Posts by a user |

### Likes
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/posts/{id}/like` | Required | Like a post (idempotent) |
| DELETE | `/api/v1/posts/{id}/like` | Required | Unlike a post (idempotent) |

### Comments
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/posts/{id}/comments` | Optional | Paginated comments |
| POST | `/api/v1/posts/{id}/comments` | Required | Add a comment |
| PUT | `/api/v1/posts/{postId}/comments/{commentId}` | Required | Edit own comment |
| DELETE | `/api/v1/posts/{postId}/comments/{commentId}` | Required | Delete own comment |

### Follow
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/users/{id}/follow` | Required | Follow a user (idempotent) |
| DELETE | `/api/v1/users/{id}/follow` | Required | Unfollow (idempotent) |
| GET | `/api/v1/users/{id}/followers` | Optional | Paginated followers (no email exposed) |
| GET | `/api/v1/users/{id}/following` | Optional | Paginated following (no email exposed) |

### Reposts
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/posts/{id}/repost` | Required | Repost (idempotent, cannot repost own post) |
| DELETE | `/api/v1/posts/{id}/repost` | Required | Un-repost (idempotent) |

### Favorites
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/posts/{id}/favorite` | Required | Favorite a post (idempotent) |
| DELETE | `/api/v1/posts/{id}/favorite` | Required | Unfavorite (idempotent) |
| GET | `/api/v1/users/me/favorites` | Required | Own favorites list (paginated) |

### Health
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | — | Health check (used by Render) |

---

## Architecture Notes

The codebase is organised by feature slice — `auth`, `post`, `like`, `comment`, `follow`, `repost`, `favorite`, `user`, `config`, `exception`. Each slice owns its entity, repository, service, and controller. Flyway is the single source of truth for the database schema across 7 migrations (V1–V7); Hibernate is set to `validate` and never modifies the schema directly.

Authentication is stateless JWT. Every request to a secured endpoint passes through `JwtAuthenticationFilter`, which validates the token and loads the `User` principal into the Spring Security context. No sessions are created or stored. The feed and post-detail endpoints batch all social counts (likes, comments, reposts) in three GROUP BY queries per page load and resolve the current user's interaction state in three IN-clause queries — eliminating N+1 queries regardless of page size.

---

## Running Tests

Tests require a live PostgreSQL database. Each test class is `@Transactional` and rolls back automatically, so they leave no data behind.

```powershell
# Windows — set password first, then run
$env:DB_PASSWORD = "your_password"
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd" test

# Run a single test class
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=RegressionSmokeTest
```

---

## Deploying to Render

> Fill in after first successful deploy.

- [ ] Create a Neon PostgreSQL database; copy the JDBC connection URL
- [ ] Create a Render Web Service connected to this repo
- [ ] Set build command: `./mvnw package -DskipTests`
- [ ] Set start command: `java -Dspring.profiles.active=prod -jar target/koinonia-backend-0.0.1-SNAPSHOT.jar`
- [ ] Add environment variables on Render:
  - `DATABASE_URL` — JDBC URL from Neon: `jdbc:postgresql://host/db?sslmode=require`
  - `JWT_SECRET` — fresh Base64-encoded 32-byte random value
  - `APP_CORS_ALLOWED_ORIGINS` — your Expo/React Native origins, comma-separated
  - `SPRING_PROFILES_ACTIVE=prod`
- [ ] Set Render health check path to `/actuator/health`
- [ ] Deploy and verify logs show "Started KoinoniaBackendApplication"
