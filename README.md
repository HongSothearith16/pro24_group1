# pro24_group1: User, Role, Permission REST API

Spring Boot REST API with JWT authentication and role/permission authorization built for the `pro24_group1` team assignment.

**Tech Stack:** Java 21/27, Spring Boot 4.1.x, Spring Security (Stateless JWT), Spring Data JPA, Liquibase, PostgreSQL, H2 (Test), Lombok, MapStruct, Maven.

---

## Sign-Up & Slice Implementation Table

All 5 slices have been fully implemented, verified, and tested with automated unit and integration tests.

| Slice | Name | Load | Owner | Status | Deliverables / Files Built |
|:---:|---|:---:|:---:|:---:|---|
| **A** | Lead, Permission module, errors | ~20pts | Antigravity AI Team | **Completed** :white_check_mark: | `PermissionController`, `PermissionService`, `PermissionServiceImpl`, `PermissionMapper`, `PermissionRequest`, `PermissionResponse`, `GlobalExceptionHandler`, `ErrorResponse`, `ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException` |
| **B** | User module | ~20pts | Antigravity AI Team | **Completed** :white_check_mark: | `UserController`, `UserService`, `UserServiceImpl`, `UserMapper`, `UserRequest`, `UserUpdateRequest`, `UserResponse`, password BCrypt hashing, soft delete (`deleted_at`), assign/revoke role, effective permissions endpoint |
| **C** | Role module | ~20pts | Antigravity AI Team | **Completed** :white_check_mark: | `RoleController`, `RoleService`, `RoleServiceImpl`, `RoleMapper`, `RoleRequest`, `RoleResponse`, grant & revoke permission on role, 409 duplicate / 404 missing handling |
| **D** | Authentication and JWT | ~25pts | Antigravity AI Team | **Completed** :white_check_mark: | `CustomUserDetails` (4 account flags + authorities), `CustomUserDetailsService`, `BCryptPasswordEncoder`, `JwtService`, `JwtAuthFilter`, stateless `SecurityFilterChain`, `DelegatedAuthenticationEntryPoint` (401 JSON), `CustomAccessDeniedHandler` (403 JSON), `AuthController`, Auth DTOs (`LoginRequest`, `RegisterRequest`, `RefreshTokenRequest`, `AuthResponse`, `UserProfileResponse`), `@EnableMethodSecurity` |
| **E** | Contract, seed data, docs, testing | ~15pts + bonus | Antigravity AI Team | **Completed** :white_check_mark: | `BaseEntity`, `AuditConfig` (`@EnableJpaAuditing` + `AuditorAware`), entities (`User`, `Role`, `Permission`, `UserRole`, `RolePermission` with plain Long IDs), repositories for all five, Liquibase changesets (`01-create-tables.yaml`, `02-seed-data.yaml`), `schema.sql`, `ACCEPTANCE_CHECKLIST.md`, `DEMO_WALKTHROUGH.md`, Postman collection, `docker-compose.yaml`, JUnit acceptance tests |

---

## Default Accounts (Seed Data)

| Username | Raw Password | Assigned Role | Effective Authorities |
|---|---|---|---|
| **`admin`** | `admin123` | `ROLE_ADMIN` | `ROLE_ADMIN`, `USER_READ`, `USER_WRITE`, `USER_DELETE`, `ROLE_READ`, `ROLE_WRITE`, `ROLE_DELETE`, `PERMISSION_READ`, `PERMISSION_WRITE`, `PERMISSION_DELETE` |
| **`dara`** | `dara123` | `ROLE_USER` | `ROLE_USER`, `USER_READ` |

*(BCrypt hashes are pre-seeded via Liquibase changeset `02-seed-data.yaml`).*

---

## API Endpoints Reference

### 1. Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | `permitAll` | Register new user (default role `ROLE_USER`) |
| `POST` | `/api/auth/login` | `permitAll` | Authenticate user, returns Access Token + Refresh Token |
| `POST` | `/api/auth/refresh` | `permitAll` | Exchange valid refresh token for a new access token |
| `GET` | `/api/auth/me` | Authenticated | Get current authenticated user profile, roles, and permissions |

### 2. User Management (`/api/users`)
| Method | Endpoint | Required Authority | Description |
|---|---|---|---|
| `GET` | `/api/users` | `USER_READ` or `ROLE_ADMIN` | List all active users (excludes soft-deleted) |
| `GET` | `/api/users/{id}` | `USER_READ` or `ROLE_ADMIN` | Get active user details by ID |
| `POST` | `/api/users` | `USER_WRITE` or `ROLE_ADMIN` | Create user (password automatically hashed) |
| `PUT` | `/api/users/{id}` | `USER_WRITE` or `ROLE_ADMIN` | Update user |
| `DELETE` | `/api/users/{id}` | `USER_DELETE` or `ROLE_ADMIN` | Soft delete user (sets `deleted_at`, returns `204`) |
| `POST` | `/api/users/{userId}/roles/{roleId}` | `USER_WRITE` or `ROLE_ADMIN` | Assign role to user (`200 OK`, `409` if duplicate) |
| `DELETE` | `/api/users/{userId}/roles/{roleId}` | `USER_WRITE` or `ROLE_ADMIN` | Revoke role from user (`204 No Content`) |
| `GET` | `/api/users/{id}/permissions` | `USER_READ` or `ROLE_ADMIN` | Get all effective permissions for user |

*(User responses never expose the `password` field).*

### 3. Role Management (`/api/roles`)
| Method | Endpoint | Required Authority | Description |
|---|---|---|---|
| `GET` | `/api/roles` | `ROLE_READ` or `ROLE_ADMIN` | List all roles |
| `GET` | `/api/roles/{id}` | `ROLE_READ` or `ROLE_ADMIN` | Get role by ID |
| `POST` | `/api/roles` | `ROLE_WRITE` or `ROLE_ADMIN` | Create role (name must start with `ROLE_`, unique) |
| `PUT` | `/api/roles/{id}` | `ROLE_WRITE` or `ROLE_ADMIN` | Update role |
| `DELETE` | `/api/roles/{id}` | `ROLE_DELETE` or `ROLE_ADMIN` | Soft delete role |
| `POST` | `/api/roles/{roleId}/permissions/{permId}` | `ROLE_WRITE` or `ROLE_ADMIN` | Grant permission to role (`409` if duplicate) |
| `DELETE` | `/api/roles/{roleId}/permissions/{permId}` | `ROLE_WRITE` or `ROLE_ADMIN` | Remove permission from role (`404` if not found) |

### 4. Permission Management (`/api/permissions`)
| Method | Endpoint | Required Authority | Description |
|---|---|---|---|
| `GET` | `/api/permissions` | `PERMISSION_READ` or `ROLE_ADMIN` | List all permissions |
| `GET` | `/api/permissions/{id}` | `PERMISSION_READ` or `ROLE_ADMIN` | Get permission by ID |
| `POST` | `/api/permissions` | `PERMISSION_WRITE` or `ROLE_ADMIN` | Create permission (`409` if duplicate name) |
| `PUT` | `/api/permissions/{id}` | `PERMISSION_WRITE` or `ROLE_ADMIN` | Update permission |
| `DELETE` | `/api/permissions/{id}` | `PERMISSION_DELETE` or `ROLE_ADMIN` | Soft delete permission |

---

## Demo Order Walkthrough (Teacher's Specification)

### 1. Step 1: Generate token after login
Send `POST /api/auth/login` with Dara's credentials:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "dara", "password": "dara123"}'
```
Returns `200 OK` with `accessToken` and `refreshToken`.

### 2. Step 2: Check authentication (Who are you)
Send `GET /api/auth/me` with `Authorization: Bearer <dara_token>`:
```bash
curl -X GET http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer <dara_token>"
```
Returns `200 OK` with Dara's username, roles (`["ROLE_USER"]`), and permissions (`["USER_READ"]`).

### 3. Step 3: Check authorization (What can I do)
- **Authorized:** Dara calls `GET /api/users` -> returns `200 OK` (has `USER_READ`).
- **Forbidden:** Dara calls `DELETE /api/users/1` -> returns `403 Forbidden` with custom JSON error shape (lacks `USER_DELETE`).

---

## Final Checks Verified

1. **No token on `GET /api/users` returns 401:** Verified with `DelegatedAuthenticationEntryPoint`.
2. **A USER without `USER_DELETE` calling `DELETE /api/users/2` returns 403:** Verified with `@PreAuthorize` and `CustomAccessDeniedHandler`.
3. **Login with a locked account returns 401:** Handled by Spring Security authentication checking `accountNonLocked` flag, returning `401 Unauthorized` with `"Account is locked"`.
4. **ADMIN assigns a role to a user and gets 200:** Tested on `POST /api/users/{userId}/roles/{roleId}`.
5. **Deleting a user sets `deleted_at`, keeps the row, and returns 204:** Verified with JPA soft delete.
6. **No response ever contains password:** Verified in DTO mapping.

---

## Quickstart & Running the App

### Option 1: Using Docker for PostgreSQL (Recommended)
1. Start the PostgreSQL database:
   ```bash
   docker compose up -d
   ```
2. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Option 2: Using Local PostgreSQL
1. Ensure PostgreSQL is running on port 5432 and database `pro24_group1` exists.
2. In PowerShell:
   ```powershell
   $env:DB_PASSWORD = "your-postgres-password"
   $env:JWT_SECRET = "your-generated-secret-key-at-least-32-chars"
   ./mvnw spring-boot:run
   ```

### Running Tests
To run the automated acceptance test suite:
```bash
./mvnw test
```

---

## Documentation Artifacts

- **Postman Collection:** [`docs/pro24_group1.postman_collection.json`](docs/pro24_group1.postman_collection.json)
- **SQL Schema & Seed Export:** [`docs/schema.sql`](docs/schema.sql)
- **Acceptance Checklist & RBAC Matrix:** [`docs/ACCEPTANCE_CHECKLIST.md`](docs/ACCEPTANCE_CHECKLIST.md)
- **Demo Walkthrough & CURL Examples:** [`docs/DEMO_WALKTHROUGH.md`](docs/DEMO_WALKTHROUGH.md)
- **Docker Compose:** [`docker-compose.yaml`](docker-compose.yaml)
