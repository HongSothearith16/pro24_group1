# Acceptance Checklist & RBAC Matrix

## 1. Table of Who Can Do What (RBAC Matrix)

| Endpoint | Method | Required Authority | `ROLE_ADMIN` | `ROLE_USER` (e.g. Dara) | Unauthenticated |
|---|---|---|:---:|:---:|:---:|
| `/api/auth/register` | POST | None (permitAll) | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| `/api/auth/login` | POST | None (permitAll) | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| `/api/auth/refresh` | POST | None (permitAll) | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| `/api/auth/me` | GET | Authenticated | :white_check_mark: | :white_check_mark: | :x: (401) |
| `/api/users` | GET | `USER_READ` or `ROLE_ADMIN` | :white_check_mark: | :white_check_mark: | :x: (401) |
| `/api/users/{id}` | GET | `USER_READ` or `ROLE_ADMIN` | :white_check_mark: | :white_check_mark: | :x: (401) |
| `/api/users` | POST | `USER_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/users/{id}` | PUT | `USER_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/users/{id}` | DELETE | `USER_DELETE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/users/{userId}/roles/{roleId}` | POST | `USER_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/users/{userId}/roles/{roleId}` | DELETE | `USER_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/users/{id}/permissions` | GET | `USER_READ` or `ROLE_ADMIN` | :white_check_mark: | :white_check_mark: | :x: (401) |
| `/api/roles` | GET | `ROLE_READ` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles/{id}` | GET | `ROLE_READ` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles` | POST | `ROLE_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles/{id}` | PUT | `ROLE_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles/{id}` | DELETE | `ROLE_DELETE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles/{roleId}/permissions/{permId}` | POST | `ROLE_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/roles/{roleId}/permissions/{permId}` | DELETE | `ROLE_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/permissions` | GET | `PERMISSION_READ` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/permissions/{id}` | GET | `PERMISSION_READ` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/permissions` | POST | `PERMISSION_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/permissions/{id}` | PUT | `PERMISSION_WRITE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |
| `/api/permissions/{id}` | DELETE | `PERMISSION_DELETE` or `ROLE_ADMIN` | :white_check_mark: | :x: (403) | :x: (401) |

---

## 2. Final Checks Acceptance List

- [x] **Check 1:** No token on `GET /api/users` returns **401 Unauthorized**.
- [x] **Check 2:** A `ROLE_USER` (e.g. Dara with `USER_READ`) without `USER_DELETE` calling `DELETE /api/users/2` returns **403 Forbidden**.
- [x] **Check 3:** Login with a locked account (`account_non_locked = false`) returns **401 Unauthorized** with message `"Account is locked"`.
- [x] **Check 4:** ADMIN assigns a role to a user (`POST /api/users/{userId}/roles/{roleId}`) and gets **200 OK**.
- [x] **Check 5:** Deleting a user (`DELETE /api/users/{id}`) sets `deleted_at`, keeps the row in DB, returns **204 No Content**, and skips deleted users in subsequent queries (returns 404).
- [x] **Check 6:** No response ever contains `password`. Password hashes are safely omitted from all DTOs and API responses.

---

## 3. Demo Order (Teacher's Specification)

1. **Step 1: Generate token after login**
   - Call `POST /api/auth/login` with `{ "username": "dara", "password": "dara123" }`
   - Returns 200 OK with `accessToken`, `refreshToken`, `tokenType: "Bearer"`.
2. **Step 2: Check authentication (who are you)**
   - Call `GET /api/auth/me` with `Authorization: Bearer <dara_token>`
   - Returns 200 OK confirming username `dara`, role `ROLE_USER`, and effective permissions `["USER_READ"]`.
3. **Step 3: Check authorization (what can I do)**
   - Call `GET /api/users` with `Authorization: Bearer <dara_token>` -> Returns 200 OK because Dara has `USER_READ`.
   - Call `DELETE /api/users/1` with `Authorization: Bearer <dara_token>` -> Returns 403 Forbidden because Dara lacks `USER_DELETE`.
