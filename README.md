# pro24_group1: User, Role, Permission REST API

Spring Boot REST API with JWT authentication and role/permission authorization.

**Tech:** Java 21, Spring Boot 4.1.x, Spring Security (JWT), Spring Data JPA, Liquibase, PostgreSQL, Lombok, MapStruct, Maven.

## Requirements

- JDK 21 (`java -version`)
- PostgreSQL (with pgAdmin)
- Git
- VS Code with the Extension Pack for Java (or IntelliJ)
- Postman

## Setup

### 1. Clone the project

```
git clone https://github.com/HongSothearith16/pro24_group1.git
cd pro24_group1
```

### 2. Create the database

In pgAdmin, create an empty database named `pro24_group1`.
Do not create any tables. Liquibase creates them when the app starts.

### 3. Set the environment variables

The app needs two variables. Never commit their real values.

| Variable | What it is |
|---|---|
| `DB_PASSWORD` | Your PostgreSQL `postgres` user password |
| `JWT_SECRET` | A random string of at least 32 characters |

Generate a secret in PowerShell:

```
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

**Option A: VS Code.** Create `.vscode/launch.json` (this folder is git-ignored):

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Pro24Group1Application",
      "request": "launch",
      "mainClass": "com.pro24_group1.Pro24Group1Application",
      "env": {
        "JWT_SECRET": "your-generated-secret",
        "DB_PASSWORD": "your-postgres-password"
      }
    }
  ]
}
```

Select **Pro24Group1Application** in Run and Debug, then press `F5`.

**Option B: Terminal (PowerShell).**

```
$env:JWT_SECRET = "your-generated-secret"
$env:DB_PASSWORD = "your-postgres-password"
mvn spring-boot:run
```

### 4. Check that it works

The log should show `Started Pro24Group1Application`. The API runs at:

```
http://localhost:8081
```

## Default accounts (seed data)

TODO: to be filled in when the seed changeset is merged.

| Username | Password | Role |
|---|---|---|
| admin | TODO | ROLE_ADMIN |
| dara | TODO | ROLE_USER |

## API

TODO: list the endpoints, or link the Postman collection in `/docs`.

## Team workflow

- Never push directly to `main`.
- Create a branch per task: `feature/p4-user-crud`.
- Open a pull request and have a teammate review it.
- Do not edit a Liquibase changeset after it has been merged. Add a new one.
- Never commit passwords, the JWT secret, `.vscode/`, or `target/`.

## Troubleshooting

| Error | Fix |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | The variable isn't set in the terminal or launch config you used |
| `password authentication failed` | `DB_PASSWORD` doesn't match your PostgreSQL password |
| `database "pro24_group1" does not exist` | Create the database in pgAdmin |
| `Port 8081 was already in use` | Stop the old run, or change `server.port` |