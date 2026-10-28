# Demo Walkthrough & Response Showcase

This document provides exact `curl` commands and expected JSON responses for each step of the assignment demo and final checks.

---

## 1. Demo Order (Teacher's Specification)

### Step 1: Generate token after login
**Request:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "dara",
    "password": "dara123"
  }'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 2,
    "username": "dara",
    "email": "dara@pro24group1.com",
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "enabled": true,
    "roles": [
      "ROLE_USER"
    ],
    "createdAt": "2026-10-28T14:30:00",
    "updatedAt": null
  }
}
```

---

### Step 2: Check authentication (Who are you)
**Request:**
```bash
curl -X GET http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer <dara_access_token>"
```

**Response (200 OK):**
```json
{
  "id": 2,
  "username": "dara",
  "email": "dara@pro24group1.com",
  "accountNonExpired": true,
  "accountNonLocked": true,
  "credentialsNonExpired": true,
  "enabled": true,
  "roles": [
    "ROLE_USER"
  ],
  "permissions": [
    "USER_READ"
  ],
  "createdAt": "2026-10-28T14:30:00",
  "updatedAt": null
}
```

---

### Step 3: Check authorization (What can I do)

#### A. Authorized action: GET /api/users
**Request:**
```bash
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer <dara_access_token>"
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@pro24group1.com",
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "enabled": true,
    "roles": [
      "ROLE_ADMIN"
    ],
    "createdAt": "2026-10-28T14:30:00",
    "updatedAt": null
  },
  {
    "id": 2,
    "username": "dara",
    "email": "dara@pro24group1.com",
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "enabled": true,
    "roles": [
      "ROLE_USER"
    ],
    "createdAt": "2026-10-28T14:30:00",
    "updatedAt": null
  }
]
```

#### B. Unauthorized action: DELETE /api/users/1
**Request:**
```bash
curl -X DELETE http://localhost:8081/api/users/1 \
  -H "Authorization: Bearer <dara_access_token>"
```

**Response (403 Forbidden):**
```json
{
  "timestamp": "2026-10-28T14:30:02",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: Access Denied",
  "path": "/api/users/1"
}
```

---

## 2. Final Checks Showcase

### Check 1: No token on GET /api/users returns 401
**Request:**
```bash
curl -X GET http://localhost:8081/api/users
```

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2026-10-28T14:30:03",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized: Full authentication is required to access this resource",
  "path": "/api/users"
}
```

---

### Check 2: A USER without USER_DELETE calling DELETE /api/users/2 returns 403
**Request:**
```bash
curl -X DELETE http://localhost:8081/api/users/2 \
  -H "Authorization: Bearer <dara_access_token>"
```

**Response (403 Forbidden):**
```json
{
  "timestamp": "2026-10-28T14:30:04",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: Access Denied",
  "path": "/api/users/2"
}
```

---

### Check 3: Login with a locked account returns 401
**Request:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "locked_user",
    "password": "password123"
  }'
```

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2026-10-28T14:30:05",
  "status": 401,
  "error": "Unauthorized",
  "message": "Account is locked",
  "path": "/api/auth/login"
}
```

---

### Check 4: ADMIN assigns a role to a user and gets 200
**Request:**
```bash
curl -X POST http://localhost:8081/api/users/2/roles/1 \
  -H "Authorization: Bearer <admin_access_token>"
```

**Response (200 OK):**
`Status: 200 OK` (Empty body)

---

### Check 5: Deleting a user sets deleted_at, keeps the row, and returns 204
**Request:**
```bash
curl -X DELETE http://localhost:8081/api/users/3 \
  -H "Authorization: Bearer <admin_access_token>"
```

**Response (204 No Content):**
`Status: 204 No Content`

**Subsequent GET Request:**
```bash
curl -X GET http://localhost:8081/api/users/3 \
  -H "Authorization: Bearer <admin_access_token>"
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-10-28T14:30:06",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 3",
  "path": "/api/users/3"
}
```
*(Row remains in PostgreSQL table `users` with `deleted_at` timestamp populated).*

---

### Check 6: No response ever contains password
Inspecting any user response (GET all, GET by ID, Auth response, Me profile) confirms:
- Field `password` is never serialized in any response JSON.
- Handled securely with clean DTO mapping.
